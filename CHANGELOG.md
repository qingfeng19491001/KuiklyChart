# Changelog

## Unreleased

- `StockChart` 新增浅色/深色券商主题预设、MA 均线、可选成交量副图与成交量均线，并在 Tooltip 中展示 OHLCV。
- OHLC、蜡烛柱、股票面积、股票折线、Renko、Kagi 和点数图统一支持点击/长按查看、准星、平移、缩放和双击复位。
- 股票图 Demo 首卡升级为“专业 K 线图”，提供日/周/月常用 Tab 和更多周期下拉；周期与主题均由业务调用层持有。

## 1.3.0

- 新增双轴柱状图、瀑布图、直方图、子弹图、半环图、玫瑰图、旭日图、嵌套饼图、OHLC、股票面积图、股票折线图、Renko、Kagi 和点数图公共组件。
- 14 类高级图表统一使用类型安全数据模型、响应式数据 provider、主题配置、点击选择事件和共享渲染内核。
- 修复堆叠面积图、百分比面积图和流图的 Y 轴视口计算，避免堆叠值超出绘图区。

## 1.2.1

- 折线图支持点击空白取消选中、多系列同 X 聚合 Tooltip 与 `tooltip { formatter { ... } }` 格式化 DSL。
- 折线图变体统一开启点按、平移、缩放、双击复位、长按框选与十字准星交互。
- 类别轴刻度优先展示 `ChartDataPoint.label` / `OhlcPoint.label`（柱状 / 条形 / 折线等）。
- 交互事件始终注册，开关在回调内判断。
- 饼图百分比标签归入 `PieChartAttr.showPercentLabel`。
- 主题改为 `ChartThemeOptions` + `resolved()` 不可变快照；运行时改主题需重建节点。
- `StockChart` 不再暴露系列图例配置。

## 1.2.0

- **折线 / 面积职责拆分**：`LineChart` 仅绘制折线与点；面积填充统一走 `AreaChart`（移除 `areaFill`）。
- **单交互壳**：`StockChart` 与系列图共用 `CartesianInteractiveView`（手势 / Tooltip / 框选叠加）。
- **柱状公开面收敛**：`StackedBarChart` / `HorizontalBarChart` 改为 `BarChart` 薄封装。
- **饼 / 环收敛**：`DonutChart` 复用 `PieChart`（`ringWidth` / `centerText`）。
- **手势默认回归组件风格**：默认全量视口；框选放大改为显式开启；移除库内操作提示文案。

## 1.1.0

- LineChart：平滑曲线、点标记配置；选中态保持系列色。
- BarChart：单柱颜色、`stacked` / `horizontal` 变体。
- 新增 `HorizontalBarChart`（条形图）、`StockChart`（K 线）。
- 修复饼/环/散点/柱/线/雷达选中统一变蓝问题。
- 雷达图：轴高亮、系列强调、Tooltip。
- 手势：单指平移、双指捏合、长按框选放大、双击复位、十字准星。

## 1.0.0

- 提供 Line、Bar、Area、Scatter、StackedBar、Pie、Donut、Radar 八类图表。
- 支持主题、响应式数据、选中、拖选、缩放、平移和视口重置。
- 提供 Android、iOS、鸿蒙宿主 Demo。
- 发布坐标：`com.tencent.kuiklybase:KuiklyChart:1.0.0-{kotlin}`。
