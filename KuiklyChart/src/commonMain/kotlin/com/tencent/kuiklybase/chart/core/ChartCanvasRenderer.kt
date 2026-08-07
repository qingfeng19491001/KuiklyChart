package com.tencent.kuiklybase.chart.core

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.views.ContextApi
import com.tencent.kuikly.core.views.TextAlign
import com.tencent.kuiklybase.chart.config.ChartAnnotationConfig
import com.tencent.kuiklybase.chart.config.ChartTheme
import com.tencent.kuiklybase.chart.config.StockAverageLine
import com.tencent.kuiklybase.chart.config.ChartThresholdConfig
import com.tencent.kuiklybase.chart.config.AreaMode
import com.tencent.kuiklybase.chart.core.cartesian.CartesianLayout
import com.tencent.kuiklybase.chart.core.cartesian.CartesianScale
import com.tencent.kuiklybase.chart.core.cartesian.PlotRect
import com.tencent.kuiklybase.chart.core.polar.PolarScale
import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.model.ChartSelection
import com.tencent.kuiklybase.chart.model.ChartSeries
import com.tencent.kuiklybase.chart.model.ChartSlice
import com.tencent.kuiklybase.chart.model.ChartViewport
import com.tencent.kuiklybase.chart.model.OhlcPoint
import com.tencent.kuiklybase.chart.model.RadarDimension
import com.tencent.kuiklybase.chart.model.RadarSeries
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** 轴刻度：数据坐标 + 展示文案（类别轴优先用 [ChartDataPoint.label]）。 */
internal data class AxisTick(val value: Float, val text: String)

internal inline fun ContextApi.withPlotClip(plot: PlotRect, draw: () -> Unit) {
    save()
    beginPath()
    moveTo(plot.left, plot.top)
    lineTo(plot.right, plot.top)
    lineTo(plot.right, plot.bottom)
    lineTo(plot.left, plot.bottom)
    closePath()
    clipPathIntersect()
    try {
        draw()
    } finally {
        restore()
    }
}

internal fun resolveAnnotationTextPosition(
    desiredX: Float,
    desiredBaselineY: Float,
    textWidth: Float,
    textAscent: Float,
    textDescent: Float,
    plot: PlotRect,
    padding: Float = 2f,
): Pair<Float, Float> {
    val minX = plot.left + padding
    val maxX = maxOf(minX, plot.right - textWidth.coerceAtLeast(0f) - padding)
    val minBaselineY = plot.top + textAscent.coerceAtLeast(0f) + padding
    val maxBaselineY = maxOf(
        minBaselineY,
        plot.bottom - textDescent.coerceAtLeast(0f) - padding,
    )
    return desiredX.coerceIn(minX, maxX) to
        desiredBaselineY.coerceIn(minBaselineY, maxBaselineY)
}

internal object ChartCanvasRenderer {
    fun axisTicksFromSeries(series: List<ChartSeries>): List<AxisTick> {
        val points = series.firstOrNull()?.points.orEmpty()
        if (points.isEmpty()) return emptyList()
        return points
            .asSequence()
            .filter { it.x.isFinite() }
            .distinctBy { it.x }
            .map { AxisTick(it.x, it.label.ifBlank { formatValue(it.x) }) }
            .toList()
    }

    fun axisTicksFromOhlc(points: List<OhlcPoint>): List<AxisTick> {
        if (points.isEmpty()) return emptyList()
        return points
            .asSequence()
            .filter { it.x.isFinite() }
            .map { AxisTick(it.x, it.label.ifBlank { formatValue(it.x) }) }
            .toList()
    }

    fun drawGrid(
        ctx: ContextApi,
        layout: CartesianLayout,
        viewport: ChartViewport,
        theme: ChartTheme,
        show: Boolean,
    ) {
        if (!show) return
        val plot = layout.plot
        val scale = CartesianScale(plot, viewport)
        ctx.strokeStyle(theme.gridColor.toChartColor())
        ctx.lineWidth(1f)
        val yTicks = 5
        for (i in 0..yTicks) {
            val y = plot.top + plot.height * i / yTicks
            ctx.beginPath()
            ctx.moveTo(plot.left, y)
            ctx.lineTo(plot.right, y)
            ctx.stroke()
        }
        val xTicks = 5
        for (i in 0..xTicks) {
            val dataX = viewport.xMin + (viewport.xMax - viewport.xMin) * i / xTicks
            val x = scale.toPixelX(dataX)
            ctx.beginPath()
            ctx.moveTo(x, plot.top)
            ctx.lineTo(x, plot.bottom)
            ctx.stroke()
        }
    }

    fun drawAxes(
        ctx: ContextApi,
        layout: CartesianLayout,
        viewport: ChartViewport,
        theme: ChartTheme,
        showX: Boolean,
        showY: Boolean,
        xTicks: List<AxisTick>? = null,
        yTicks: List<AxisTick>? = null,
    ) {
        val plot = layout.plot
        val scale = CartesianScale(plot, viewport)
        ctx.font(theme.fontSize)
        ctx.fillStyle(theme.axisColor.toChartColor())
        ctx.textAlign(TextAlign.CENTER)
        if (showX) {
            ctx.beginPath()
            ctx.strokeStyle(theme.axisColor.toChartColor())
            ctx.lineWidth(1f)
            ctx.moveTo(plot.left, plot.bottom)
            ctx.lineTo(plot.right, plot.bottom)
            ctx.stroke()
        }
        if (showY) {
            ctx.beginPath()
            ctx.strokeStyle(theme.axisColor.toChartColor())
            ctx.moveTo(plot.left, plot.top)
            ctx.lineTo(plot.left, plot.bottom)
            ctx.stroke()
            ctx.textAlign(TextAlign.RIGHT)
            val ticks = resolveVisibleTicks(yTicks, viewport.yMin, viewport.yMax)
            if (ticks != null) {
                ticks.forEach { tick ->
                    val y = scale.toPixelY(tick.value)
                    if (y in plot.top..plot.bottom) {
                        ctx.fillText(tick.text, plot.left - 6f, y + theme.fontSize * 0.35f)
                    }
                }
            } else {
                val count = 5
                for (i in 0..count) {
                    val ratio = i.toFloat() / count
                    val value = viewport.yMin + (viewport.yMax - viewport.yMin) * (1f - ratio)
                    val y = plot.top + plot.height * ratio
                    ctx.fillText(formatValue(value), plot.left - 6f, y + theme.fontSize * 0.35f)
                }
            }
        }
        if (showX) {
            ctx.textAlign(TextAlign.CENTER)
            val maxTickCount = resolveDynamicXAxisTickCount(
                availableWidth = plot.width,
                labels = xTicks.orEmpty()
                    .filter { it.value in minOf(viewport.xMin, viewport.xMax)..maxOf(viewport.xMin, viewport.xMax) }
                    .map { it.text },
                fontSize = theme.fontSize,
            )
            val ticks = resolveVisibleTicks(
                xTicks,
                viewport.xMin,
                viewport.xMax,
                maxCount = maxTickCount,
            )
            if (ticks != null) {
                ticks.forEach { tick ->
                    val x = scale.toPixelX(tick.value)
                    if (x in plot.left..plot.right) {
                        ctx.textAlign(resolveXAxisTickTextAlign(x, plot))
                        ctx.fillText(tick.text, x, plot.bottom + theme.fontSize + 4f)
                    }
                }
            } else {
                val count = min(6, 8)
                for (i in 0 until count) {
                    val ratio = i.toFloat() / (count - 1).coerceAtLeast(1)
                    val value = viewport.xMin + (viewport.xMax - viewport.xMin) * ratio
                    val x = scale.toPixelX(value)
                    ctx.textAlign(resolveXAxisTickTextAlign(x, plot))
                    ctx.fillText(formatValue(value), x, plot.bottom + theme.fontSize + 4f)
                }
            }
        }
    }

    /** 视口内可见刻度；过多时均匀抽样。null 表示走数值轴。 */
    private fun resolveVisibleTicks(
        ticks: List<AxisTick>?,
        minValue: Float,
        maxValue: Float,
        maxCount: Int = 6,
    ): List<AxisTick>? {
        if (ticks.isNullOrEmpty()) return null
        val lo = minOf(minValue, maxValue)
        val hi = maxOf(minValue, maxValue)
        val visible = ticks.filter { it.value in lo..hi }
        if (visible.isEmpty()) return emptyList()
        if (visible.size <= maxCount) return visible
        val step = (visible.size - 1).toFloat() / (maxCount - 1).coerceAtLeast(1)
        return (0 until maxCount).map { i -> visible[(i * step).toInt().coerceIn(0, visible.lastIndex)] }
            .distinctBy { it.value }
    }

    fun drawLineSeries(
        ctx: ContextApi,
        layout: CartesianLayout,
        viewport: ChartViewport,
        series: List<ChartSeries>,
        theme: ChartTheme,
        selection: ChartSelection?,
        showPoints: Boolean = true,
        pointRadius: Float = 4f,
        smooth: Boolean = false,
    ) {
        val scale = CartesianScale(layout.plot, viewport)
        series.forEachIndexed { sIdx, s ->
            if (s.points.isEmpty()) return@forEachIndexed
            val pixels = s.points.map { scale.toPixelX(it.x) to scale.toPixelY(it.y) }
            val color = s.color.toChartColor()
            ctx.beginPath()
            ctx.strokeStyle(color)
            ctx.lineWidth(theme.lineWidth)
            pathThroughPoints(ctx, pixels, smooth)
            ctx.stroke()
            if (showPoints) {
                s.points.forEachIndexed { pIdx, p ->
                    val (px, py) = pixels[pIdx]
                    val selected = selection is ChartSelection.Cartesian &&
                        selection.seriesIndex == sIdx && selection.itemIndex == pIdx
                    val fill = p.resolveColor(s.color)
                    drawMarker(ctx, px, py, fill, if (selected) pointRadius + 2f else pointRadius, selected)
                }
            }
        }
    }

    internal fun containsNonFinitePoint(series: List<ChartSeries>): Boolean =
        series.any { item -> item.points.any { point -> !point.x.isFinite() || !point.y.isFinite() } }

    internal fun lineSegments(
        points: List<Pair<Float, Float>?>,
        connectNulls: Boolean,
    ): List<List<Pair<Float, Float>>> {
        if (connectNulls) {
            return points.filterNotNull().takeIf { it.isNotEmpty() }?.let(::listOf) ?: emptyList()
        }
        val segments = mutableListOf<List<Pair<Float, Float>>>()
        var current = mutableListOf<Pair<Float, Float>>()
        points.forEach { point ->
            if (point == null) {
                if (current.isNotEmpty()) {
                    segments += current
                    current = mutableListOf()
                }
            } else {
                current += point
            }
        }
        if (current.isNotEmpty()) segments += current
        return segments
    }

    /**
     * 增强版折线绘制：支持阈值参考线、缺失值断点、折线下方填充、文本注释。
     * 渲染顺序：
     *   1) 阈值参考线
     *   2) 缺失值跳过
     *   3) 折线下方填充（可选）
     *   4) 折线主描边
     *   5) 数据点标记
     *   6) 文本注释（含可选连接线/锚点）
     */
    fun drawLineSeriesEnhanced(
        ctx: ContextApi,
        layout: CartesianLayout,
        viewport: ChartViewport,
        series: List<ChartSeries>,
        theme: ChartTheme,
        selection: ChartSelection?,
        smooth: Boolean,
        showPoints: Boolean,
        pointRadius: Float,
        connectNulls: Boolean,
        fillBelow: Boolean,
        thresholds: List<ChartThresholdConfig>,
        annotations: List<ChartAnnotationConfig>,
    ) {
        val scale = CartesianScale(layout.plot, viewport)
        val plot = layout.plot

        thresholds.forEach { t ->
            drawThreshold(ctx, scale, plot, theme, t)
        }

        series.forEachIndexed { sIdx, s ->
            if (s.points.isEmpty()) return@forEachIndexed
            val pixels = s.points.map { p ->
                val px = if (!p.x.isFinite() || !p.y.isFinite()) null
                else scale.toPixelX(p.x) to scale.toPixelY(p.y)
                p to px
            }
            val baseColor = s.color.toChartColor()

            if (fillBelow) {
                drawLineFillBelow(ctx, plot, pixels, s.color, connectNulls)
            }

            ctx.beginPath()
            ctx.strokeStyle(baseColor)
            ctx.lineWidth(theme.lineWidth)
            drawLinePath(ctx, pixels, smooth = smooth, connectNulls = connectNulls)
            ctx.stroke()

            if (showPoints) {
                pixels.forEachIndexed { pIdx, entry ->
                    val pos = entry.second ?: return@forEachIndexed
                    val p = entry.first
                    val selected = selection is ChartSelection.Cartesian &&
                        selection.seriesIndex == sIdx && selection.itemIndex == pIdx
                    val fill = p.resolveColor(s.color)
                    drawMarker(
                        ctx,
                        pos.first,
                        pos.second,
                        fill,
                        if (selected) pointRadius + 2f else pointRadius,
                        selected,
                    )
                }
            }
        }

        annotations.forEach { a ->
            drawAnnotation(ctx, scale, plot, a)
        }
    }

    /**
     * 描绘折线主路径：缺失值断点由 [connectNulls] 控制；[smooth] 切换到三次贝塞尔。
     */
    private fun drawLinePath(
        ctx: ContextApi,
        pixels: List<Pair<ChartDataPoint, Pair<Float, Float>?>>,
        smooth: Boolean,
        connectNulls: Boolean,
    ) {
        lineSegments(pixels.map { it.second }, connectNulls).forEach { segment ->
            if (smooth) {
                pathThroughPoints(ctx, segment, smooth = true)
            } else {
                ctx.moveTo(segment.first().first, segment.first().second)
                segment.drop(1).forEach { point -> ctx.lineTo(point.first, point.second) }
            }
        }
    }

    /**
     * 描绘折线下方区域填充：缺失段自动断开成多个填充区域。
     */
    private fun drawLineFillBelow(
        ctx: ContextApi,
        plot: PlotRect,
        pixels: List<Pair<ChartDataPoint, Pair<Float, Float>?>>,
        seriesColor: Long,
        connectNulls: Boolean,
    ) {
        lineSegments(pixels.map { it.second }, connectNulls).forEach { segment ->
            if (segment.isEmpty()) return@forEach
            ctx.beginPath()
            ctx.moveTo(segment.first().first, plot.bottom)
            segment.forEach { point -> ctx.lineTo(point.first, point.second) }
            ctx.lineTo(segment.last().first, plot.bottom)
            ctx.closePath()
            val gradient = ctx.createLinearGradient(0f, plot.top, 0f, plot.bottom)
            gradient.addColorStop(0f, Color(seriesColor.withAlpha(0xAA)))
            gradient.addColorStop(1f, Color(seriesColor.withAlpha(0x05)))
            ctx.fillStyle(gradient)
            ctx.fill()
        }
    }

    private fun drawThreshold(
        ctx: ContextApi,
        scale: CartesianScale,
        plot: PlotRect,
        theme: ChartTheme,
        cfg: ChartThresholdConfig,
    ) {
        val y = scale.toPixelY(cfg.value)
        if (y !in plot.top..plot.bottom) return
        ctx.beginPath()
        ctx.strokeStyle(cfg.color.toChartColor())
        ctx.lineWidth(1f)
        if (cfg.dashWidth > 0f) {
            var x = plot.left
            while (x < plot.right) {
                val end = minOf(x + cfg.dashWidth, plot.right)
                ctx.beginPath()
                ctx.moveTo(x, y)
                ctx.lineTo(end, y)
                ctx.stroke()
                x += cfg.dashWidth * 2f
            }
        } else {
            ctx.moveTo(plot.left, y)
            ctx.lineTo(plot.right, y)
            ctx.stroke()
        }
        if (cfg.showLabel && cfg.label.isNotEmpty()) {
            ctx.font(theme.fontSize)
            ctx.fillStyle(cfg.color.toChartColor())
            ctx.textAlign(TextAlign.LEFT)
            ctx.fillText(cfg.label, plot.left + 4f, y - 3f)
        }
    }

    private fun drawAnnotation(
        ctx: ContextApi,
        scale: CartesianScale,
        plot: PlotRect,
        cfg: ChartAnnotationConfig,
    ) {
        val ax = scale.toPixelX(cfg.dataX)
        val ay = scale.toPixelY(cfg.dataY)
        ctx.font(cfg.fontSize)
        ctx.textAlign(TextAlign.LEFT)
        val metrics = ctx.measureText(cfg.text)
        val (tx, ty) = resolveAnnotationTextPosition(
            desiredX = ax + cfg.dx,
            desiredBaselineY = ay + cfg.dy,
            textWidth = metrics.width,
            textAscent = metrics.actualBoundingBoxAscent,
            textDescent = metrics.actualBoundingBoxDescent,
            plot = plot,
        )

        if (cfg.connector) {
            ctx.beginPath()
            ctx.strokeStyle(cfg.connectorColor.toChartColor())
            ctx.lineWidth(1f)
            ctx.moveTo(ax, ay)
            ctx.lineTo(tx, ty)
            ctx.stroke()
        }
        if (cfg.anchorPoint) {
            ctx.beginPath()
            ctx.fillStyle(cfg.connectorColor.toChartColor())
            ctx.arc(ax, ay, 2.5f, 0f, (2 * PI).toFloat(), false)
            ctx.fill()
        }

        ctx.fillStyle(cfg.color.toChartColor())
        ctx.fillText(cfg.text, tx, ty)
    }

    fun drawAreaSeries(
        ctx: ContextApi,
        layout: CartesianLayout,
        viewport: ChartViewport,
        series: List<ChartSeries>,
        theme: ChartTheme,
        gradientFill: Boolean,
        selection: ChartSelection?,
        smooth: Boolean = false,
        showPoints: Boolean = false,
        pointRadius: Float = 4f,
        mode: AreaMode = AreaMode.BASIC,
    ) {
        if (mode == AreaMode.POLAR) {
            drawPolarArea(ctx, layout, viewport, series, theme, selection)
            return
        }
        if (mode == AreaMode.RANGE) {
            drawRangeArea(ctx, layout, viewport, series, theme)
            return
        }
        val plot = layout.plot
        val scale = CartesianScale(plot, viewport)
        val totals = if (mode == AreaMode.PERCENT_STACKED || mode == AreaMode.STREAM) {
            val n = series.maxOfOrNull { it.points.size } ?: 0
            (0 until n).map { i ->
                series.sumOf { it.points.getOrNull(i)?.y?.toDouble() ?: 0.0 }.toFloat()
            }
        } else emptyList()
        series.forEachIndexed { sIdx, s ->
            if (s.points.isEmpty()) return@forEachIndexed
            val stacked = mode == AreaMode.STACKED || mode == AreaMode.PERCENT_STACKED || mode == AreaMode.STREAM
            val values = s.points.mapIndexed { i, p -> when (mode) {
                    AreaMode.PERCENT_STACKED -> p.y / totals.getOrElse(i) { 1f } * 100f
                    else -> p.y
                }
            }
            val pixels = values.mapIndexed { i, value ->
                val lower = if (stacked) {
                    val prefix = series.take(sIdx)
                        .sumOf { it.points.getOrNull(i)?.y?.toDouble() ?: 0.0 }
                        .toFloat()
                    when (mode) {
                        AreaMode.PERCENT_STACKED -> prefix / totals.getOrElse(i) { 1f }.coerceAtLeast(1e-6f) * 100f
                        AreaMode.STREAM -> prefix - totals.getOrElse(i) { 0f } / 2f
                        else -> prefix
                    }
                } else 0f
                scale.toPixelX(s.points[i].x) to scale.toPixelY(value + lower)
            }
            val basePixels = values.mapIndexed { i, _ ->
                val lower = if (stacked) {
                    val prefix = series.take(sIdx)
                        .sumOf { it.points.getOrNull(i)?.y?.toDouble() ?: 0.0 }
                        .toFloat()
                    when (mode) {
                        AreaMode.PERCENT_STACKED -> prefix / totals.getOrElse(i) { 1f }.coerceAtLeast(1e-6f) * 100f
                        AreaMode.STREAM -> prefix - totals.getOrElse(i) { 0f } / 2f
                        else -> prefix
                    }
                } else 0f
                scale.toPixelX(s.points[i].x) to scale.toPixelY(lower)
            }
            val color = s.color.toChartColor()
            ctx.beginPath()
            val first = pixels.first()
            ctx.moveTo(first.first, if (stacked) basePixels.first().second else plot.bottom)
            if (mode == AreaMode.STEP) {
                ctx.lineTo(pixels.first().first, pixels.first().second)
                for (i in 1 until pixels.size) {
                    ctx.lineTo(pixels[i].first, pixels[i - 1].second)
                    ctx.lineTo(pixels[i].first, pixels[i].second)
                }
            } else if (smooth && pixels.size >= 2) {
                ctx.lineTo(first.first, first.second)
                pathThroughPoints(ctx, pixels, smooth = true, startFromIndex = 1)
            } else {
                pixels.forEach { (px, py) -> ctx.lineTo(px, py) }
            }
            val last = pixels.last()
            ctx.lineTo(last.first, if (stacked) basePixels.last().second else plot.bottom)
            if (stacked) {
                for (i in basePixels.lastIndex - 1 downTo 0) ctx.lineTo(basePixels[i].first, basePixels[i].second)
            }
            ctx.closePath()
            if (gradientFill) {
                val gradient = ctx.createLinearGradient(0f, plot.top, 0f, plot.bottom)
                gradient.addColorStop(0f, Color(s.color.withAlpha(0xAA)))
                gradient.addColorStop(1f, Color(s.color.withAlpha(0x11)))
                ctx.fillStyle(gradient)
            } else {
                ctx.fillStyle(Color(s.color.withAlpha(0x55)))
            }
            ctx.fill()
            ctx.beginPath()
            ctx.strokeStyle(color)
            ctx.lineWidth(theme.lineWidth)
            if (mode == AreaMode.STEP) {
                ctx.moveTo(pixels.first().first, pixels.first().second)
                for (i in 1 until pixels.size) {
                    ctx.lineTo(pixels[i].first, pixels[i - 1].second)
                    ctx.lineTo(pixels[i].first, pixels[i].second)
                }
            } else pathThroughPoints(ctx, pixels, smooth || mode == AreaMode.SPLINE)
            ctx.stroke()
            s.points.forEachIndexed { pIdx, p ->
                val selected = selection is ChartSelection.Cartesian &&
                    selection.seriesIndex == sIdx && selection.itemIndex == pIdx
                if (showPoints || selected) {
                    val (px, py) = pixels[pIdx]
                    val fill = p.resolveColor(s.color)
                    drawMarker(ctx, px, py, fill, if (selected) pointRadius + 2f else pointRadius, selected)
                }
            }
        }
    }

    private fun drawPolarArea(
        ctx: ContextApi, layout: CartesianLayout, viewport: ChartViewport,
        series: List<ChartSeries>, theme: ChartTheme, selection: ChartSelection?,
    ) {
        val plot = layout.plot
        val cx = (plot.left + plot.right) / 2f
        val cy = (plot.top + plot.bottom) / 2f
        val radius = min(plot.width, plot.height) * 0.38f
        val values = series.firstOrNull()?.points.orEmpty()
        val max = values.maxOfOrNull { it.y }?.coerceAtLeast(1f) ?: return
        val step = (2 * PI).toFloat() / values.size.coerceAtLeast(1)
        values.forEachIndexed { i, p ->
            val a0 = -PI.toFloat() / 2f + i * step + 0.03f
            val a1 = a0 + step - 0.06f
            val r = radius * (p.y / max).coerceIn(0f, 1f)
            ctx.beginPath(); ctx.moveTo(cx, cy)
            ctx.lineTo(cx + r * cos(a0), cy + r * sin(a0))
            ctx.arc(cx, cy, r, a0, a1, false); ctx.closePath()
            ctx.fillStyle(Color(p.resolveColor(series.first().color).withAlpha(0xAA))); ctx.fill()
            ctx.strokeStyle(theme.backgroundColor.toChartColor()); ctx.lineWidth(2f); ctx.stroke()
        }
    }

    private fun drawRangeArea(
        ctx: ContextApi, layout: CartesianLayout, viewport: ChartViewport,
        series: List<ChartSeries>, theme: ChartTheme,
    ) {
        if (series.size < 2) return
        val scale = CartesianScale(layout.plot, viewport)
        val lower = series[0].points.map { scale.toPixelX(it.x) to scale.toPixelY(it.y) }
        val upper = series[1].points.map { scale.toPixelX(it.x) to scale.toPixelY(it.y) }
        if (lower.isEmpty() || upper.isEmpty()) return
        ctx.beginPath(); ctx.moveTo(lower.first().first, lower.first().second)
        lower.drop(1).forEach { ctx.lineTo(it.first, it.second) }
        upper.asReversed().forEach { ctx.lineTo(it.first, it.second) }
        ctx.closePath(); ctx.fillStyle(Color(series[0].color.withAlpha(0x44))); ctx.fill()
        ctx.beginPath(); lower.forEachIndexed { i, p -> if (i == 0) ctx.moveTo(p.first, p.second) else ctx.lineTo(p.first, p.second) }
        ctx.strokeStyle(series[0].color.toChartColor()); ctx.lineWidth(theme.lineWidth); ctx.stroke()
        ctx.beginPath(); upper.forEachIndexed { i, p -> if (i == 0) ctx.moveTo(p.first, p.second) else ctx.lineTo(p.first, p.second) }
        ctx.strokeStyle(series[1].color.toChartColor()); ctx.lineWidth(theme.lineWidth); ctx.stroke()
    }

    fun drawBarSeries(
        ctx: ContextApi,
        layout: CartesianLayout,
        viewport: ChartViewport,
        series: List<ChartSeries>,
        theme: ChartTheme,
        selection: ChartSelection?,
        showLabel: Boolean,
        grouped: Boolean = true,
    ) {
        val plot = layout.plot
        val scale = CartesianScale(plot, viewport)
        if (series.isEmpty()) return
        val categories = series.first().points.size
        if (categories == 0) return
        val groupWidth = plot.width / categories
        val barCount = if (grouped) series.size else 1
        val barWidth = groupWidth * 0.7f / barCount
        series.forEachIndexed { sIdx, s ->
            s.points.forEachIndexed { pIdx, p ->
                val cx = scale.toPixelX(p.x)
                val groupLeft = cx - groupWidth * 0.35f
                val barLeft = if (grouped) {
                    groupLeft + barWidth * sIdx + groupWidth * 0.15f / barCount
                } else {
                    cx - barWidth / 2f
                }
                val barTop = scale.toPixelY(p.y)
                val barBottom = scale.toPixelY(0f.coerceAtLeast(viewport.yMin))
                val selected = selection is ChartSelection.Cartesian &&
                    selection.seriesIndex == sIdx && selection.itemIndex == pIdx
                val fill = p.resolveColor(s.color)
                ctx.fillStyle(fill.toChartColor())
                ctx.fillRect(barLeft, barTop, barWidth, (barBottom - barTop).coerceAtLeast(1f))
                if (selected) {
                    strokeRect(ctx, barLeft, barTop, barWidth, (barBottom - barTop).coerceAtLeast(1f), fill)
                }
                if (showLabel) {
                    ctx.font(theme.fontSize - 1f)
                    ctx.fillStyle(theme.textColor.toChartColor())
                    ctx.textAlign(TextAlign.CENTER)
                    ctx.fillText(formatValue(p.y), barLeft + barWidth / 2f, barTop - 4f)
                }
            }
        }
    }

    fun drawStackedBarSeries(
        ctx: ContextApi,
        layout: CartesianLayout,
        viewport: ChartViewport,
        series: List<ChartSeries>,
        theme: ChartTheme,
        selection: ChartSelection?,
        showTotalLabel: Boolean,
    ) {
        val plot = layout.plot
        val scale = CartesianScale(plot, viewport)
        if (series.isEmpty()) return
        val categories = series.first().points.size
        val barWidth = plot.width / categories * 0.6f
        for (cIdx in 0 until categories) {
            var stackBase = 0f
            var total = 0f
            series.forEachIndexed { sIdx, s ->
                val p = s.points.getOrNull(cIdx) ?: return@forEachIndexed
                val cx = scale.toPixelX(p.x)
                val barLeft = cx - barWidth / 2f
                val barBottom = scale.toPixelY(stackBase)
                stackBase += p.y
                total += p.y
                val barTop = scale.toPixelY(stackBase)
                val selected = selection is ChartSelection.Cartesian &&
                    selection.seriesIndex == sIdx && selection.itemIndex == cIdx
                val fill = p.resolveColor(s.color)
                ctx.fillStyle(fill.toChartColor())
                ctx.fillRect(barLeft, barTop, barWidth, (barBottom - barTop).coerceAtLeast(1f))
                if (selected) {
                    strokeRect(ctx, barLeft, barTop, barWidth, (barBottom - barTop).coerceAtLeast(1f), fill)
                }
            }
            if (showTotalLabel && categories > 0) {
                val p = series.first().points[cIdx]
                val cx = scale.toPixelX(p.x)
                ctx.font(theme.fontSize - 1f)
                ctx.fillStyle(theme.textColor.toChartColor())
                ctx.textAlign(TextAlign.CENTER)
                ctx.fillText(formatValue(total), cx, scale.toPixelY(stackBase) - 4f)
            }
        }
    }

    fun drawHorizontalBarSeries(
        ctx: ContextApi,
        layout: CartesianLayout,
        viewport: ChartViewport,
        series: List<ChartSeries>,
        theme: ChartTheme,
        selection: ChartSelection?,
        showLabel: Boolean,
        stacked: Boolean = false,
        showTotalLabel: Boolean = true,
    ) {
        val plot = layout.plot
        val scale = CartesianScale(plot, viewport)
        if (series.isEmpty()) return
        val categories = series.first().points.size
        if (categories == 0) return
        val groupHeight = plot.height / categories
        if (stacked) {
            for (cIdx in 0 until categories) {
                var stackBase = 0f
                var total = 0f
                val category = series.first().points[cIdx].x
                val cy = scale.toPixelY(category)
                val barTop = cy - groupHeight * 0.3f
                val barHeight = groupHeight * 0.6f
                series.forEachIndexed { sIdx, s ->
                    val p = s.points.getOrNull(cIdx) ?: return@forEachIndexed
                    val barLeft = scale.toPixelX(stackBase)
                    stackBase += p.y
                    total += p.y
                    val barRight = scale.toPixelX(stackBase)
                    val selected = selection is ChartSelection.Cartesian &&
                        selection.seriesIndex == sIdx && selection.itemIndex == cIdx
                    val fill = p.resolveColor(s.color)
                    ctx.fillStyle(fill.toChartColor())
                    ctx.fillRect(barLeft, barTop, (barRight - barLeft).coerceAtLeast(1f), barHeight)
                    if (selected) {
                        strokeRect(ctx, barLeft, barTop, (barRight - barLeft).coerceAtLeast(1f), barHeight, fill)
                    }
                }
                if (showTotalLabel) {
                    ctx.font(theme.fontSize - 1f)
                    ctx.fillStyle(theme.textColor.toChartColor())
                    ctx.textAlign(TextAlign.LEFT)
                    ctx.fillText(
                        formatValue(total),
                        scale.toPixelX(stackBase) + 4f,
                        cy + theme.fontSize * 0.35f,
                    )
                }
            }
            return
        }
        val barCount = series.size
        val barHeight = groupHeight * 0.7f / barCount
        series.forEachIndexed { sIdx, s ->
            s.points.forEachIndexed { pIdx, p ->
                val cy = scale.toPixelY(p.x)
                val groupTop = cy - groupHeight * 0.35f
                val barTop = groupTop + barHeight * sIdx + groupHeight * 0.15f / barCount
                val barLeft = scale.toPixelX(0f.coerceAtLeast(viewport.xMin))
                val barRight = scale.toPixelX(p.y)
                val selected = selection is ChartSelection.Cartesian &&
                    selection.seriesIndex == sIdx && selection.itemIndex == pIdx
                val fill = p.resolveColor(s.color)
                ctx.fillStyle(fill.toChartColor())
                ctx.fillRect(barLeft, barTop, (barRight - barLeft).coerceAtLeast(1f), barHeight)
                if (selected) {
                    strokeRect(ctx, barLeft, barTop, (barRight - barLeft).coerceAtLeast(1f), barHeight, fill)
                }
                if (showLabel) {
                    ctx.font(theme.fontSize - 1f)
                    ctx.fillStyle(theme.textColor.toChartColor())
                    ctx.textAlign(TextAlign.LEFT)
                    ctx.fillText(formatValue(p.y), barRight + 4f, barTop + barHeight * 0.7f)
                }
            }
        }
    }

    fun drawScatterSeries(
        ctx: ContextApi,
        layout: CartesianLayout,
        viewport: ChartViewport,
        series: List<ChartSeries>,
        theme: ChartTheme,
        selection: ChartSelection?,
        pointRadius: Float,
    ) {
        val plot = layout.plot
        val scale = CartesianScale(plot, viewport)
        series.forEachIndexed { sIdx, s ->
            s.points.forEachIndexed { pIdx, p ->
                val px = scale.toPixelX(p.x)
                val py = scale.toPixelY(p.y)
                val selected = selection is ChartSelection.Cartesian &&
                    selection.seriesIndex == sIdx && selection.itemIndex == pIdx
                val radius = if (selected) pointRadius + 2f else pointRadius
                val fill = p.resolveColor(s.color)
                drawMarker(ctx, px, py, fill, radius, selected)
            }
        }
    }

    fun drawCandlesticks(
        ctx: ContextApi,
        layout: CartesianLayout,
        viewport: ChartViewport,
        points: List<OhlcPoint>,
        theme: ChartTheme,
        selection: ChartSelection?,
        candleWidthRatio: Float,
    ) {
        if (points.isEmpty()) return
        val plot = layout.plot
        val scale = CartesianScale(plot, viewport)
        val visibleCount = points.count { it.x in viewport.xMin..viewport.xMax }.coerceAtLeast(1)
        val slot = plot.width / visibleCount
        val bodyWidth = slot * candleWidthRatio.coerceIn(0.2f, 0.9f)
        points.forEachIndexed { idx, p ->
            val cx = scale.toPixelX(p.x)
            val highY = scale.toPixelY(p.high)
            val lowY = scale.toPixelY(p.low)
            val openY = scale.toPixelY(p.open)
            val closeY = scale.toPixelY(p.close)
            val up = p.close >= p.open
            val fill = if (up) theme.upColor else theme.downColor
            val selected = selection is ChartSelection.Cartesian &&
                selection.seriesIndex == 0 && selection.itemIndex == idx
            ctx.beginPath()
            ctx.strokeStyle(fill.toChartColor())
            ctx.lineWidth(1f)
            ctx.moveTo(cx, highY)
            ctx.lineTo(cx, lowY)
            ctx.stroke()
            val top = minOf(openY, closeY)
            val height = (kotlin.math.abs(closeY - openY)).coerceAtLeast(1f)
            ctx.fillStyle(fill.toChartColor())
            ctx.fillRect(cx - bodyWidth / 2f, top, bodyWidth, height)
            if (selected) {
                strokeRect(ctx, cx - bodyWidth / 2f, top, bodyWidth, height, fill)
            }
        }
    }

    fun drawStockAverageLine(
        ctx: ContextApi,
        layout: CartesianLayout,
        viewport: ChartViewport,
        points: List<Pair<Float, Float>>,
        color: Long,
        theme: ChartTheme,
    ) {
        if (points.isEmpty()) return
        drawLineSeries(
            ctx = ctx,
            layout = layout,
            viewport = viewport,
            series = listOf(
                ChartSeries(
                    name = "",
                    color = color,
                    points = points.map { (x, y) -> ChartDataPoint("", x, y) },
                ),
            ),
            theme = theme,
            selection = null,
            showPoints = false,
        )
    }

    fun drawStockVolumes(
        ctx: ContextApi,
        layout: CartesianLayout,
        viewport: ChartViewport,
        points: List<OhlcPoint>,
        candleWidthRatio: Float,
        theme: ChartTheme,
    ) {
        val maximum = viewport.yMax.coerceAtLeast(1f)
        val scale = CartesianScale(layout.plot, viewport)
        val visibleCount = points.count { it.x in viewport.xMin..viewport.xMax }.coerceAtLeast(1)
        val bodyWidth = layout.plot.width / visibleCount * candleWidthRatio.coerceIn(0.2f, 0.9f)
        points.forEach { point ->
            val volume = point.volume ?: return@forEach
            val x = scale.toPixelX(point.x)
            if (x < layout.plot.left - bodyWidth || x > layout.plot.right + bodyWidth) return@forEach
            val top = scale.toPixelY(volume.coerceIn(0f, maximum))
            val color = if (point.close >= point.open) theme.upColor else theme.downColor
            ctx.fillStyle(color.toChartColor())
            ctx.fillRect(
                x - bodyWidth / 2f,
                top,
                bodyWidth,
                (layout.plot.bottom - top).coerceAtLeast(1f),
            )
        }
    }

    fun drawStockLegend(
        ctx: ContextApi,
        left: Float,
        baseline: Float,
        lines: List<StockAverageLine>,
        values: Map<Int, Float?>,
        theme: ChartTheme,
    ) {
        var x = left
        ctx.font(theme.fontSize)
        ctx.textAlign(TextAlign.LEFT)
        lines.forEach { line ->
            val value = values[line.period]
            val text = if (value == null) line.label else "${line.label}:${formatValue(value)}"
            ctx.fillStyle(line.color.toChartColor())
            ctx.fillText(text, x, baseline)
            x += text.length * theme.fontSize * 0.58f + 12f
        }
    }

    fun drawPieSlices(
        ctx: ContextApi,
        centerX: Float,
        centerY: Float,
        outerRadius: Float,
        innerRadius: Float,
        slices: List<ChartSlice>,
        startAngleDeg: Float,
        selection: ChartSelection?,
        theme: ChartTheme,
        showPercent: Boolean,
    ) {
        val total = slices.sumOf { it.value.coerceAtLeast(0f).toDouble() }
            .toFloat()
            .coerceAtLeast(1e-6f)
        var angle = startAngleDeg * PI.toFloat() / 180f
        slices.forEachIndexed { idx, slice ->
            val safeValue = slice.value.coerceAtLeast(0f)
            val sweep = safeValue / total * (2 * PI).toFloat()
            val selected = selection is ChartSelection.Slice && selection.sliceIndex == idx
            val radius = if (selected) outerRadius + 6f else outerRadius
            ctx.beginPath()
            ctx.moveTo(
                centerX + innerRadius * cos(angle),
                centerY + innerRadius * sin(angle),
            )
            ctx.arc(centerX, centerY, radius, angle, angle + sweep, false)
            ctx.arc(centerX, centerY, innerRadius, angle + sweep, angle, true)
            ctx.closePath()
            ctx.fillStyle(slice.color.toChartColor())
            ctx.fill()
            if (selected) {
                ctx.strokeStyle(Color.WHITE)
                ctx.lineWidth(2.5f)
                ctx.stroke()
            }
            if (showPercent) {
                val mid = angle + sweep / 2f
                val labelR = (radius + innerRadius) / 2f
                val lx = centerX + labelR * cos(mid)
                val ly = centerY + labelR * sin(mid)
                ctx.font(theme.fontSize)
                ctx.fillStyle(theme.textColor.toChartColor())
                ctx.textAlign(TextAlign.CENTER)
                val percent = (safeValue / total * 100f).let { formatValue(it) }
                ctx.fillText("$percent%", lx, ly)
            }
            angle += sweep
        }
    }

    fun drawRadar(
        ctx: ContextApi,
        centerX: Float,
        centerY: Float,
        radius: Float,
        dimensions: List<RadarDimension>,
        series: List<RadarSeries>,
        theme: ChartTheme,
        selection: ChartSelection?,
    ) {
        if (dimensions.isEmpty()) return
        val count = dimensions.size
        val angleStep = (2 * PI).toFloat() / count
        val startAngle = -PI.toFloat() / 2f
        val selectedDim = (selection as? ChartSelection.Radar)?.dimensionIndex
        val selectedSeries = (selection as? ChartSelection.Radar)?.seriesIndex

        for (level in 1..4) {
            val r = radius * level / 4f
            ctx.beginPath()
            for (i in 0 until count) {
                val angle = startAngle + angleStep * i
                val x = centerX + r * cos(angle)
                val y = centerY + r * sin(angle)
                if (i == 0) ctx.moveTo(x, y) else ctx.lineTo(x, y)
            }
            ctx.closePath()
            ctx.strokeStyle(theme.gridColor.toChartColor())
            ctx.lineWidth(1f)
            ctx.stroke()
        }
        dimensions.forEachIndexed { i, dim ->
            val angle = startAngle + angleStep * i
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            val axisSelected = selectedDim == i
            ctx.beginPath()
            ctx.moveTo(centerX, centerY)
            ctx.lineTo(x, y)
            ctx.strokeStyle(
                if (axisSelected) theme.primaryColor.toChartColor() else theme.gridColor.toChartColor(),
            )
            ctx.lineWidth(if (axisSelected) 2f else 1f)
            ctx.stroke()
            ctx.font(theme.fontSize)
            ctx.fillStyle(
                if (axisSelected) theme.primaryColor.toChartColor() else theme.textColor.toChartColor(),
            )
            ctx.textAlign(TextAlign.CENTER)
            ctx.fillText(dim.label, x + 12f * cos(angle), y + 12f * sin(angle))
        }
        series.forEachIndexed { sIdx, s ->
            val isSelectedSeries = selectedSeries == sIdx
            val dimOthers = selectedSeries != null && !isSelectedSeries
            val fillAlpha = when {
                isSelectedSeries -> 0x66
                dimOthers -> 0x18
                else -> 0x44
            }
            val strokeAlpha = if (dimOthers) 0x66 else 0xFF
            ctx.beginPath()
            s.values.forEachIndexed { i, value ->
                val dim = dimensions.getOrNull(i) ?: return@forEachIndexed
                val ratio = PolarScale.radarValueRatio(value, dim.maxValue)
                val angle = startAngle + angleStep * i
                val x = centerX + radius * ratio * cos(angle)
                val y = centerY + radius * ratio * sin(angle)
                if (i == 0) ctx.moveTo(x, y) else ctx.lineTo(x, y)
            }
            ctx.closePath()
            ctx.fillStyle(Color(s.color.withAlpha(fillAlpha)))
            ctx.fill()
            ctx.strokeStyle(Color(s.color.withAlpha(strokeAlpha)))
            ctx.lineWidth(if (isSelectedSeries) theme.lineWidth + 1f else theme.lineWidth)
            ctx.stroke()
            s.values.forEachIndexed { i, value ->
                val dim = dimensions.getOrNull(i) ?: return@forEachIndexed
                val ratio = PolarScale.radarValueRatio(value, dim.maxValue)
                val angle = startAngle + angleStep * i
                val x = centerX + radius * ratio * cos(angle)
                val y = centerY + radius * ratio * sin(angle)
                val selected = selection is ChartSelection.Radar &&
                    selection.seriesIndex == sIdx && selection.dimensionIndex == i
                drawMarker(ctx, x, y, s.color, if (selected) 7f else 4f, selected)
            }
        }
    }

    private fun pathThroughPoints(
        ctx: ContextApi,
        points: List<Pair<Float, Float>>,
        smooth: Boolean,
        startFromIndex: Int = 0,
    ) {
        if (points.isEmpty()) return
        if (startFromIndex == 0) {
            ctx.moveTo(points[0].first, points[0].second)
        }
        if (!smooth || points.size < 3) {
            val from = if (startFromIndex == 0) 1 else startFromIndex
            for (i in from until points.size) {
                ctx.lineTo(points[i].first, points[i].second)
            }
            return
        }
        val start = if (startFromIndex == 0) 0 else startFromIndex - 1
        for (i in start until points.size - 1) {
            val p0 = points[if (i == 0) 0 else i - 1]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[if (i + 2 < points.size) i + 2 else i + 1]
            val cp1x = p1.first + (p2.first - p0.first) / 6f
            val cp1y = p1.second + (p2.second - p0.second) / 6f
            val cp2x = p2.first - (p3.first - p1.first) / 6f
            val cp2y = p2.second - (p3.second - p1.second) / 6f
            ctx.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, p2.first, p2.second)
        }
    }

    /** 选中态保持自身颜色，外扩 + 白色描边强调。 */
    private fun drawMarker(
        ctx: ContextApi,
        px: Float,
        py: Float,
        color: Long,
        radius: Float,
        selected: Boolean,
    ) {
        if (selected) {
            ctx.beginPath()
            ctx.fillStyle(Color(color.withAlpha(0x33)))
            ctx.arc(px, py, radius + 4f, 0f, (2 * PI).toFloat(), false)
            ctx.fill()
        }
        ctx.beginPath()
        ctx.fillStyle(color.toChartColor())
        ctx.arc(px, py, radius, 0f, (2 * PI).toFloat(), false)
        ctx.fill()
        if (selected) {
            ctx.beginPath()
            ctx.strokeStyle(Color.WHITE)
            ctx.lineWidth(2f)
            ctx.arc(px, py, radius, 0f, (2 * PI).toFloat(), false)
            ctx.stroke()
        }
    }

    private fun strokeRect(
        ctx: ContextApi,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        color: Long,
    ) {
        ctx.beginPath()
        ctx.moveTo(left, top)
        ctx.lineTo(left + width, top)
        ctx.lineTo(left + width, top + height)
        ctx.lineTo(left, top + height)
        ctx.closePath()
        ctx.strokeStyle(Color.WHITE)
        ctx.lineWidth(2f)
        ctx.stroke()
        ctx.beginPath()
        ctx.moveTo(left, top)
        ctx.lineTo(left + width, top)
        ctx.lineTo(left + width, top + height)
        ctx.lineTo(left, top + height)
        ctx.closePath()
        ctx.strokeStyle(color.toChartColor())
        ctx.lineWidth(1.5f)
        ctx.stroke()
    }

    private fun formatValue(value: Float): String {
        return if (value == value.toLong().toFloat()) {
            value.toLong().toString()
        } else {
            val rounded = (value * 10f).toInt() / 10f
            rounded.toString()
        }
    }

    private fun ContextApi.fillRect(left: Float, top: Float, width: Float, height: Float) {
        beginPath()
        moveTo(left, top)
        lineTo(left + width, top)
        lineTo(left + width, top + height)
        lineTo(left, top + height)
        closePath()
        fill()
    }
}

internal fun resolveXAxisTickTextAlign(x: Float, plot: PlotRect): TextAlign = when {
    x <= plot.left + 0.5f -> TextAlign.LEFT
    x >= plot.right - 0.5f -> TextAlign.RIGHT
    else -> TextAlign.CENTER
}

internal fun resolveDynamicXAxisTickCount(
    availableWidth: Float,
    labels: List<String>,
    fontSize: Float,
    minimumGap: Float = 4f,
): Int {
    if (labels.isEmpty()) return 0
    if (labels.size == 1) return 1
    val estimatedLabelWidth = labels.maxOf { estimateAxisLabelWidth(it, fontSize) }
        .coerceAtLeast(fontSize)
    val slotWidth = estimatedLabelWidth + minimumGap
    val countByWidth = (availableWidth.coerceAtLeast(0f) / slotWidth).toInt() + 1
    return countByWidth.coerceIn(2, labels.size)
}

internal fun estimateAxisLabelWidth(text: String, fontSize: Float): Float =
    text.fold(0f) { width, char ->
        width + if (char.code <= 0x7F) fontSize * 0.5f else fontSize
    }
