package com.tencent.kuiklybase.chart.advanced

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.ContextApi
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextAlign
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chart.config.ChartTheme
import com.tencent.kuiklybase.chart.core.withAlpha
import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.model.ChartSlice
import com.tencent.kuiklybase.chart.model.OhlcPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

internal sealed interface AdvancedChartData {
    val size: Int
    fun selection(index: Int): AdvancedChartSelection?

    data class DualAxis(val items: List<DualAxisPoint>) : AdvancedChartData {
        override val size get() = items.size
        override fun selection(index: Int) = items.getOrNull(index)?.let {
            AdvancedChartSelection(AdvancedChartKind.DUAL_AXIS_BAR, index, it.label, it.barValue)
        }
    }

    data class Waterfall(val items: List<WaterfallPoint>) : AdvancedChartData {
        override val size get() = items.size
        override fun selection(index: Int) = items.getOrNull(index)?.let {
            AdvancedChartSelection(AdvancedChartKind.WATERFALL, index, it.label, it.value)
        }
    }

    data class Histogram(val items: List<HistogramBin>) : AdvancedChartData {
        override val size get() = items.size
        override fun selection(index: Int) = items.getOrNull(index)?.let {
            AdvancedChartSelection(AdvancedChartKind.HISTOGRAM, index, it.label, it.value)
        }
    }

    data class Bullet(val items: List<BulletChartItem>) : AdvancedChartData {
        override val size get() = items.size
        override fun selection(index: Int) = items.getOrNull(index)?.let {
            AdvancedChartSelection(AdvancedChartKind.BULLET, index, it.label, it.actual)
        }
    }

    data class Slices(
        val kind: AdvancedChartKind,
        val items: List<ChartSlice>,
    ) : AdvancedChartData {
        override val size get() = items.size
        override fun selection(index: Int) = items.getOrNull(index)?.let {
            AdvancedChartSelection(kind, index, it.label, it.value)
        }
    }

    data class Sunburst(val items: List<SunburstNode>) : AdvancedChartData {
        override val size get() = items.size
        override fun selection(index: Int) = items.getOrNull(index)?.let {
            AdvancedChartSelection(AdvancedChartKind.SUNBURST, index, it.label, it.value)
        }
    }

    data class NestedPie(val items: List<NestedPieSlice>) : AdvancedChartData {
        override val size get() = items.size
        override fun selection(index: Int) = items.getOrNull(index)?.let {
            AdvancedChartSelection(AdvancedChartKind.NESTED_PIE, index, it.label, it.value)
        }
    }

    data class Ohlc(val items: List<OhlcPoint>) : AdvancedChartData {
        override val size get() = items.size
        override fun selection(index: Int) = items.getOrNull(index)?.let {
            AdvancedChartSelection(AdvancedChartKind.OHLC, index, it.label, it.close)
        }
    }

    data class Points(
        val kind: AdvancedChartKind,
        val items: List<ChartDataPoint>,
    ) : AdvancedChartData {
        override val size get() = items.size
        override fun selection(index: Int) = items.getOrNull(index)?.let {
            AdvancedChartSelection(kind, index, it.label, it.y)
        }
    }

    data class PointFigure(val items: List<PointFigureColumn>) : AdvancedChartData {
        override val size get() = items.size
        override fun selection(index: Int) = items.getOrNull(index)?.let {
            AdvancedChartSelection(AdvancedChartKind.POINT_FIGURE, index, it.label, it.count.toFloat())
        }
    }
}

class AdvancedChartView internal constructor(
    private val kind: AdvancedChartKind,
    private val dataProvider: () -> AdvancedChartData,
) : ComposeView<AdvancedChartAttr, AdvancedChartEvent>() {
    private var selectedIndex by observable(-1)
    private var tooltip by observable("")
    private var viewportStart by observable(0f)
    private var viewportEnd by observable(1f)
    private var canvasWidth = 0f
    private var canvasHeight = 0f
    private var panLastX = 0f
    private var pinchStartDistance = 0f
    private var pinchStart = 0f
    private var pinchEnd = 1f
    private var pinching = false
    private var viewportInitialized = false

    override fun createAttr() = AdvancedChartAttr()
    override fun createEvent() = AdvancedChartEvent()

    private fun supportsViewport() = kind == AdvancedChartKind.STOCK_AREA ||
        kind == AdvancedChartKind.STOCK_LINE || kind == AdvancedChartKind.KAGI

    private fun updateViewport(start: Float, end: Float) {
        val span = (end - start).coerceIn(0.16f, 1f)
        val nextStart = start.coerceIn(0f, 1f - span)
        viewportStart = nextStart
        viewportEnd = nextStart + span
        event.onViewportChange?.invoke(viewportStart, viewportEnd)
    }

    private fun resetViewport() {
        val ratio = attr.interaction.initialVisibleRatio.coerceIn(0.16f, 1f)
        updateViewport(1f - ratio, 1f)
        selectedIndex = -1
        tooltip = ""
    }

    override fun body(): ViewBuilder {
        val chart = this
        return {
            View {
                attr {
                    flex(1f)
                    backgroundColor(chart.attr.theme.backgroundColor.toChartColor())
                }
                event {
                    click { params ->
                        if (!chart.attr.interaction.enableTap) return@click
                        val data = chart.dataProvider()
                        val index = AdvancedChartRenderer.hitTest(
                            chart.kind, data, params.x, params.y, chart.canvasWidth, chart.canvasHeight,
                            chart.viewportStart, chart.viewportEnd,
                        )
                        chart.selectedIndex = if (index == chart.selectedIndex) -1 else index
                        val selection = data.selection(chart.selectedIndex)
                        chart.tooltip = selection?.let { item ->
                            item.value?.let { "${item.label}: $it" } ?: item.label
                        }.orEmpty()
                        if (selection != null) chart.event.onItemClick?.invoke(selection)
                    }
                    doubleClick {
                        if (chart.supportsViewport() && chart.attr.interaction.enableReset) chart.resetViewport()
                    }
                    pan { params ->
                        if (!chart.supportsViewport() || !chart.attr.interaction.enablePan || chart.pinching) return@pan
                        when (params.state) {
                            "start" -> chart.panLastX = params.x
                            "move" -> {
                                val span = chart.viewportEnd - chart.viewportStart
                                val delta = -(params.x - chart.panLastX) / (chart.canvasWidth - 46f).coerceAtLeast(1f) * span
                                chart.panLastX = params.x
                                chart.updateViewport(chart.viewportStart + delta, chart.viewportEnd + delta)
                            }
                        }
                    }
                    touchDown { params ->
                        if (!chart.supportsViewport() || !chart.attr.interaction.enableScale || params.touches.size < 2) return@touchDown
                        val a = params.touches[0]
                        val b = params.touches[1]
                        chart.pinching = true
                        chart.pinchStartDistance = hypot(a.x - b.x, a.y - b.y).coerceAtLeast(1f)
                        chart.pinchStart = chart.viewportStart
                        chart.pinchEnd = chart.viewportEnd
                    }
                    touchMove { params ->
                        if (!chart.pinching || params.touches.size < 2) return@touchMove
                        val a = params.touches[0]
                        val b = params.touches[1]
                        val factor = hypot(a.x - b.x, a.y - b.y).coerceAtLeast(1f) / chart.pinchStartDistance
                        val oldSpan = chart.pinchEnd - chart.pinchStart
                        val newSpan = (oldSpan / factor).coerceIn(0.16f, 1f)
                        val focalRatio = (((a.x + b.x) / 2f - 34f) / (chart.canvasWidth - 46f).coerceAtLeast(1f))
                            .coerceIn(0f, 1f)
                        val focalData = chart.pinchStart + oldSpan * focalRatio
                        chart.updateViewport(focalData - newSpan * focalRatio, focalData + newSpan * (1f - focalRatio))
                    }
                    touchUp { chart.pinching = false }
                }
                Canvas({ attr { flex(1f) } }) { context, width, height ->
                    chart.canvasWidth = width
                    chart.canvasHeight = height
                    if (!chart.viewportInitialized) {
                        chart.viewportInitialized = true
                        val ratio = chart.attr.interaction.initialVisibleRatio.coerceIn(0.16f, 1f)
                        chart.viewportStart = 1f - ratio
                    }
                    AdvancedChartRenderer.draw(
                        context, width, height, chart.kind, chart.dataProvider(), chart.attr.theme.resolved(),
                        chart.selectedIndex, chart.viewportStart, chart.viewportEnd,
                    )
                }
                vif({ chart.tooltip.isNotEmpty() }) {
                    View {
                        attr {
                            positionAbsolute()
                            top(8f)
                            right(8f)
                            padding(7f, 9f, 7f, 9f)
                            borderRadius(6f)
                            backgroundColor(Color(0xE62C3542))
                        }
                        Text {
                            attr {
                                text(chart.tooltip)
                                fontSize(11f)
                                color(Color.WHITE)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Long.toChartColor() = Color(this)

private fun ViewContainer<*, *>.advancedChart(
    kind: AdvancedChartKind,
    provider: () -> AdvancedChartData,
    viewport: Boolean = false,
    init: AdvancedChartView.() -> Unit,
) {
    addChild(AdvancedChartView(kind, provider)) {
        if (viewport) {
            attr {
                interaction {
                    enablePan = true
                    enableScale = true
                    enableReset = true
                    initialVisibleRatio = 0.65f
                }
            }
        }
        init()
    }
}

fun ViewContainer<*, *>.DualAxisBarChart(provider: () -> ObservableList<DualAxisPoint>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.DUAL_AXIS_BAR, { AdvancedChartData.DualAxis(provider().toList()) }, init = init)

fun ViewContainer<*, *>.WaterfallChart(provider: () -> ObservableList<WaterfallPoint>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.WATERFALL, { AdvancedChartData.Waterfall(provider().toList()) }, init = init)

fun ViewContainer<*, *>.HistogramChart(provider: () -> ObservableList<HistogramBin>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.HISTOGRAM, { AdvancedChartData.Histogram(provider().toList()) }, init = init)

fun ViewContainer<*, *>.BulletChart(provider: () -> ObservableList<BulletChartItem>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.BULLET, { AdvancedChartData.Bullet(provider().toList()) }, init = init)

fun ViewContainer<*, *>.HalfDonutChart(provider: () -> ObservableList<ChartSlice>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.HALF_DONUT, { AdvancedChartData.Slices(AdvancedChartKind.HALF_DONUT, provider().toList()) }, init = init)

fun ViewContainer<*, *>.RoseChart(provider: () -> ObservableList<ChartSlice>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.ROSE, { AdvancedChartData.Slices(AdvancedChartKind.ROSE, provider().toList()) }, init = init)

fun ViewContainer<*, *>.SunburstChart(provider: () -> ObservableList<SunburstNode>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.SUNBURST, { AdvancedChartData.Sunburst(provider().toList()) }, init = init)

fun ViewContainer<*, *>.NestedPieChart(provider: () -> ObservableList<NestedPieSlice>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.NESTED_PIE, { AdvancedChartData.NestedPie(provider().toList()) }, init = init)

fun ViewContainer<*, *>.OhlcChart(provider: () -> ObservableList<OhlcPoint>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.OHLC, { AdvancedChartData.Ohlc(provider().toList()) }, init = init)

fun ViewContainer<*, *>.StockAreaChart(provider: () -> ObservableList<ChartDataPoint>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.STOCK_AREA, { AdvancedChartData.Points(AdvancedChartKind.STOCK_AREA, provider().toList()) }, true, init)

fun ViewContainer<*, *>.StockLineChart(provider: () -> ObservableList<ChartDataPoint>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.STOCK_LINE, { AdvancedChartData.Points(AdvancedChartKind.STOCK_LINE, provider().toList()) }, true, init)

fun ViewContainer<*, *>.RenkoChart(provider: () -> ObservableList<ChartDataPoint>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.RENKO, { AdvancedChartData.Points(AdvancedChartKind.RENKO, provider().toList()) }, init = init)

fun ViewContainer<*, *>.KagiChart(provider: () -> ObservableList<ChartDataPoint>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.KAGI, { AdvancedChartData.Points(AdvancedChartKind.KAGI, provider().toList()) }, true, init)

fun ViewContainer<*, *>.PointFigureChart(provider: () -> ObservableList<PointFigureColumn>, init: AdvancedChartView.() -> Unit) =
    advancedChart(AdvancedChartKind.POINT_FIGURE, { AdvancedChartData.PointFigure(provider().toList()) }, init = init)

private object AdvancedChartRenderer {
    fun draw(
        ctx: ContextApi,
        width: Float,
        height: Float,
        kind: AdvancedChartKind,
        data: AdvancedChartData,
        theme: ChartTheme,
        selected: Int,
        viewportStart: Float,
        viewportEnd: Float,
    ) {
        when (kind) {
            AdvancedChartKind.DUAL_AXIS_BAR -> drawDualAxis(ctx, width, height, (data as AdvancedChartData.DualAxis).items, theme, selected)
            AdvancedChartKind.WATERFALL -> drawWaterfall(ctx, width, height, (data as AdvancedChartData.Waterfall).items, theme, selected)
            AdvancedChartKind.HISTOGRAM -> drawHistogram(ctx, width, height, (data as AdvancedChartData.Histogram).items, theme, selected)
            AdvancedChartKind.BULLET -> drawBullet(ctx, width, height, (data as AdvancedChartData.Bullet).items, theme, selected)
            AdvancedChartKind.HALF_DONUT -> drawHalfDonut(ctx, width, height, (data as AdvancedChartData.Slices).items, theme, selected)
            AdvancedChartKind.ROSE -> drawRose(ctx, width, height, (data as AdvancedChartData.Slices).items, theme, selected)
            AdvancedChartKind.SUNBURST -> drawSunburst(ctx, width, height, (data as AdvancedChartData.Sunburst).items, theme, selected)
            AdvancedChartKind.NESTED_PIE -> drawNestedPie(ctx, width, height, (data as AdvancedChartData.NestedPie).items, theme, selected)
            AdvancedChartKind.OHLC -> drawOhlc(ctx, width, height, (data as AdvancedChartData.Ohlc).items, theme, selected)
            AdvancedChartKind.STOCK_AREA, AdvancedChartKind.STOCK_LINE -> drawStock(
                ctx, width, height, (data as AdvancedChartData.Points).items, theme, selected,
                viewportStart, viewportEnd, area = kind == AdvancedChartKind.STOCK_AREA,
            )
            AdvancedChartKind.RENKO -> drawRenko(ctx, width, height, (data as AdvancedChartData.Points).items, theme, selected)
            AdvancedChartKind.KAGI -> drawKagi(ctx, width, height, (data as AdvancedChartData.Points).items, theme, selected, viewportStart, viewportEnd)
            AdvancedChartKind.POINT_FIGURE -> drawPointFigure(ctx, width, height, (data as AdvancedChartData.PointFigure).items, theme, selected)
        }
    }

    fun hitTest(
        kind: AdvancedChartKind,
        data: AdvancedChartData,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        viewportStart: Float,
        viewportEnd: Float,
    ): Int {
        if (data.size == 0 || width <= 0f || height <= 0f) return -1
        return when (kind) {
            AdvancedChartKind.HALF_DONUT -> halfDonutIndex(x, y, width, height, (data as AdvancedChartData.Slices).items)
            AdvancedChartKind.ROSE -> roseIndex(x, y, width, height, (data as AdvancedChartData.Slices).items)
            AdvancedChartKind.SUNBURST -> sunburstIndex(x, y, width, height, (data as AdvancedChartData.Sunburst).items)
            AdvancedChartKind.NESTED_PIE -> nestedPieIndex(x, y, width, height, (data as AdvancedChartData.NestedPie).items)
            AdvancedChartKind.BULLET ->
                ((y - 20f) / ((height - 32f) / data.size.coerceAtLeast(1))).toInt().coerceIn(0, data.size - 1)
            AdvancedChartKind.STOCK_AREA, AdvancedChartKind.STOCK_LINE, AdvancedChartKind.KAGI -> {
                val ratio = ((x - 34f) / (width - 46f).coerceAtLeast(1f)).coerceIn(0f, 1f)
                ((viewportStart + (viewportEnd - viewportStart) * ratio) * (data.size - 1)).toInt()
                    .coerceIn(0, data.size - 1)
            }
            else -> (((x - 34f) / (width - 46f).coerceAtLeast(1f)) * data.size).toInt().coerceIn(0, data.size - 1)
        }
    }

    private fun halfDonutIndex(x: Float, y: Float, width: Float, height: Float, items: List<ChartSlice>): Int {
        val cx = width / 2f; val cy = height * 0.72f
        val outer = minOf(width * 0.34f, height * 0.56f); val distance = hypot(x - cx, y - cy)
        if (distance !in (outer * 0.58f)..(outer + 8f) || y > cy) return -1
        val angle = normalizedAngle(atan2(y - cy, x - cx))
        return weightedIndex(angle - PI.toFloat(), PI.toFloat(), items.map { it.value })
    }

    private fun roseIndex(x: Float, y: Float, width: Float, height: Float, items: List<ChartSlice>): Int {
        if (items.isEmpty()) return -1
        val cx = width / 2f; val cy = height / 2f; val distance = hypot(x - cx, y - cy)
        val step = (2 * PI).toFloat() / items.size
        val angle = normalizedAngle(atan2(y - cy, x - cx) + PI.toFloat() / 2f)
        val index = (angle / step).toInt().coerceIn(0, items.lastIndex)
        val max = items.maxOf { it.value }.coerceAtLeast(1f)
        val radius = minOf(width, height) * 0.35f * items[index].value.coerceAtLeast(0f) / max
        return if (distance <= radius + 8f) index else -1
    }

    private fun sunburstIndex(x: Float, y: Float, width: Float, height: Float, items: List<SunburstNode>): Int {
        if (items.isEmpty()) return -1
        val cx = width / 2f; val cy = height / 2f; val distance = hypot(x - cx, y - cy)
        val maxDepth = items.maxOf { it.depth }.coerceAtLeast(0)
        val outer = minOf(width, height) * 0.38f; val ringWidth = outer / (maxDepth + 1)
        if (distance > outer + 6f) return -1
        val depth = (distance / ringWidth).toInt().coerceIn(0, maxDepth)
        val level = items.withIndex().filter { it.value.depth == depth }
        val localIndex = weightedIndex(
            normalizedAngle(atan2(y - cy, x - cx) + PI.toFloat() / 2f),
            (2 * PI).toFloat(),
            level.map { it.value.value },
        )
        return level.getOrNull(localIndex)?.index ?: -1
    }

    private fun nestedPieIndex(x: Float, y: Float, width: Float, height: Float, items: List<NestedPieSlice>): Int {
        val cx = width / 2f; val cy = height / 2f; val radius = minOf(width, height) * 0.36f
        val distance = hypot(x - cx, y - cy)
        val ring = when {
            distance <= radius * 0.55f -> NestedPieRing.INNER
            distance in (radius * 0.58f)..(radius + 8f) -> NestedPieRing.OUTER
            else -> return -1
        }
        val ringItems = items.withIndex().filter { it.value.ring == ring }
        val localIndex = weightedIndex(
            normalizedAngle(atan2(y - cy, x - cx) + PI.toFloat() / 2f),
            (2 * PI).toFloat(),
            ringItems.map { it.value.value },
        )
        return ringItems.getOrNull(localIndex)?.index ?: -1
    }

    private fun normalizedAngle(angle: Float): Float {
        val full = (2 * PI).toFloat()
        var result = angle % full
        if (result < 0f) result += full
        return result
    }

    private fun weightedIndex(angle: Float, sweep: Float, values: List<Float>): Int {
        if (values.isEmpty() || angle !in 0f..sweep) return -1
        val total = values.sumOf { it.coerceAtLeast(0f).toDouble() }.toFloat()
        if (total <= 0f) return -1
        val target = angle / sweep * total
        var accumulated = 0f
        values.forEachIndexed { index, value ->
            accumulated += value.coerceAtLeast(0f)
            if (target < accumulated || index == values.lastIndex) return index
        }
        return -1
    }

    private fun axes(ctx: ContextApi, width: Float, height: Float, theme: ChartTheme): Plot {
        val plot = Plot(34f, 12f, width - 12f, height - 30f)
        ctx.strokeStyle(theme.gridColor.toChartColor())
        ctx.lineWidth(1f)
        repeat(5) { index ->
            val y = plot.top + plot.height * index / 4f
            ctx.beginPath(); ctx.moveTo(plot.left, y); ctx.lineTo(plot.right, y); ctx.stroke()
        }
        ctx.strokeStyle(theme.axisColor.toChartColor())
        ctx.beginPath(); ctx.moveTo(plot.left, plot.top); ctx.lineTo(plot.left, plot.bottom); ctx.lineTo(plot.right, plot.bottom); ctx.stroke()
        return plot
    }

    private fun drawLabels(ctx: ContextApi, plot: Plot, labels: List<String>, theme: ChartTheme) {
        if (labels.isEmpty()) return
        ctx.font(theme.fontSize.coerceAtMost(10f))
        ctx.fillStyle(theme.textColor.toChartColor())
        ctx.textAlign(TextAlign.CENTER)
        val slot = plot.width / labels.size
        labels.forEachIndexed { index, label -> ctx.fillText(label, plot.left + slot * (index + 0.5f), plot.bottom + 14f) }
    }

    private fun drawDualAxis(ctx: ContextApi, width: Float, height: Float, items: List<DualAxisPoint>, theme: ChartTheme, selected: Int) {
        if (items.isEmpty()) return
        val plot = axes(ctx, width, height, theme)
        val barMax = items.maxOf { it.barValue }.coerceAtLeast(1f)
        val lineMax = items.maxOf { it.lineValue }.coerceAtLeast(1f)
        val slot = plot.width / items.size
        items.forEachIndexed { index, item ->
            val barHeight = plot.height * item.barValue / barMax
            ctx.fillStyle((item.barColor ?: if (index == selected) 0xFFFA8C16 else theme.primaryColor).toChartColor())
            ctx.fillRectPath(plot.left + slot * index + slot * 0.22f, plot.bottom - barHeight, slot * 0.48f, barHeight)
        }
        ctx.beginPath(); ctx.strokeStyle(0xFFFA8C16.toChartColor()); ctx.lineWidth(theme.lineWidth)
        items.forEachIndexed { index, item ->
            val x = plot.left + slot * (index + 0.5f)
            val y = plot.bottom - plot.height * item.lineValue / lineMax
            if (index == 0) ctx.moveTo(x, y) else ctx.lineTo(x, y)
        }
        ctx.stroke()
        drawLabels(ctx, plot, items.map { it.label }, theme)
    }

    private fun drawWaterfall(ctx: ContextApi, width: Float, height: Float, items: List<WaterfallPoint>, theme: ChartTheme, selected: Int) {
        if (items.isEmpty()) return
        val plot = axes(ctx, width, height, theme)
        var running = 0f
        var maxValue = 0f
        var minValue = 0f
        items.forEach { item ->
            val end = if (item.isTotal) item.value else running + item.value
            maxValue = maxOf(maxValue, running, end)
            minValue = minOf(minValue, running, end)
            if (!item.isTotal) running = end
        }
        val range = (maxValue - minValue).coerceAtLeast(1f)
        val mapY: (Float) -> Float = { value -> plot.bottom - plot.height * (value - minValue) / range }
        val slot = plot.width / items.size
        running = 0f
        items.forEachIndexed { index, item ->
            val start = if (item.isTotal) 0f else running
            val end = if (item.isTotal) item.value else running + item.value
            val top = mapY(maxOf(start, end)); val bottom = mapY(minOf(start, end))
            val color = item.color ?: when { index == selected -> 0xFFFA8C16; item.isTotal -> theme.primaryColor; item.value >= 0f -> 0xFF52C41A; else -> 0xFFFF4D4F }
            ctx.fillStyle(color.toChartColor())
            ctx.fillRectPath(plot.left + slot * index + slot * 0.18f, top, slot * 0.64f, (bottom - top).coerceAtLeast(2f))
            if (!item.isTotal) running = end
        }
        drawLabels(ctx, plot, items.map { it.label }, theme)
    }

    private fun drawHistogram(ctx: ContextApi, width: Float, height: Float, items: List<HistogramBin>, theme: ChartTheme, selected: Int) {
        if (items.isEmpty()) return
        val plot = axes(ctx, width, height, theme)
        val max = items.maxOf { it.value }.coerceAtLeast(1f)
        val slot = plot.width / items.size
        items.forEachIndexed { index, item ->
            val h = plot.height * item.value / max
            ctx.fillStyle((item.color ?: if (index == selected) 0xFFFA8C16 else theme.primaryColor).toChartColor())
            ctx.fillRectPath(plot.left + slot * index, plot.bottom - h, (slot - 1f).coerceAtLeast(1f), h)
        }
        drawLabels(ctx, plot, items.map { it.label }, theme)
    }

    private fun drawBullet(ctx: ContextApi, width: Float, height: Float, items: List<BulletChartItem>, theme: ChartTheme, selected: Int) {
        if (items.isEmpty()) return
        val left = 50f; val right = width - 16f; val rowHeight = (height - 28f) / items.size
        items.forEachIndexed { index, item ->
            val y = 16f + index * rowHeight
            val w = right - left
            val ranges = item.ranges.map { it.coerceIn(0f, 1f) }.sortedDescending()
            val shades = listOf(0xFFF0F0F0, 0xFFD9D9D9, 0xFFBFBFBF)
            ranges.take(3).forEachIndexed { rangeIndex, ratio ->
                ctx.fillStyle(shades[rangeIndex].toChartColor()); ctx.fillRectPath(left, y, w * ratio, 22f)
            }
            ctx.fillStyle((item.color ?: if (index == selected) 0xFFFA8C16 else theme.primaryColor).toChartColor())
            ctx.fillRectPath(left, y + 6f, w * item.actual.coerceIn(0f, 1f), 10f)
            val marker = left + w * item.target.coerceIn(0f, 1f)
            ctx.strokeStyle(0xFFFF4D4F.toChartColor()); ctx.lineWidth(2f)
            ctx.beginPath(); ctx.moveTo(marker, y + 2f); ctx.lineTo(marker, y + 20f); ctx.stroke()
            ctx.font(theme.fontSize.coerceAtMost(10f)); ctx.fillStyle(theme.textColor.toChartColor()); ctx.textAlign(TextAlign.LEFT)
            ctx.fillText(item.label, 6f, y + 15f)
        }
    }

    private fun drawHalfDonut(ctx: ContextApi, width: Float, height: Float, items: List<ChartSlice>, theme: ChartTheme, selected: Int) {
        val total = items.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1e-6f)
        val cx = width / 2f; val cy = height * 0.72f; val outer = minOf(width * 0.34f, height * 0.56f); val inner = outer * 0.58f
        var angle = PI.toFloat()
        items.forEachIndexed { index, item ->
            val sweep = PI.toFloat() * item.value.coerceAtLeast(0f) / total
            ringSlice(ctx, cx, cy, inner, outer + if (index == selected) 7f else 0f, angle, angle + sweep, item.color.toChartColor(), index == selected)
            angle += sweep
        }
        ctx.fillStyle(theme.textColor.toChartColor()); ctx.font(theme.fontSize + 2f); ctx.textAlign(TextAlign.CENTER)
        ctx.fillText(items.sumOf { it.value.toDouble() }.toInt().toString(), cx, cy - 6f)
    }

    private fun drawRose(ctx: ContextApi, width: Float, height: Float, items: List<ChartSlice>, theme: ChartTheme, selected: Int) {
        if (items.isEmpty()) return
        val cx = width / 2f; val cy = height / 2f; val max = items.maxOf { it.value }.coerceAtLeast(1f)
        val step = (2 * PI).toFloat() / items.size
        items.forEachIndexed { index, item ->
            val start = -PI.toFloat() / 2f + index * step + 0.025f
            sector(ctx, cx, cy, minOf(width, height) * 0.35f * item.value / max + if (index == selected) 8f else 0f,
                start, start + step - 0.05f, item.color.toChartColor(), index == selected)
        }
    }

    private fun drawSunburst(ctx: ContextApi, width: Float, height: Float, items: List<SunburstNode>, theme: ChartTheme, selected: Int) {
        if (items.isEmpty()) return
        val cx = width / 2f; val cy = height / 2f; val maxDepth = items.maxOf { it.depth }.coerceAtLeast(0)
        val outerRadius = minOf(width, height) * 0.38f; val ringWidth = outerRadius / (maxDepth + 1)
        for (depth in 0..maxDepth) {
            val level = items.withIndex().filter { it.value.depth == depth }
            val total = level.sumOf { it.value.value.toDouble() }.toFloat().coerceAtLeast(1e-6f)
            var angle = -PI.toFloat() / 2f
            level.forEach { indexed ->
                val sweep = (2 * PI).toFloat() * indexed.value.value.coerceAtLeast(0f) / total
                ringSlice(ctx, cx, cy, ringWidth * depth, ringWidth * (depth + 1) + if (indexed.index == selected) 6f else 0f,
                    angle, angle + sweep, indexed.value.color.toChartColor(), indexed.index == selected)
                angle += sweep
            }
        }
    }

    private fun drawNestedPie(ctx: ContextApi, width: Float, height: Float, items: List<NestedPieSlice>, theme: ChartTheme, selected: Int) {
        val cx = width / 2f; val cy = height / 2f; val radius = minOf(width, height) * 0.36f
        NestedPieRing.entries.forEach { ring ->
            val ringItems = items.withIndex().filter { it.value.ring == ring }
            val total = ringItems.sumOf { it.value.value.toDouble() }.toFloat().coerceAtLeast(1e-6f)
            var angle = -PI.toFloat() / 2f
            ringItems.forEach { indexed ->
                val sweep = (2 * PI).toFloat() * indexed.value.value.coerceAtLeast(0f) / total
                val inner = if (ring == NestedPieRing.INNER) 0f else radius * 0.58f
                val outer = if (ring == NestedPieRing.INNER) radius * 0.55f else radius + if (indexed.index == selected) 7f else 0f
                ringSlice(ctx, cx, cy, inner, outer, angle, angle + sweep, indexed.value.color.toChartColor(), indexed.index == selected)
                angle += sweep
            }
        }
    }

    private fun drawOhlc(ctx: ContextApi, width: Float, height: Float, items: List<OhlcPoint>, theme: ChartTheme, selected: Int) {
        if (items.isEmpty()) return
        val plot = axes(ctx, width, height, theme)
        val min = items.minOf { it.low }; val max = items.maxOf { it.high }; val range = (max - min).coerceAtLeast(1f); val slot = plot.width / items.size
        items.forEachIndexed { index, item ->
            val x = plot.left + slot * (index + 0.5f)
            fun y(value: Float) = plot.bottom - plot.height * (value - min) / range
            ctx.strokeStyle((if (index == selected) 0xFFFA8C16 else if (item.close >= item.open) theme.upColor else theme.downColor).toChartColor())
            ctx.lineWidth(if (index == selected) 3f else 1.5f); ctx.beginPath()
            ctx.moveTo(x, y(item.high)); ctx.lineTo(x, y(item.low)); ctx.moveTo(x - slot * 0.28f, y(item.open)); ctx.lineTo(x, y(item.open))
            ctx.moveTo(x, y(item.close)); ctx.lineTo(x + slot * 0.28f, y(item.close)); ctx.stroke()
        }
        drawLabels(ctx, plot, items.map { it.label }, theme)
    }

    private fun drawStock(ctx: ContextApi, width: Float, height: Float, items: List<ChartDataPoint>, theme: ChartTheme, selected: Int,
        viewportStart: Float, viewportEnd: Float, area: Boolean) {
        if (items.isEmpty()) return
        val plot = axes(ctx, width, height, theme); val visible = visiblePoints(items, viewportStart, viewportEnd)
        val rawMin = visible.minOf { it.y }; val rawMax = visible.maxOf { it.y }; val pad = (rawMax - rawMin).coerceAtLeast(1f) * 0.12f
        val min = rawMin - pad; val max = rawMax + pad; val points = items.mapIndexed { index, item ->
            viewportX(plot, index / (items.size - 1f).coerceAtLeast(1f), viewportStart, viewportEnd) to
                plot.bottom - plot.height * (item.y - min) / (max - min).coerceAtLeast(1f)
        }
        withPlotClip(ctx, plot) {
            if (area) {
                ctx.beginPath(); ctx.moveTo(points.first().first, plot.bottom); points.forEach { ctx.lineTo(it.first, it.second) }
                ctx.lineTo(points.last().first, plot.bottom); ctx.closePath(); ctx.fillStyle(Color(theme.primaryColor.withAlpha(0x55))); ctx.fill()
            }
            ctx.beginPath(); ctx.strokeStyle(theme.primaryColor.toChartColor()); ctx.lineWidth(theme.lineWidth)
            points.forEachIndexed { index, point -> if (index == 0) ctx.moveTo(point.first, point.second) else ctx.lineTo(point.first, point.second) }; ctx.stroke()
            drawSelectedGuide(ctx, plot, selected, items.size, viewportStart, viewportEnd)
        }
    }

    private fun drawRenko(ctx: ContextApi, width: Float, height: Float, items: List<ChartDataPoint>, theme: ChartTheme, selected: Int) {
        if (items.isEmpty()) return
        val plot = axes(ctx, width, height, theme); val min = items.minOf { it.y }; val max = items.maxOf { it.y }; val range = (max - min + 1f).coerceAtLeast(1f)
        val slot = plot.width / items.size; val brickHeight = plot.height / range
        items.forEachIndexed { index, item ->
            val up = index == 0 || item.y >= items[index - 1].y
            val top = plot.bottom - brickHeight * (item.y - min + 1f)
            ctx.fillStyle((if (index == selected) 0xFFFA8C16 else if (up) theme.upColor else theme.downColor).toChartColor())
            ctx.fillRectPath(plot.left + slot * index + 1f, top, (slot - 2f).coerceAtLeast(1f), brickHeight.coerceAtLeast(2f) - 2f)
        }
        drawLabels(ctx, plot, items.map { it.label }, theme)
    }

    private fun drawKagi(ctx: ContextApi, width: Float, height: Float, items: List<ChartDataPoint>, theme: ChartTheme, selected: Int,
        viewportStart: Float, viewportEnd: Float) {
        if (items.size < 2) return
        val plot = axes(ctx, width, height, theme); val visible = visiblePoints(items, viewportStart, viewportEnd)
        val min = visible.minOf { it.y }; val max = visible.maxOf { it.y }; val range = (max - min).coerceAtLeast(1f)
        withPlotClip(ctx, plot) {
            for (index in 0 until items.lastIndex) {
                val x1 = viewportX(plot, index / (items.size - 1f), viewportStart, viewportEnd)
                val x2 = viewportX(plot, (index + 1) / (items.size - 1f), viewportStart, viewportEnd)
                val y1 = plot.bottom - plot.height * (items[index].y - min) / range
                val y2 = plot.bottom - plot.height * (items[index + 1].y - min) / range
                val rising = items[index + 1].y >= items[index].y
                ctx.strokeStyle((if (index == selected) 0xFFFA8C16 else if (rising) theme.upColor else theme.downColor).toChartColor())
                ctx.lineWidth(if (rising) theme.lineWidth + 1.5f else theme.lineWidth); ctx.beginPath()
                ctx.moveTo(x1, y1); ctx.lineTo(x1, y2); ctx.lineTo(x2, y2); ctx.stroke()
            }
            drawSelectedGuide(ctx, plot, selected, items.size, viewportStart, viewportEnd)
        }
    }

    private fun drawPointFigure(ctx: ContextApi, width: Float, height: Float, items: List<PointFigureColumn>, theme: ChartTheme, selected: Int) {
        if (items.isEmpty()) return
        val plot = axes(ctx, width, height, theme); val maxCount = items.maxOf { it.count }.coerceAtLeast(1); val slot = plot.width / items.size
        ctx.font((plot.height / (maxCount + 1)).coerceIn(12f, 18f)); ctx.textAlign(TextAlign.CENTER)
        items.forEachIndexed { index, item ->
            ctx.fillStyle((if (index == selected) 0xFFFA8C16 else if (item.rising) theme.upColor else theme.downColor).toChartColor())
            repeat(item.count.coerceAtLeast(0)) { row ->
                ctx.fillText(if (item.rising) "X" else "O", plot.left + slot * (index + 0.5f), plot.bottom - 12f - row * 18f)
            }
        }
        drawLabels(ctx, plot, items.map { it.label }, theme)
    }

    private fun drawSelectedGuide(ctx: ContextApi, plot: Plot, selected: Int, count: Int, start: Float, end: Float) {
        if (selected !in 0 until count) return
        val x = viewportX(plot, selected / (count - 1f).coerceAtLeast(1f), start, end)
        if (x !in plot.left..plot.right) return
        ctx.setLineDash(listOf(3f, 3f)); ctx.strokeStyle(0x991677FF.toChartColor()); ctx.lineWidth(1f)
        ctx.beginPath(); ctx.moveTo(x, plot.top); ctx.lineTo(x, plot.bottom); ctx.stroke(); ctx.setLineDash(emptyList())
    }

    private fun visiblePoints(items: List<ChartDataPoint>, start: Float, end: Float): List<ChartDataPoint> =
        items.filterIndexed { index, _ -> index / (items.size - 1f).coerceAtLeast(1f) in start..end }.ifEmpty { items }

    private fun viewportX(plot: Plot, ratio: Float, start: Float, end: Float) =
        plot.left + plot.width * (ratio - start) / (end - start).coerceAtLeast(0.0001f)

    private inline fun withPlotClip(ctx: ContextApi, plot: Plot, draw: () -> Unit) {
        ctx.save(); ctx.beginPath(); ctx.moveTo(plot.left, plot.top); ctx.lineTo(plot.right, plot.top)
        ctx.lineTo(plot.right, plot.bottom); ctx.lineTo(plot.left, plot.bottom); ctx.closePath(); ctx.clipPathIntersect(); draw(); ctx.restore()
    }

    private fun sector(ctx: ContextApi, cx: Float, cy: Float, radius: Float, start: Float, end: Float, color: Color, selected: Boolean) {
        ctx.beginPath(); ctx.moveTo(cx, cy); ctx.arc(cx, cy, radius, start, end, false); ctx.closePath(); ctx.fillStyle(color); ctx.fill()
        if (selected) { ctx.strokeStyle(Color.WHITE); ctx.lineWidth(3f); ctx.stroke() }
    }

    private fun ringSlice(ctx: ContextApi, cx: Float, cy: Float, inner: Float, outer: Float, start: Float, end: Float, color: Color, selected: Boolean) {
        ctx.beginPath(); ctx.moveTo(cx + inner * cos(start), cy + inner * sin(start)); ctx.arc(cx, cy, outer, start, end, false)
        if (inner > 0f) ctx.arc(cx, cy, inner, end, start, true) else ctx.lineTo(cx, cy)
        ctx.closePath(); ctx.fillStyle(color); ctx.fill(); ctx.strokeStyle(Color.WHITE); ctx.lineWidth(if (selected) 3f else 1.5f); ctx.stroke()
    }

    private fun ContextApi.fillRectPath(left: Float, top: Float, width: Float, height: Float) {
        beginPath(); moveTo(left, top); lineTo(left + width, top); lineTo(left + width, top + height); lineTo(left, top + height); closePath(); fill()
    }

    private data class Plot(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val width get() = right - left
        val height get() = bottom - top
    }
}
