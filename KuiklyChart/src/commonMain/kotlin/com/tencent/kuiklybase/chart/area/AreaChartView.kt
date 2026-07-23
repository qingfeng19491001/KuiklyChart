package com.tencent.kuiklybase.chart.area

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.ContextApi
import com.tencent.kuiklybase.chart.config.AreaChartAttr
import com.tencent.kuiklybase.chart.core.ChartCanvasRenderer
import com.tencent.kuiklybase.chart.core.cartesian.CartesianChartView
import com.tencent.kuiklybase.chart.core.cartesian.CartesianLayout
import com.tencent.kuiklybase.chart.core.cartesian.cartesianChartView
import com.tencent.kuiklybase.chart.model.ChartSelection
import com.tencent.kuiklybase.chart.model.ChartSeries
import com.tencent.kuiklybase.chart.model.ChartViewport

class AreaChartView(seriesProvider: () -> ObservableList<ChartSeries>) :
    CartesianChartView<AreaChartAttr>(seriesProvider) {

    override fun createAttr() = AreaChartAttr()

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
        ChartCanvasRenderer.drawAreaSeries(
            context, layout, viewport, series, theme,
            gradientFill = attr.gradientFill,
            selection = selection,
            smooth = attr.smooth,
            showPoints = attr.showPoints,
            pointRadius = attr.pointRadius,
        )
    }
}

fun ViewContainer<*, *>.AreaChart(
    seriesProvider: () -> ObservableList<ChartSeries>,
    init: AreaChartView.() -> Unit,
) {
    cartesianChartView(AreaChartView(seriesProvider), init)
}
