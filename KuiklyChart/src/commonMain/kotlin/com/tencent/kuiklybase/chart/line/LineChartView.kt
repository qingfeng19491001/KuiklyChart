package com.tencent.kuiklybase.chart.line

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.ContextApi
import com.tencent.kuiklybase.chart.config.LineChartAttr
import com.tencent.kuiklybase.chart.core.ChartCanvasRenderer
import com.tencent.kuiklybase.chart.core.cartesian.CartesianChartView
import com.tencent.kuiklybase.chart.core.cartesian.CartesianLayout
import com.tencent.kuiklybase.chart.core.cartesian.cartesianChartView
import com.tencent.kuiklybase.chart.model.ChartSelection
import com.tencent.kuiklybase.chart.model.ChartSeries
import com.tencent.kuiklybase.chart.model.ChartViewport

class LineChartView(seriesProvider: () -> ObservableList<ChartSeries>) :
    CartesianChartView<LineChartAttr>(seriesProvider) {

    override fun createAttr() = LineChartAttr()

    override fun drawChart(
        context: ContextApi,
        width: Float,
        height: Float,
        layout: CartesianLayout,
        viewport: ChartViewport,
        selection: ChartSelection?,
        series: List<ChartSeries>,
    ) {
        val theme = config.theme.resolved()
        ChartCanvasRenderer.drawGrid(context, layout, viewport, theme, config.grid.show)
        drawChartAxes(context, layout, viewport, series)
        ChartCanvasRenderer.drawLineSeries(
            context, layout, viewport, series, theme, selection,
            showPoints = config.showPoints,
            pointRadius = config.pointRadius,
            smooth = config.smooth,
        )
    }
}

fun ViewContainer<*, *>.LineChart(
    seriesProvider: () -> ObservableList<ChartSeries>,
    init: LineChartView.() -> Unit,
) {
    cartesianChartView(LineChartView(seriesProvider), init)
}
