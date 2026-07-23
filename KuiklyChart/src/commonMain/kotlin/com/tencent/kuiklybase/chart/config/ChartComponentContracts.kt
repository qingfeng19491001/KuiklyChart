package com.tencent.kuiklybase.chart.config

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.model.ChartSelection
import com.tencent.kuiklybase.chart.model.ChartSlice
import com.tencent.kuiklybase.chart.model.ChartViewport
import com.tencent.kuiklybase.chart.model.RadarDimension

open class CartesianChartAttr : ComposeAttr() {
    var title by observable("")
    val xAxis = ChartAxisConfig()
    val yAxis = ChartAxisConfig()
    val grid = ChartGridConfig()
    val theme = ChartThemeOptions()
    val interaction = ChartInteractionConfig()

    fun xAxis(block: ChartAxisConfig.() -> Unit) = xAxis.apply(block)
    fun yAxis(block: ChartAxisConfig.() -> Unit) = yAxis.apply(block)
    fun grid(block: ChartGridConfig.() -> Unit) = grid.apply(block)
    fun theme(block: ChartThemeOptions.() -> Unit) = theme.apply(block)
    fun interaction(block: ChartInteractionConfig.() -> Unit) = interaction.apply(block)
}

/** 带系列图例的笛卡尔图 Attr（折线 / 柱 / 面积 / 散点）。K 线不继承本类。 */
open class SeriesCartesianChartAttr : CartesianChartAttr() {
    val legend = ChartLegendConfig()

    fun legend(block: ChartLegendConfig.() -> Unit) = legend.apply(block)
}

class LineChartAttr : SeriesCartesianChartAttr() {
    /** 平滑曲线（三次贝塞尔近似）。 */
    var smooth: Boolean = false
    /** 是否绘制数据点标记。 */
    var showPoints: Boolean = true
    /** 数据点半径。 */
    var pointRadius: Float = 4f
}

class BarChartAttr : SeriesCartesianChartAttr() {
    val label = ChartLabelConfig()
    /** 堆叠柱状图模式。 */
    var stacked: Boolean = false
    /** 堆叠时是否显示累计标签。 */
    var showTotalLabel: Boolean = true
    /** 水平条形图。 */
    var horizontal: Boolean = false

    fun label(block: ChartLabelConfig.() -> Unit) = label.apply(block)
}

class AreaChartAttr : SeriesCartesianChartAttr() {
    var gradientFill: Boolean = true
    var smooth: Boolean = false
    var showPoints: Boolean = false
    var pointRadius: Float = 4f
}

class ScatterChartAttr : SeriesCartesianChartAttr() {
    var pointRadius: Float = 5f
}

class StockChartAttr : CartesianChartAttr() {
    var candleWidthRatio: Float = 0.6f
}

class CartesianChartEvent : ComposeEvent() {
    var onPointClick: ((ChartDataPoint, Int, Int) -> Unit)? = null
    var onViewportChange: ((ChartViewport) -> Unit)? = null
    var onSelectionChange: ((ChartSelection?) -> Unit)? = null
    var onDragSelect: ((ClosedFloatingPointRange<Float>) -> Unit)? = null

    fun pointClick(handler: (ChartDataPoint, Int, Int) -> Unit) {
        onPointClick = handler
    }

    fun viewportChange(handler: (ChartViewport) -> Unit) {
        onViewportChange = handler
    }

    fun selectionChange(handler: (ChartSelection?) -> Unit) {
        onSelectionChange = handler
    }

    fun dragSelect(handler: (ClosedFloatingPointRange<Float>) -> Unit) {
        onDragSelect = handler
    }
}

open class PolarChartAttr : ComposeAttr() {
    var title by observable("")
    val legend = ChartLegendConfig()
    val theme = ChartThemeOptions()
    val interaction = TapInteractionConfig()

    fun legend(block: ChartLegendConfig.() -> Unit) = legend.apply(block)
    fun theme(block: ChartThemeOptions.() -> Unit) = theme.apply(block)
    fun interaction(block: TapInteractionConfig.() -> Unit) = interaction.apply(block)
}

class PieChartAttr : PolarChartAttr() {
    var showPercentLabel: Boolean = true
    var innerRadiusRatio: Float = 0f
    var startAngle: Float = -90f
    /** 环形模式：环宽（>0 时按环绘制，优先于 [innerRadiusRatio]）。 */
    var ringWidth: Float = 0f
    /** 环心文案（仅环形有意义）。 */
    var centerText: String = ""
}

class PieChartEvent : ComposeEvent() {
    var onSliceClick: ((ChartSlice, Int) -> Unit)? = null
    var onSelectionChange: ((ChartSelection?) -> Unit)? = null

    fun sliceClick(handler: (ChartSlice, Int) -> Unit) {
        onSliceClick = handler
    }

    fun selectionChange(handler: (ChartSelection?) -> Unit) {
        onSelectionChange = handler
    }
}

class RadarChartAttr : PolarChartAttr() {
    var dimensions by observableList<RadarDimension>()

    fun dimensions(block: RadarDimensionsBuilder.() -> Unit) {
        dimensions.clear()
        dimensions.addAll(RadarDimensionsBuilder().apply(block).items)
    }
}

class RadarDimensionsBuilder {
    internal val items = mutableListOf<RadarDimension>()

    fun dimension(label: String, maxValue: Float) {
        items.add(RadarDimension(label, maxValue))
    }
}

class RadarChartEvent : ComposeEvent() {
    var onRadarClick: ((Int, Int, String?) -> Unit)? = null
    var onSelectionChange: ((ChartSelection?) -> Unit)? = null

    fun radarClick(handler: (Int, Int, String?) -> Unit) {
        onRadarClick = handler
    }

    fun selectionChange(handler: (ChartSelection?) -> Unit) {
        onSelectionChange = handler
    }
}
