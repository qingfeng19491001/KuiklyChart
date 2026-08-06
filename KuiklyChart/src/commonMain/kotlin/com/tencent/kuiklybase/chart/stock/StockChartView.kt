package com.tencent.kuiklybase.chart.stock

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.ContextApi
import com.tencent.kuiklybase.chart.config.StockChartAttr
import com.tencent.kuiklybase.chart.core.ChartCanvasRenderer
import com.tencent.kuiklybase.chart.core.withPlotClip
import com.tencent.kuiklybase.chart.core.cartesian.CartesianInteractiveView
import com.tencent.kuiklybase.chart.core.cartesian.CartesianLayout
import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.model.ChartSelection
import com.tencent.kuiklybase.chart.model.ChartViewport
import com.tencent.kuiklybase.chart.model.OhlcPoint
import kotlin.math.abs

class StockChartView(
    private val ohlcProvider: () -> ObservableList<OhlcPoint>,
) : CartesianInteractiveView<StockChartAttr>() {

    private var lastSnapshot: List<OhlcPoint>? = null

    override fun createAttr() = StockChartAttr()

    override fun onBeforeResetViewport() {
        lastSnapshot = null
    }

    override fun syncDataFromProvider() {
        val data = ohlcProvider().toList()
        val bounds = ChartViewport.fromOhlc(data)
        val changed = data != lastSnapshot
        lastSnapshot = data
        applyViewportBounds(bounds, changed)
    }

    override fun drawPlot(
        context: ContextApi,
        width: Float,
        height: Float,
        layout: CartesianLayout,
        viewport: ChartViewport,
        selection: ChartSelection?,
    ) {
        val data = ohlcProvider().toList()
        val theme = attr.theme.resolved()
        ChartCanvasRenderer.drawGrid(context, layout, viewport, theme, attr.grid.show)
        ChartCanvasRenderer.drawAxes(
            context,
            layout,
            viewport,
            theme,
            attr.xAxis.show,
            attr.yAxis.show,
            xTicks = ChartCanvasRenderer.axisTicksFromOhlc(data),
        )
        context.withPlotClip(layout.plot) {
            ChartCanvasRenderer.drawCandlesticks(
                context, layout, viewport, data, theme, selection,
                candleWidthRatio = attr.candleWidthRatio,
            )
        }
    }

    override fun onPlotClick(x: Float, y: Float) {
        val data = ohlcProvider().toList()
        if (data.isEmpty() || canvasWidth <= 0f) return
        val scale = currentScale()
        var bestIdx = -1
        var bestDist = 24f
        data.forEachIndexed { idx, p ->
            val dist = abs(scale.toPixelX(p.x) - x)
            if (dist <= bestDist) {
                bestDist = dist
                bestIdx = idx
            }
        }
        if (bestIdx < 0) return
        val candle = data[bestIdx]
        selection = ChartSelection.Cartesian(0, bestIdx, candle.label)
        event.onSelectionChange?.invoke(selection)
        event.onPointClick?.invoke(
            ChartDataPoint(candle.label, candle.x, candle.close),
            0,
            bestIdx,
        )
        showSelectionTooltip(
            text = "${candle.label.ifEmpty { candle.x.toString() }}  O:${candle.open} H:${candle.high} L:${candle.low} C:${candle.close}",
            localX = x,
            localY = y,
            crossX = scale.toPixelX(candle.x),
        )
    }
}

fun ViewContainer<*, *>.StockChart(
    ohlcProvider: () -> ObservableList<OhlcPoint>,
    init: StockChartView.() -> Unit,
) {
    addChild(StockChartView(ohlcProvider), init)
}
