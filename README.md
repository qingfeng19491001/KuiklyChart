# KuiklyChart

基于 [KuiklyUI](https://github.com/Tencent-TDS/KuiklyUI) 跨端框架构建的图表 UI 组件库，支持 Android、iOS、鸿蒙多端运行。
![img_1.png](img_1.png)

## 接入

在仓库配置中加入 Kuikly Maven 仓库：

```kotlin
repositories {
    maven("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
}
```

标准平台（Kotlin 2.1.21）：

```kotlin
implementation("com.tencent.kuiklybase:KuiklyChart:1.2.1-2.1.21")
```

鸿蒙（Kotlin 2.0.21 KBA）：

```kotlin
implementation("com.tencent.kuiklybase:KuiklyChart:1.2.1-2.0.21-KBA-010")
```

本地开发可执行：

```bash
./publish-maven.sh -l true
```

## 核心 API

### 图表

- `LineChart`：折线趋势（默认直线折线 + 点标记；可通过 `smooth = true` 开启平滑曲线；面积填充请用 `AreaChart`）。
- `BarChart`：柱状图；`stacked` / `horizontal` 变体；支持单柱颜色与数值标签。
- `StackedBarChart` / `HorizontalBarChart`：兼容入口，等价于上述 `BarChart` 变体。
- `AreaChart`：面积图（渐变填充，可平滑）。
- `ScatterChart`：散点图。
- `PieChart`：饼图；`ringWidth` / `centerText` 可画环形。
- `DonutChart`：环形图兼容入口（复用 `PieChart`）。
- `RadarChart`：雷达图。
- `StockChart`：K 线（与系列图共用笛卡尔交互壳；无图例配置）。

### 数据模型

```kotlin
data class ChartDataPoint(val label: String, val x: Float, val y: Float, val color: Long? = null)
data class ChartSeries(val name: String, val points: List<ChartDataPoint>, val color: Long)
data class ChartSlice(val label: String, val value: Float, val color: Long)
data class RadarDimension(val label: String, val maxValue: Float)
data class RadarSeries(val name: String, val values: List<Float>, val color: Long)
data class OhlcPoint(val label: String, val x: Float, val open: Float, val high: Float, val low: Float, val close: Float)
```

选中态保持数据自身颜色（外扩 / 白描边），不再统一刷成主题蓝。

### 事件

直角坐标图支持：

```kotlin
event {
    pointClick { point, seriesIndex, pointIndex -> }
    dragSelect { range -> }
    viewportChange { viewport -> }
    selectionChange { selection -> }
}
```

饼图和环形图支持 `sliceClick`、`selectionChange`；雷达图支持 `radarClick`、`selectionChange`。

交互开关属于组件初始化配置。运行时如需切换交互策略，请重建图表节点。
事件始终注册，开关在回调内判断。

默认交互（基础）：

- **单击数据点**：选中、高亮并显示 Tooltip
- **单击空白区域**：取消选中并关闭 Tooltip
- 多系列 `LineChart` 默认聚合同一 X 坐标的数据

平移、缩放、双击复位、框选和十字准星需显式开启：

```kotlin
interaction {
    enablePan = true
    enableScale = true
    enableReset = true
    enableCrosshair = true
    enableDragSelect = true  // 长按框选
    brushZoom = true
    lockY = true
    initialVisibleRatio = 0.55f
}
```

折线图 Tooltip 支持同 X 聚合与格式化 DSL：

```kotlin
tooltip {
    sharedByX = true
    formatter { context ->
        buildString {
            append(context.label)
            context.items.forEach { item ->
                append("\n${item.seriesName}: ${item.point.y}")
            }
        }
    }
}
```

交互示例已合并到各图表变体页，避免独立手势 Demo 与真实使用场景脱节。

## 主题

`theme { }` 写入 `ChartThemeOptions`；绘制时取 `resolved()` 不可变快照。
主题与交互同属初始化配置，运行时修改请重建图表节点。

```kotlin
attr {
    theme {
        primaryColor = 0xFF6C5CE7  // 强调色（如雷达选中轴）
        axisColor = 0xFF666666
        gridColor = 0xFFE8E0F5
        textColor = 0xFF2D3436
        backgroundColor = 0xFFF8F6FF
        fontSize = 12f
        lineWidth = 2.5f
        upColor = 0xFFE74C3C
        downColor = 0xFF27AE60
    }
}
```

## 使用示例

```kotlin
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuiklybase.chart.line.LineChart
import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.model.ChartSeries

private var series by observableList<ChartSeries>()

override fun created() {
    super.created()
    series.add(
        ChartSeries(
            name = "销售额",
            color = 0xFF4F8FFF,
            points = listOf(
                ChartDataPoint("一月", 0f, 42f),
                ChartDataPoint("二月", 1f, 68f),
            ),
        ),
    )
}

override fun body(): ViewBuilder {
    val ctx = this
    return {
        LineChart({ ctx.series }) {
            attr {
                flex(1f)
                title = "销售趋势"
                smooth = false
                showPoints = true
                xAxis { show = true }
                yAxis { show = true }
                grid { show = true }
                legend { show = true }
                interaction {
                    enableTap = true
                    enableScale = true
                    enablePan = true
                    enableReset = true
                    enableCrosshair = true
                }
                tooltip { sharedByX = true }
            }
            event {
                pointClick { point, seriesIndex, pointIndex ->
                    // 处理数据点点击
                }
                viewportChange { viewport ->
                    // 处理视口变化
                }
            }
        }
    }
}
```

图表数据必须由 `observableList` 持有，并通过 `() -> ObservableList<T>` 传给组件。

运行 `androidApp`、`iosApp` 或 `ohosApp` 后进入默认 `router` 页面。Demo 覆盖折线 / 柱状 / 堆叠 / 条形 / 面积 / 饼 / 环 / 散点 / 雷达 / 股票等图表，以及视口手势、主题、实时数据和最小 API 用法。

## License

[MIT](LICENSE)
