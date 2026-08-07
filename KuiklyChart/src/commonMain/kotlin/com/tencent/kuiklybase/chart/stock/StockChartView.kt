package com.tencent.kuiklybase.chart.stock

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.ContextApi
import com.tencent.kuiklybase.chart.config.StockChartAttr
import com.tencent.kuiklybase.chart.config.ChartTheme
import com.tencent.kuiklybase.chart.config.resolveStockTheme
import com.tencent.kuiklybase.chart.core.ChartCanvasRenderer
import com.tencent.kuiklybase.chart.core.withPlotClip
import com.tencent.kuiklybase.chart.core.cartesian.CartesianInteractiveView
import com.tencent.kuiklybase.chart.core.cartesian.CartesianLayout
import com.tencent.kuiklybase.chart.core.cartesian.CartesianScale
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

    override fun resolvedTheme(): ChartTheme = resolveStockTheme(attr.theme, attr.preset)

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
        val theme = resolvedTheme()
        val showLegend = attr.movingAverages.show && attr.movingAverages.lines.isNotEmpty()
        val contentPlot = if (showLegend) layout.plot.copy(top = layout.plot.top + 18f) else layout.plot
        val showVolume = shouldShowVolumePanel(data, attr.volumePanel.show)
        val plots = splitStockPlots(contentPlot, showVolume, attr.volumePanel.heightRatio)
        val priceLayout = CartesianLayout(plots.price)
        ChartCanvasRenderer.drawGrid(context, priceLayout, viewport, theme, attr.grid.show)
        ChartCanvasRenderer.drawAxes(
            ctx = context,
            layout = priceLayout,
            viewport = viewport,
            theme = theme,
            showX = attr.xAxis.show && !showVolume,
            showY = attr.yAxis.show,
            xTicks = ChartCanvasRenderer.axisTicksFromOhlc(data),
        )
        context.withPlotClip(plots.price) {
            ChartCanvasRenderer.drawCandlesticks(
                context, priceLayout, viewport, data, theme, selection,
                candleWidthRatio = attr.candleWidthRatio,
            )
            if (attr.movingAverages.show) {
                attr.movingAverages.lines.forEach { line ->
                    ChartCanvasRenderer.drawStockAverageLine(
                        ctx = context,
                        layout = priceLayout,
                        viewport = viewport,
                        points = stockAveragePoints(data, stockMovingAverage(data, line.period)),
                        color = line.color,
                        theme = theme,
                    )
                }
            }
        }
        if (showLegend) {
            val selectedIndex = (selection as? ChartSelection.Cartesian)?.itemIndex ?: data.lastIndex
            ChartCanvasRenderer.drawStockLegend(
                ctx = context,
                left = layout.plot.left,
                baseline = layout.plot.top + theme.fontSize,
                lines = attr.movingAverages.lines,
                values = attr.movingAverages.lines.associate { line ->
                    line.period to stockMovingAverage(data, line.period).getOrNull(selectedIndex)
                },
                theme = theme,
            )
        }
        plots.volume?.let { volumePlot ->
            val maxVolume = stockVolumeBounds(data).endInclusive.coerceAtLeast(1f)
            val volumeViewport = ChartViewport(viewport.xMin, viewport.xMax, 0f, maxVolume * 1.08f)
            val volumeLayout = CartesianLayout(volumePlot)
            ChartCanvasRenderer.drawGrid(context, volumeLayout, volumeViewport, theme, attr.grid.show)
            ChartCanvasRenderer.drawAxes(
                ctx = context,
                layout = volumeLayout,
                viewport = volumeViewport,
                theme = theme,
                showX = attr.xAxis.show,
                showY = attr.yAxis.show,
                xTicks = ChartCanvasRenderer.axisTicksFromOhlc(data),
            )
            context.withPlotClip(volumePlot) {
                ChartCanvasRenderer.drawStockVolumes(
                    ctx = context,
                    layout = volumeLayout,
                    viewport = volumeViewport,
                    points = data,
                    candleWidthRatio = attr.candleWidthRatio,
                    theme = theme,
                )
                attr.volumePanel.averageLines.forEach { line ->
                    val values = stockVolumeMovingAverage(data, line.period)
                    val points = data.zip(values).mapNotNull { (point, value) -> value?.let { point.x to it } }
                    ChartCanvasRenderer.drawStockAverageLine(
                        ctx = context,
                        layout = volumeLayout,
                        viewport = volumeViewport,
                        points = points,
                        color = line.color,
                        theme = theme,
                    )
                }
            }
        }
    }

    override fun selectionCrosshair(
        layout: CartesianLayout,
        viewport: ChartViewport,
    ): Pair<Float, Float>? {
        val selected = selection as? ChartSelection.Cartesian ?: return null
        val point = ohlcProvider().getOrNull(selected.itemIndex) ?: return null
        val data = ohlcProvider().toList()
        val showLegend = attr.movingAverages.show && attr.movingAverages.lines.isNotEmpty()
        val contentPlot = if (showLegend) layout.plot.copy(top = layout.plot.top + 18f) else layout.plot
        val plots = splitStockPlots(
            contentPlot,
            shouldShowVolumePanel(data, attr.volumePanel.show),
            attr.volumePanel.heightRatio,
        )
        val scale = CartesianScale(plots.price, viewport)
        return scale.toPixelX(point.x) to scale.toPixelY(point.close)
    }

    override fun onPlotClick(x: Float, y: Float) {
        val data = ohlcProvider().toList()
        if (data.isEmpty() || canvasWidth <= 0f) return
        val showLegend = attr.movingAverages.show && attr.movingAverages.lines.isNotEmpty()
        val layout = com.tencent.kuiklybase.chart.core.cartesian.CartesianLayoutEngine.compute(canvasWidth, canvasHeight)
        val contentPlot = if (showLegend) layout.plot.copy(top = layout.plot.top + 18f) else layout.plot
        val pricePlot = splitStockPlots(
            contentPlot,
            shouldShowVolumePanel(data, attr.volumePanel.show),
            attr.volumePanel.heightRatio,
        ).price
        val scale = CartesianScale(pricePlot, viewport)
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
            text = buildString {
                append(candle.label.ifEmpty { candle.x.toString() })
                append("  O:${candle.open} H:${candle.high} L:${candle.low} C:${candle.close}")
                candle.volume?.let { append("  VOL:$it") }
            },
            localX = x,
            localY = y,
            crossX = scale.toPixelX(candle.x),
            crossY = scale.toPixelY(candle.close),
        )
    }
}

fun ViewContainer<*, *>.StockChart(
    ohlcProvider: () -> ObservableList<OhlcPoint>,
    init: StockChartView.() -> Unit,
) {
    addChild(StockChartView(ohlcProvider), init)
}
