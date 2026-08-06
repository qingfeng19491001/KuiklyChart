package com.tencent.kuiklybase.chart.advanced

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuiklybase.chart.config.ChartThemeOptions

enum class AdvancedChartKind {
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

data class DualAxisPoint(
    val label: String,
    val barValue: Float,
    val lineValue: Float,
    val barColor: Long? = null,
)

data class WaterfallPoint(
    val label: String,
    val value: Float,
    val isTotal: Boolean = false,
    val color: Long? = null,
)

data class HistogramBin(
    val label: String,
    val value: Float,
    val color: Long? = null,
)

data class BulletChartItem(
    val label: String,
    val actual: Float,
    val target: Float,
    val ranges: List<Float> = listOf(0.55f, 0.8f, 1f),
    val color: Long? = null,
)

data class SunburstNode(
    val label: String,
    val value: Float,
    val color: Long,
    val depth: Int,
)

enum class NestedPieRing { INNER, OUTER }

data class NestedPieSlice(
    val label: String,
    val value: Float,
    val color: Long,
    val ring: NestedPieRing,
)

data class PointFigureColumn(
    val label: String,
    val count: Int,
    val rising: Boolean,
)

data class AdvancedChartSelection(
    val kind: AdvancedChartKind,
    val index: Int,
    val label: String,
    val value: Float? = null,
)

class AdvancedChartInteractionConfig {
    var enableTap: Boolean = true
    var enablePan: Boolean = false
    var enableScale: Boolean = false
    var enableReset: Boolean = false
    var initialVisibleRatio: Float = 1f
}

class AdvancedChartAttr : ComposeAttr() {
    val theme = ChartThemeOptions()
    val interaction = AdvancedChartInteractionConfig()

    fun theme(block: ChartThemeOptions.() -> Unit) = theme.apply(block)
    fun interaction(block: AdvancedChartInteractionConfig.() -> Unit) = interaction.apply(block)
}

class AdvancedChartEvent : ComposeEvent() {
    var onItemClick: ((AdvancedChartSelection) -> Unit)? = null
    var onViewportChange: ((Float, Float) -> Unit)? = null

    fun itemClick(handler: (AdvancedChartSelection) -> Unit) {
        onItemClick = handler
    }

    fun viewportChange(handler: (Float, Float) -> Unit) {
        onViewportChange = handler
    }
}
