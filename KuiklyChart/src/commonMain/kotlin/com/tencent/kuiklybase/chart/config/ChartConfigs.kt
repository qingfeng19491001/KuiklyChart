package com.tencent.kuiklybase.chart.config

class ChartAxisConfig {
    var show: Boolean = true
}

class ChartGridConfig {
    var show: Boolean = true
}

class ChartLegendConfig {
    var show: Boolean = true
}

class ChartLabelConfig {
    var show: Boolean = false
}

/**
 * DSL 可变主题配置（对齐 ChatUI `ChatThemeOptions`）。
 * 渲染请使用 [resolved] 得到不可变快照；运行时改字段需重建图表节点。
 */
class ChartThemeOptions {
    /** 强调色（雷达选中轴高亮等）。 */
    var primaryColor: Long = 0xFF4F8FFF
    var axisColor: Long = 0xFF999999
    var gridColor: Long = 0xFFE5EAF2
    var textColor: Long = 0xFF333333
    var backgroundColor: Long = 0xFFFFFFFF
    var fontSize: Float = 11f
    var lineWidth: Float = 2f
    /** 上涨 K 线实体色。 */
    var upColor: Long = 0xFFE74C3C
    /** 下跌 K 线实体色。 */
    var downColor: Long = 0xFF27AE60

    fun resolved(): ChartTheme = ChartTheme(
        primaryColor = primaryColor,
        axisColor = axisColor,
        gridColor = gridColor,
        textColor = textColor,
        backgroundColor = backgroundColor,
        fontSize = fontSize,
        lineWidth = lineWidth,
        upColor = upColor,
        downColor = downColor,
    )
}

/** 渲染用不可变主题快照。 */
data class ChartTheme(
    val primaryColor: Long = 0xFF4F8FFF,
    val axisColor: Long = 0xFF999999,
    val gridColor: Long = 0xFFE5EAF2,
    val textColor: Long = 0xFF333333,
    val backgroundColor: Long = 0xFFFFFFFF,
    val fontSize: Float = 11f,
    val lineWidth: Float = 2f,
    val upColor: Long = 0xFFE74C3C,
    val downColor: Long = 0xFF27AE60,
)

/**
 * 笛卡尔图交互。
 *
 * 默认：全量视口 + 点选 / 平移 / 捏合 / 双击复位。
 * 框选放大等进阶手势需显式开启（见 [enableDragSelect]）。
 */
class ChartInteractionConfig {
    var enableTap: Boolean = true
    /** 长按拖动框选；默认关闭，松手后是否缩放见 [brushZoom]。 */
    var enableDragSelect: Boolean = false
    /** 双指捏合缩放；默认开启。 */
    var enableScale: Boolean = true
    /** 单指拖动平移；默认开启。 */
    var enablePan: Boolean = true
    /** 双击复位到初始窗口；默认开启。 */
    var enableReset: Boolean = true
    /** 选中/框选时绘制十字准星。 */
    var enableCrosshair: Boolean = true
    /** 框选结束是否缩放到 X 选区（对齐 Ant brushXFilter）。 */
    var brushZoom: Boolean = true
    /** 锁定 Y 轴，仅沿 X 平移/缩放（时间序列/K 线习惯）。 */
    var lockY: Boolean = false
    /** 视口限制在数据全量范围内，避免拖出空白。 */
    var clampToData: Boolean = true
    /**
     * 初始可见 X 范围占全量的比例（0.15–1）。
     * 默认 1（全貌）；小于 1 时开局即可单指平移浏览局部。
     */
    var initialVisibleRatio: Float = 1f
    /**
     * 初始窗口锚点。默认 CENTER。
     * END 适合股票最新价靠右。
     */
    var initialVisibleAnchor: VisibleAnchor = VisibleAnchor.CENTER
}

enum class VisibleAnchor {
    START,
    CENTER,
    END,
}

class TapInteractionConfig {
    var enableTap: Boolean = true
}
