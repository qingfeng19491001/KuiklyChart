package com.kuikly.kuiklychart

import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.ContextApi
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextAlign
import com.tencent.kuikly.core.views.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

internal fun ViewContainer<*, *>.demoVariantSection(
    index: Int,
    title: String,
    chartHeight: Float = 236f,
    chart: ViewBuilder,
) = demoVariantSectionContent(index, title, null, chartHeight, chart)

internal fun ViewContainer<*, *>.stockDemoVariantSection(
    index: Int,
    title: String,
    subtitle: String,
    chartHeight: Float = 236f,
    chart: ViewBuilder,
) = demoVariantSectionContent(index, title, subtitle, chartHeight, chart)

private fun ViewContainer<*, *>.demoVariantSectionContent(
    index: Int,
    title: String,
    subtitle: String?,
    chartHeight: Float,
    chart: ViewBuilder,
) {
    View {
        attr {
            marginTop(16f)
            marginLeft(16f)
            marginRight(16f)
            flexDirectionRow()
        }
        View {
            attr {
                size(24f, 24f)
                borderRadius(12f)
                allCenter()
                backgroundColor(Color(0xFFE6F4FF))
                marginRight(8f)
            }
            Text {
                attr {
                    text(index.toString())
                    fontSize(12f)
                    fontWeightSemiBold()
                    color(Color(0xFF1677FF))
                }
            }
        }
        View {
            attr { flex(1f) }
            Text {
                attr {
                    text(title)
                    fontSize(15f)
                    fontWeightSemiBold()
                    color(Color(0xFF262626))
                }
            }
            vif({ subtitle != null }) {
                Text {
                    attr {
                        text(subtitle.orEmpty())
                        fontSize(12f)
                        color(Color(0xFF8C8C8C))
                        marginTop(2f)
                    }
                }
            }
        }
    }
    View {
        attr {
            height(chartHeight)
            marginTop(9f)
            marginLeft(16f)
            marginRight(16f)
            padding(12f)
            borderRadius(12f)
            backgroundColor(Color.WHITE)
            boxShadow(BoxShadow(0f, 4f, 14f, Color(0x12000000)))
        }
        chart()
    }
}

internal enum class SpecialDemoChartKind {
    DUAL_AXIS_BAR,
    WATERFALL,
    HISTOGRAM,
    BULLET,
    HALF_DONUT,
    ROSE,
    SUNBURST,
    NESTED_PIE,
    OHLC,
    STOCK_AREA,
    STOCK_LINE,
    RENKO,
    KAGI,
    POINT_FIGURE,
}

internal fun ViewContainer<*, *>.SpecialDemoChart(kind: SpecialDemoChartKind) {
    addChild(InteractiveSpecialDemoChartView(kind)) {
        attr { flex(1f) }
    }
}

private class InteractiveSpecialDemoChartAttr : ComposeAttr()

private class InteractiveSpecialDemoChartView(
    private val kind: SpecialDemoChartKind,
) : ComposeView<InteractiveSpecialDemoChartAttr, ComposeEvent>() {
    private var selectedIndex by observable(-1)
    private var tooltip by observable("")
    private var canvasWidth = 0f
    private var canvasHeight = 0f
    private var viewportStart by observable(0f)
    private var viewportEnd by observable(1f)
    private var panLastX = 0f
    private var pinchStartDistance = 0f
    private var pinchViewportStart = 0f
    private var pinchViewportEnd = 1f
    private var pinching = false

    private fun supportsViewport(): Boolean = kind == SpecialDemoChartKind.STOCK_AREA ||
        kind == SpecialDemoChartKind.STOCK_LINE || kind == SpecialDemoChartKind.KAGI

    private fun resetViewport() {
        viewportStart = 0f
        viewportEnd = 1f
        selectedIndex = -1
        tooltip = ""
    }

    private fun updateViewport(start: Float, end: Float) {
        val span = (end - start).coerceIn(0.16f, 1f)
        var nextStart = start
        var nextEnd = nextStart + span
        if (nextStart < 0f) {
            nextStart = 0f
            nextEnd = span
        }
        if (nextEnd > 1f) {
            nextEnd = 1f
            nextStart = 1f - span
        }
        viewportStart = nextStart
        viewportEnd = nextEnd
    }

    override fun createAttr() = InteractiveSpecialDemoChartAttr()

    override fun createEvent() = ComposeEvent()

    override fun body(): ViewBuilder {
        val chart = this
        return {
            View {
                attr { flex(1f) }
                event {
                    click { params ->
                        val hit = SpecialDemoChartRenderer.hitTest(
                            chart.kind,
                            params.x,
                            params.y,
                            chart.canvasWidth,
                            chart.canvasHeight,
                            chart.viewportStart,
                            chart.viewportEnd,
                        )
                        chart.selectedIndex = if (hit.index == chart.selectedIndex) -1 else hit.index
                        chart.tooltip = if (chart.selectedIndex >= 0) hit.text else ""
                    }
                    doubleClick {
                        if (chart.supportsViewport()) chart.resetViewport()
                    }
                    pan { params ->
                        if (!chart.supportsViewport() || chart.pinching) return@pan
                        when (params.state) {
                            "start" -> chart.panLastX = params.x
                            "move" -> {
                                val plotWidth = (chart.canvasWidth - 46f).coerceAtLeast(1f)
                                val span = chart.viewportEnd - chart.viewportStart
                                val delta = -(params.x - chart.panLastX) / plotWidth * span
                                chart.panLastX = params.x
                                chart.updateViewport(chart.viewportStart + delta, chart.viewportEnd + delta)
                            }
                        }
                    }
                    touchDown { params ->
                        if (!chart.supportsViewport() || params.touches.size < 2) return@touchDown
                        val a = params.touches[0]
                        val b = params.touches[1]
                        chart.pinching = true
                        chart.pinchStartDistance = hypot(a.x - b.x, a.y - b.y).coerceAtLeast(1f)
                        chart.pinchViewportStart = chart.viewportStart
                        chart.pinchViewportEnd = chart.viewportEnd
                    }
                    touchMove { params ->
                        if (!chart.supportsViewport() || params.touches.size < 2) return@touchMove
                        val a = params.touches[0]
                        val b = params.touches[1]
                        if (!chart.pinching) {
                            chart.pinching = true
                            chart.pinchStartDistance = hypot(a.x - b.x, a.y - b.y).coerceAtLeast(1f)
                            chart.pinchViewportStart = chart.viewportStart
                            chart.pinchViewportEnd = chart.viewportEnd
                        }
                        val distance = hypot(a.x - b.x, a.y - b.y).coerceAtLeast(1f)
                        val factor = distance / chart.pinchStartDistance
                        val oldSpan = chart.pinchViewportEnd - chart.pinchViewportStart
                        val newSpan = (oldSpan / factor).coerceIn(0.16f, 1f)
                        val focalPixel = (a.x + b.x) / 2f
                        val focalRatio = ((focalPixel - 34f) / (chart.canvasWidth - 46f).coerceAtLeast(1f)).coerceIn(0f, 1f)
                        val focalData = chart.pinchViewportStart + oldSpan * focalRatio
                        chart.updateViewport(
                            focalData - newSpan * focalRatio,
                            focalData + newSpan * (1f - focalRatio),
                        )
                    }
                    touchUp {
                        chart.pinching = false
                    }
                }
                Canvas({ attr { flex(1f) } }) { context, width, height ->
                    chart.canvasWidth = width
                    chart.canvasHeight = height
                    SpecialDemoChartRenderer.draw(
                        context, width, height, chart.kind, chart.selectedIndex,
                        chart.viewportStart, chart.viewportEnd,
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

private object SpecialDemoChartRenderer {
    private val blue = Color(0xFF1677FF)
    private val cyan = Color(0xFF36CFC9)
    private val orange = Color(0xFFFA8C16)
    private val green = Color(0xFF52C41A)
    private val red = Color(0xFFFF4D4F)
    private val purple = Color(0xFF722ED1)
    private val grid = Color(0xFFF0F0F0)
    private val text = Color(0xFF595959)

    fun draw(
        context: ContextApi,
        width: Float,
        height: Float,
        kind: SpecialDemoChartKind,
        selected: Int = -1,
        viewportStart: Float = 0f,
        viewportEnd: Float = 1f,
    ) {
        when (kind) {
            SpecialDemoChartKind.DUAL_AXIS_BAR -> drawDualAxis(context, width, height, selected)
            SpecialDemoChartKind.WATERFALL -> drawWaterfall(context, width, height, selected)
            SpecialDemoChartKind.HISTOGRAM -> drawHistogram(context, width, height, selected)
            SpecialDemoChartKind.BULLET -> drawBullet(context, width, height)
            SpecialDemoChartKind.HALF_DONUT -> drawHalfDonut(context, width, height, selected)
            SpecialDemoChartKind.ROSE -> drawRose(context, width, height, selected)
            SpecialDemoChartKind.SUNBURST -> drawSunburst(context, width, height, selected)
            SpecialDemoChartKind.NESTED_PIE -> drawNestedPie(context, width, height, selected)
            SpecialDemoChartKind.OHLC -> drawOhlc(context, width, height, selected)
            SpecialDemoChartKind.STOCK_AREA -> drawStockArea(context, width, height, selected, viewportStart, viewportEnd)
            SpecialDemoChartKind.STOCK_LINE -> drawStockLine(context, width, height, selected, viewportStart, viewportEnd)
            SpecialDemoChartKind.RENKO -> drawRenko(context, width, height, selected)
            SpecialDemoChartKind.KAGI -> drawKagi(context, width, height, selected, viewportStart, viewportEnd)
            SpecialDemoChartKind.POINT_FIGURE -> drawPointFigure(context, width, height, selected)
        }
    }

    data class HitResult(val index: Int, val text: String)

    fun hitTest(
        kind: SpecialDemoChartKind,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        viewportStart: Float = 0f,
        viewportEnd: Float = 1f,
    ): HitResult {
        if (width <= 0f || height <= 0f) return HitResult(-1, "")
        return when (kind) {
            SpecialDemoChartKind.HALF_DONUT -> polarHit(x, y, width, height, 4, true, listOf("移动端 38%", "桌面端 27%", "小程序 20%", "线下终端 15%"))
            SpecialDemoChartKind.ROSE -> polarHit(x, y, width, height, 8, false, (1..8).map { "区域 $it · ${40 + it * 7}" })
            SpecialDemoChartKind.SUNBURST -> polarHit(x, y, width, height, 6, false, listOf("华东 / 上海", "华东 / 杭州", "华南 / 深圳", "华南 / 广州", "华北 / 北京", "西南 / 成都"))
            SpecialDemoChartKind.NESTED_PIE -> polarHit(x, y, width, height, 6, false, listOf("自然流量 / 搜索", "自然流量 / 推荐", "活动 / 会场", "活动 / 分享", "付费 / 信息流", "付费 / 搜索"))
            SpecialDemoChartKind.OHLC -> cartesianHit(x, width, 10, "OHLC")
            SpecialDemoChartKind.STOCK_AREA -> edgeCartesianHit(x, width, 12, "收盘价", viewportStart, viewportEnd)
            SpecialDemoChartKind.STOCK_LINE -> edgeCartesianHit(x, width, 12, "收盘价", viewportStart, viewportEnd)
            SpecialDemoChartKind.RENKO -> cartesianHit(x, width, 11, "砖块")
            SpecialDemoChartKind.KAGI -> edgeCartesianHit(x, width, 12, "转折", viewportStart, viewportEnd)
            SpecialDemoChartKind.POINT_FIGURE -> cartesianHit(x, width, 9, "列")
            SpecialDemoChartKind.DUAL_AXIS_BAR -> cartesianHit(x, width, 6, "月份")
            SpecialDemoChartKind.WATERFALL -> cartesianHit(x, width, 6, "项目")
            SpecialDemoChartKind.HISTOGRAM -> cartesianHit(x, width, 10, "区间")
            SpecialDemoChartKind.BULLET -> HitResult(((y - 24f) / ((height - 36f) / 3f)).toInt().coerceIn(0, 2), "实际值 / 目标值")
        }
    }

    private fun cartesianHit(x: Float, width: Float, count: Int, label: String): HitResult {
        val plotLeft = 34f
        val plotWidth = (width - 46f).coerceAtLeast(1f)
        val index = (((x - plotLeft) / plotWidth) * count).toInt().coerceIn(0, count - 1)
        return HitResult(index, "$label ${index + 1} · 点击选中")
    }

    private fun edgeCartesianHit(
        x: Float,
        width: Float,
        count: Int,
        label: String,
        viewportStart: Float,
        viewportEnd: Float,
    ): HitResult {
        val plotLeft = 34f
        val plotWidth = (width - 46f).coerceAtLeast(1f)
        val screenRatio = ((x - plotLeft) / plotWidth).coerceIn(0f, 1f)
        val dataRatio = viewportStart + (viewportEnd - viewportStart) * screenRatio
        val index = (dataRatio * (count - 1)).toInt().coerceIn(0, count - 1)
        return HitResult(index, "$label ${index + 1} · 点击选中")
    }

    private fun polarHit(x: Float, y: Float, width: Float, height: Float, count: Int, half: Boolean, labels: List<String>): HitResult {
        val cx = width / 2f
        val cy = if (half) height * 0.72f else height / 2f
        val dx = x - cx
        val dy = y - cy
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        val outer = minOf(width, height) * 0.42f
        if (distance > outer || (half && y > cy + 8f)) return HitResult(-1, "")
        var angle = kotlin.math.atan2(y - cy, x - cx)
        if (angle < 0f) angle += (2 * PI).toFloat()
        val normalized = if (half) ((angle - PI.toFloat()).let { if (it < 0f) it + (2 * PI).toFloat() else it }) / PI.toFloat() else angle / (2 * PI).toFloat()
        val index = (normalized * count).toInt().coerceIn(0, count - 1)
        return HitResult(index, labels.getOrElse(index) { "分类 ${index + 1}" })
    }

    private fun axes(ctx: ContextApi, width: Float, height: Float): Plot {
        val plot = Plot(34f, 12f, width - 12f, height - 30f)
        ctx.strokeStyle(grid)
        ctx.lineWidth(1f)
        for (i in 0..4) {
            val y = plot.top + plot.height * i / 4f
            ctx.beginPath()
            ctx.moveTo(plot.left, y)
            ctx.lineTo(plot.right, y)
            ctx.stroke()
        }
        ctx.strokeStyle(Color(0xFFD9D9D9))
        ctx.beginPath()
        ctx.moveTo(plot.left, plot.top)
        ctx.lineTo(plot.left, plot.bottom)
        ctx.lineTo(plot.right, plot.bottom)
        ctx.stroke()
        return plot
    }

    private fun drawAxisValues(
        ctx: ContextApi,
        plot: Plot,
        yMin: Float,
        yMax: Float,
        xCount: Int,
    ) {
        ctx.font(10f)
        ctx.fillStyle(text)
        ctx.textAlign(TextAlign.RIGHT)
        for (i in 0..4) {
            val ratio = i / 4f
            val value = yMax - (yMax - yMin) * ratio
            val y = plot.top + plot.height * ratio + 3f
            ctx.fillText(value.toInt().toString(), plot.left - 5f, y)
        }
        ctx.textAlign(TextAlign.CENTER)
        val count = xCount.coerceAtLeast(1)
        for (i in 0 until count) {
            val x = plot.left + plot.width * (i + 0.5f) / count
            ctx.fillText((i + 1).toString(), x, plot.bottom + 14f)
        }
    }

    private fun drawDualAxis(ctx: ContextApi, width: Float, height: Float, selected: Int) {
        val p = axes(ctx, width, height)
        val bars = listOf(42f, 68f, 55f, 82f, 73f, 92f)
        val line = listOf(31f, 46f, 38f, 61f, 58f, 76f)
        val slot = p.width / bars.size
        bars.forEachIndexed { i, value ->
            val h = p.height * value / 100f
            ctx.fillStyle(if (i == selected) orange else if (i % 2 == 0) blue else cyan)
            ctx.fillRect(p.left + slot * i + slot * 0.22f, p.bottom - h, slot * 0.48f, h)
        }
        ctx.beginPath()
        ctx.strokeStyle(orange)
        ctx.lineWidth(2.5f)
        line.forEachIndexed { i, value ->
            val x = p.left + slot * (i + 0.5f)
            val y = p.bottom - p.height * value / 80f
            if (i == 0) ctx.moveTo(x, y) else ctx.lineTo(x, y)
        }
        ctx.stroke()
        line.forEachIndexed { i, value ->
            val x = p.left + slot * (i + 0.5f)
            val y = p.bottom - p.height * value / 80f
            ctx.fillStyle(orange)
            ctx.beginPath()
            ctx.arc(x, y, 3.5f, 0f, (2 * PI).toFloat(), false)
            ctx.fill()
        }
        ctx.font(10f)
        ctx.textAlign(TextAlign.LEFT)
        ctx.fillStyle(blue)
        ctx.fillText("规模", p.left, p.top - 2f)
        ctx.textAlign(TextAlign.RIGHT)
        ctx.fillStyle(orange)
        ctx.fillText("增长率", p.right, p.top - 2f)
        ctx.textAlign(TextAlign.CENTER)
        ctx.fillStyle(text)
        bars.forEachIndexed { i, _ ->
            ctx.fillText("M${i + 1}", p.left + slot * (i + 0.5f), p.bottom + 14f)
        }
    }

    private fun drawWaterfall(ctx: ContextApi, width: Float, height: Float, selected: Int) {
        val p = axes(ctx, width, height)
        val deltas = listOf(70f, 24f, -18f, 31f, -12f, 95f)
        val slot = p.width / deltas.size
        var running = 0f
        deltas.forEachIndexed { i, delta ->
            val start = if (i == deltas.lastIndex) 0f else running
            val end = if (i == deltas.lastIndex) delta else running + delta
            val topValue = maxOf(start, end)
            val bottomValue = minOf(start, end)
            val top = p.bottom - p.height * topValue / 130f
            val bottom = p.bottom - p.height * bottomValue / 130f
            ctx.fillStyle(if (i == selected) orange else if (i == deltas.lastIndex) blue else if (delta >= 0f) green else red)
            ctx.fillRect(p.left + slot * i + slot * 0.18f, top, slot * 0.64f, (bottom - top).coerceAtLeast(2f))
            if (i != deltas.lastIndex) running = end
            if (i < deltas.lastIndex - 1) {
                val y = p.bottom - p.height * running / 130f
                ctx.setLineDash(listOf(3f, 3f))
                ctx.strokeStyle(Color(0xFFBFBFBF))
                ctx.beginPath()
                ctx.moveTo(p.left + slot * (i + 0.82f), y)
                ctx.lineTo(p.left + slot * (i + 1.18f), y)
                ctx.stroke()
                ctx.setLineDash(emptyList())
            }
        }
        ctx.font(10f)
        ctx.textAlign(TextAlign.CENTER)
        ctx.fillStyle(text)
        deltas.forEachIndexed { i, _ ->
            ctx.fillText(if (i == deltas.lastIndex) "总计" else "步骤${i + 1}", p.left + slot * (i + 0.5f), p.bottom + 14f)
        }
        ctx.textAlign(TextAlign.LEFT)
        ctx.fillText("增减变化", p.left, p.top - 2f)
    }

    private fun drawHistogram(ctx: ContextApi, width: Float, height: Float, selected: Int) {
        val p = axes(ctx, width, height)
        val bins = listOf(10f, 22f, 41f, 68f, 91f, 76f, 52f, 31f, 17f, 8f)
        val slot = p.width / bins.size
        bins.forEachIndexed { i, value ->
            val h = p.height * value / 100f
            ctx.fillStyle(if (i == selected) orange else Color(0xB31677FF))
            ctx.fillRect(p.left + slot * i, p.bottom - h, slot - 1f, h)
        }
        ctx.font(10f)
        ctx.textAlign(TextAlign.CENTER)
        ctx.fillStyle(text)
        bins.indices.forEach { i ->
            ctx.fillText("${i * 10}-${(i + 1) * 10}", p.left + slot * (i + 0.5f), p.bottom + 14f)
        }
        ctx.textAlign(TextAlign.LEFT)
        ctx.fillText("频数", p.left, p.top - 2f)
    }

    private fun drawBullet(ctx: ContextApi, width: Float, height: Float) {
        val left = 42f
        val right = width - 18f
        val rows = listOf(0.78f to 0.86f, 0.62f to 0.72f, 0.88f to 0.81f)
        rows.forEachIndexed { i, values ->
            val y = 34f + i * (height - 56f) / 3f
            val w = right - left
            ctx.fillStyle(Color(0xFFF0F0F0))
            ctx.fillRect(left, y, w, 22f)
            ctx.fillStyle(Color(0xFFD9D9D9))
            ctx.fillRect(left, y, w * 0.8f, 22f)
            ctx.fillStyle(Color(0xFFBFBFBF))
            ctx.fillRect(left, y, w * 0.55f, 22f)
            ctx.fillStyle(blue)
            ctx.fillRect(left, y + 6f, w * values.first, 10f)
            val marker = left + w * values.second
            ctx.strokeStyle(red)
            ctx.lineWidth(2f)
            ctx.beginPath()
            ctx.moveTo(marker, y + 2f)
            ctx.lineTo(marker, y + 20f)
            ctx.stroke()
            ctx.font(10f)
            ctx.textAlign(TextAlign.LEFT)
            ctx.fillStyle(text)
            ctx.fillText("指标${i + 1}", 8f, y + 15f)
            ctx.textAlign(TextAlign.RIGHT)
            ctx.fillText("实际 ${(values.first * 100).toInt()}%", right, y - 3f)
            ctx.fillText("目标 ${(values.second * 100).toInt()}%", right, y + 34f)
        }
        ctx.textAlign(TextAlign.LEFT)
        ctx.fillStyle(text)
        ctx.fillText("差 / 中 / 优", left, height - 8f)
    }

    private fun drawHalfDonut(ctx: ContextApi, width: Float, height: Float, selected: Int) {
        val cx = width / 2f
        val cy = height * 0.72f
        val outer = minOf(width * 0.34f, height * 0.56f)
        val inner = outer * 0.58f
        val values = listOf(38f, 27f, 20f, 15f)
        val colors = listOf(blue, cyan, purple, orange)
        var angle = PI.toFloat()
        values.forEachIndexed { i, value ->
            val sweep = PI.toFloat() * value / 100f
            ringSlice(ctx, cx, cy, inner, outer + if (i == selected) 7f else 0f, angle, angle + sweep, colors[i], i == selected)
            angle += sweep
        }
        ctx.fillStyle(text)
        ctx.font(13f)
        ctx.textAlign(TextAlign.CENTER)
        ctx.fillText("总计 1,070", cx, cy - 6f)
    }

    private fun drawRose(ctx: ContextApi, width: Float, height: Float, selected: Int) {
        val cx = width / 2f
        val cy = height / 2f
        val values = listOf(0.55f, 0.82f, 0.66f, 0.96f, 0.72f, 0.48f, 0.88f, 0.62f)
        val colors = listOf(blue, cyan, green, orange, purple, red, Color(0xFF2F54EB), Color(0xFF13C2C2))
        val step = (2 * PI).toFloat() / values.size
        values.forEachIndexed { i, value ->
            val start = -PI.toFloat() / 2f + i * step + 0.025f
            val end = start + step - 0.05f
            sector(ctx, cx, cy, minOf(width, height) * 0.35f * value + if (i == selected) 8f else 0f, start, end, colors[i], i == selected)
        }
    }

    private fun drawSunburst(ctx: ContextApi, width: Float, height: Float, selected: Int) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) * 0.34f
        ringSlice(ctx, cx, cy, 0f, r * 0.42f, 0f, (2 * PI).toFloat(), blue)
        val innerColors = listOf(cyan, purple, orange)
        var angle = -PI.toFloat() / 2f
        listOf(0.38f, 0.34f, 0.28f).forEachIndexed { i, ratio ->
            val sweep = (2 * PI).toFloat() * ratio
            ringSlice(ctx, cx, cy, r * 0.44f, r * 0.72f, angle, angle + sweep, innerColors[i])
            angle += sweep
        }
        val outerColors = listOf(Color(0xFF69B1FF), Color(0xFF5CDBD3), Color(0xFFB37FEB), Color(0xFFFFBB96), Color(0xFF95DE64), Color(0xFFFFD666))
        angle = -PI.toFloat() / 2f
        outerColors.forEachIndexed { i, color ->
            val sweep = (2 * PI).toFloat() / outerColors.size
            ringSlice(ctx, cx, cy, r * 0.74f, r + if (i == selected) 7f else 0f, angle, angle + sweep, color, i == selected)
            angle += sweep
        }
    }

    private fun drawNestedPie(ctx: ContextApi, width: Float, height: Float, selected: Int) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) * 0.35f
        var angle = -PI.toFloat() / 2f
        listOf(0.46f to blue, 0.31f to cyan, 0.23f to purple).forEach { (ratio, color) ->
            val sweep = (2 * PI).toFloat() * ratio
            ringSlice(ctx, cx, cy, 0f, r * 0.55f, angle, angle + sweep, color)
            angle += sweep
        }
        angle = -PI.toFloat() / 2f
        val outer = listOf(0.18f, 0.28f, 0.12f, 0.19f, 0.11f, 0.12f)
        val colors = listOf(Color(0xFF69B1FF), Color(0xFF0958D9), Color(0xFF5CDBD3), Color(0xFF08979C), Color(0xFFB37FEB), Color(0xFF531DAB))
        outer.forEachIndexed { i, ratio ->
            val sweep = (2 * PI).toFloat() * ratio
            ringSlice(ctx, cx, cy, r * 0.58f, r + if (i == selected) 7f else 0f, angle, angle + sweep, colors[i], i == selected)
            angle += sweep
        }
    }

    private fun drawOhlc(ctx: ContextApi, width: Float, height: Float, selected: Int) {
        val p = axes(ctx, width, height)
        val values = listOf(
            floatArrayOf(102f, 108f, 100f, 106f), floatArrayOf(106f, 110f, 103f, 104f),
            floatArrayOf(104f, 109f, 101f, 108f), floatArrayOf(108f, 114f, 106f, 112f),
            floatArrayOf(112f, 116f, 107f, 109f), floatArrayOf(109f, 115f, 108f, 114f),
            floatArrayOf(114f, 120f, 112f, 118f), floatArrayOf(118f, 121f, 113f, 115f),
            floatArrayOf(115f, 119f, 110f, 111f), floatArrayOf(111f, 122f, 109f, 120f),
        )
        val min = 96f
        val max = 124f
        drawAxisValues(ctx, p, min, max, values.size)
        val slot = p.width / values.size
        values.forEachIndexed { i, v ->
            val x = p.left + slot * (i + 0.5f)
            val yHigh = p.bottom - p.height * (v[1] - min) / (max - min)
            val yLow = p.bottom - p.height * (v[2] - min) / (max - min)
            val yOpen = p.bottom - p.height * (v[0] - min) / (max - min)
            val yClose = p.bottom - p.height * (v[3] - min) / (max - min)
            ctx.strokeStyle(if (i == selected) orange else if (v[3] >= v[0]) red else green)
            ctx.lineWidth(if (i == selected) 3f else 1.5f)
            ctx.beginPath()
            ctx.moveTo(x, yHigh)
            ctx.lineTo(x, yLow)
            ctx.moveTo(x - slot * 0.28f, yOpen)
            ctx.lineTo(x, yOpen)
            ctx.moveTo(x, yClose)
            ctx.lineTo(x + slot * 0.28f, yClose)
            ctx.stroke()
        }
        drawSelectedGuide(ctx, p, selected, values.size)
    }

    private fun drawStockArea(
        ctx: ContextApi,
        width: Float,
        height: Float,
        selected: Int,
        viewportStart: Float,
        viewportEnd: Float,
    ) {
        val p = axes(ctx, width, height)
        val values = listOf(102f, 106f, 104f, 111f, 109f, 114f, 118f, 115f, 111f, 120f, 123f, 119f)
        val visible = visibleValues(values, viewportStart, viewportEnd)
        val padding = ((visible.maxOrNull() ?: 126f) - (visible.minOrNull() ?: 98f)).coerceAtLeast(4f) * 0.12f
        val min = (visible.minOrNull() ?: 98f) - padding
        val max = (visible.maxOrNull() ?: 126f) + padding
        drawAxisValues(ctx, p, min, max, values.size)
        val points = values.mapIndexed { i, value ->
            val x = viewportPixelX(p, i / (values.size - 1f), viewportStart, viewportEnd)
            val y = p.bottom - p.height * (value - min) / (max - min)
            x to y
        }
        val gradient = ctx.createLinearGradient(0f, p.top, 0f, p.bottom)
        gradient.addColorStop(0f, Color(0x991677FF))
        gradient.addColorStop(1f, Color(0x081677FF))
        withPlotClip(ctx, p) {
            ctx.beginPath()
            ctx.moveTo(points.first().first, p.bottom)
            points.forEach { ctx.lineTo(it.first, it.second) }
            ctx.lineTo(points.last().first, p.bottom)
            ctx.closePath()
            ctx.fillStyle(gradient)
            ctx.fill()
            drawStockPath(ctx, points, selected)
            drawSelectedGuide(ctx, p, selected, values.size, true, viewportStart, viewportEnd)
        }
    }

    private fun drawStockLine(
        ctx: ContextApi,
        width: Float,
        height: Float,
        selected: Int,
        viewportStart: Float,
        viewportEnd: Float,
    ) {
        val p = axes(ctx, width, height)
        val values = listOf(102f, 106f, 104f, 111f, 109f, 114f, 118f, 115f, 111f, 120f, 123f, 119f)
        val visible = visibleValues(values, viewportStart, viewportEnd)
        val padding = ((visible.maxOrNull() ?: 126f) - (visible.minOrNull() ?: 98f)).coerceAtLeast(4f) * 0.12f
        val min = (visible.minOrNull() ?: 98f) - padding
        val max = (visible.maxOrNull() ?: 126f) + padding
        drawAxisValues(ctx, p, min, max, values.size)
        val points = values.mapIndexed { i, value ->
            viewportPixelX(p, i / (values.size - 1f), viewportStart, viewportEnd) to
                p.bottom - p.height * (value - min) / (max - min)
        }
        withPlotClip(ctx, p) {
            drawStockPath(ctx, points, selected)
            drawSelectedGuide(ctx, p, selected, values.size, true, viewportStart, viewportEnd)
        }
    }

    private fun drawStockPath(ctx: ContextApi, points: List<Pair<Float, Float>>, selected: Int) {
        ctx.beginPath()
        ctx.strokeStyle(blue)
        ctx.lineWidth(2.5f)
        points.forEachIndexed { i, point ->
            if (i == 0) ctx.moveTo(point.first, point.second) else ctx.lineTo(point.first, point.second)
        }
        ctx.stroke()
        if (selected in points.indices) {
            val point = points[selected]
            ctx.fillStyle(orange)
            ctx.beginPath()
            ctx.arc(point.first, point.second, 5f, 0f, (2 * PI).toFloat(), false)
            ctx.fill()
            ctx.strokeStyle(Color.WHITE)
            ctx.lineWidth(2f)
            ctx.stroke()
        }
    }

    private fun drawRenko(ctx: ContextApi, width: Float, height: Float, selected: Int) {
        val p = axes(ctx, width, height)
        val levels = listOf(2, 3, 4, 5, 4, 5, 6, 7, 6, 5, 6)
        drawAxisValues(ctx, p, 0f, 8f, levels.size)
        val slot = p.width / levels.size
        val brickH = p.height / 8f
        levels.forEachIndexed { i, level ->
            val up = i == 0 || level >= levels[i - 1]
            val left = p.left + slot * i + 1f
            val top = p.bottom - brickH * (level + 1)
            ctx.fillStyle(if (i == selected) orange else if (up) red else green)
            ctx.fillRect(left, top, slot - 2f, brickH - 2f)
            ctx.strokeStyle(Color.WHITE)
            ctx.lineWidth(1f)
            ctx.strokeRectPath(left, top, slot - 2f, brickH - 2f)
        }
        drawSelectedGuide(ctx, p, selected, levels.size)
    }

    private fun drawKagi(
        ctx: ContextApi,
        width: Float,
        height: Float,
        selected: Int,
        viewportStart: Float,
        viewportEnd: Float,
    ) {
        val p = axes(ctx, width, height)
        val values = listOf(2f, 5f, 3f, 7f, 4f, 8f, 6f, 9f, 5f, 7f, 4f, 8f)
        val visible = visibleValues(values, viewportStart, viewportEnd)
        val padding = ((visible.maxOrNull() ?: 10f) - (visible.minOrNull() ?: 0f)).coerceAtLeast(2f) * 0.12f
        val min = (visible.minOrNull() ?: 0f) - padding
        val max = (visible.maxOrNull() ?: 10f) + padding
        drawAxisValues(ctx, p, min, max, values.size)
        withPlotClip(ctx, p) {
            for (i in 0 until values.lastIndex) {
                val x1 = viewportPixelX(p, i / (values.size - 1f), viewportStart, viewportEnd)
                val x2 = viewportPixelX(p, (i + 1) / (values.size - 1f), viewportStart, viewportEnd)
                val y1 = p.bottom - p.height * (values[i] - min) / (max - min)
                val y2 = p.bottom - p.height * (values[i + 1] - min) / (max - min)
                val rising = values[i + 1] >= values[i]
                ctx.strokeStyle(if (i == selected) orange else if (rising) red else green)
                ctx.lineWidth(if (rising) 3.5f else 1.8f)
                ctx.beginPath()
                ctx.moveTo(x1, y1)
                ctx.lineTo(x1, y2)
                ctx.lineTo(x2, y2)
                ctx.stroke()
            }
            drawSelectedGuide(ctx, p, selected, values.size, true, viewportStart, viewportEnd)
        }
    }

    private fun drawPointFigure(ctx: ContextApi, width: Float, height: Float, selected: Int) {
        val p = axes(ctx, width, height)
        val columns = listOf(3, -4, 5, -3, 6, -5, 4, -3, 5)
        drawAxisValues(ctx, p, 0f, 8f, columns.size)
        val slot = p.width / columns.size
        ctx.font(16f)
        ctx.textAlign(TextAlign.CENTER)
        columns.forEachIndexed { i, value ->
            val count = kotlin.math.abs(value)
            val rising = value > 0
            ctx.fillStyle(if (i == selected) orange else if (rising) red else green)
            repeat(count) { row ->
                val x = p.left + slot * (i + 0.5f)
                val y = p.bottom - 14f - row * 18f
                ctx.fillText(if (rising) "X" else "O", x, y)
            }
        }
        drawSelectedGuide(ctx, p, selected, columns.size)
    }

    private fun drawSelectedGuide(
        ctx: ContextApi,
        p: Plot,
        selected: Int,
        count: Int,
        edgeAligned: Boolean = false,
        viewportStart: Float = 0f,
        viewportEnd: Float = 1f,
    ) {
        if (selected !in 0 until count) return
        val dataRatio = if (edgeAligned && count > 1) {
            selected / (count - 1f)
        } else {
            (selected + 0.5f) / count
        }
        val x = viewportPixelX(p, dataRatio, viewportStart, viewportEnd)
        if (x !in p.left..p.right) return
        ctx.setLineDash(listOf(3f, 3f))
        ctx.strokeStyle(Color(0x991677FF))
        ctx.lineWidth(1f)
        ctx.beginPath()
        ctx.moveTo(x, p.top)
        ctx.lineTo(x, p.bottom)
        ctx.stroke()
        ctx.setLineDash(emptyList())
    }

    private fun viewportPixelX(plot: Plot, dataRatio: Float, start: Float, end: Float): Float =
        plot.left + plot.width * (dataRatio - start) / (end - start).coerceAtLeast(0.0001f)

    private fun visibleValues(values: List<Float>, start: Float, end: Float): List<Float> {
        if (values.size < 2) return values
        return values.filterIndexed { index, _ ->
            val ratio = index / (values.size - 1f)
            ratio in start..end
        }.ifEmpty { values }
    }

    private inline fun withPlotClip(ctx: ContextApi, plot: Plot, draw: () -> Unit) {
        ctx.save()
        ctx.beginPath()
        ctx.moveTo(plot.left, plot.top)
        ctx.lineTo(plot.right, plot.top)
        ctx.lineTo(plot.right, plot.bottom)
        ctx.lineTo(plot.left, plot.bottom)
        ctx.closePath()
        ctx.clipPathIntersect()
        draw()
        ctx.restore()
    }

    private fun sector(ctx: ContextApi, cx: Float, cy: Float, radius: Float, start: Float, end: Float, color: Color, selected: Boolean = false) {
        ctx.beginPath()
        ctx.moveTo(cx, cy)
        ctx.arc(cx, cy, radius, start, end, false)
        ctx.closePath()
        ctx.fillStyle(color)
        ctx.fill()
        if (selected) {
            ctx.strokeStyle(Color.WHITE)
            ctx.lineWidth(3f)
            ctx.stroke()
        }
    }

    private fun ringSlice(ctx: ContextApi, cx: Float, cy: Float, inner: Float, outer: Float, start: Float, end: Float, color: Color, selected: Boolean = false) {
        ctx.beginPath()
        ctx.moveTo(cx + inner * cos(start), cy + inner * sin(start))
        ctx.arc(cx, cy, outer, start, end, false)
        ctx.arc(cx, cy, inner, end, start, true)
        ctx.closePath()
        ctx.fillStyle(color)
        ctx.fill()
        ctx.strokeStyle(Color.WHITE)
        ctx.lineWidth(if (selected) 3f else 1.5f)
        ctx.stroke()
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

    private fun ContextApi.strokeRectPath(left: Float, top: Float, width: Float, height: Float) {
        beginPath()
        moveTo(left, top)
        lineTo(left + width, top)
        lineTo(left + width, top + height)
        lineTo(left, top + height)
        closePath()
        stroke()
    }

    private data class Plot(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
    }
}
