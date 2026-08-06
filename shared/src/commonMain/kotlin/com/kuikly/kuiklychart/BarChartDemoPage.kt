package com.kuikly.kuiklychart

import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuiklybase.chart.advanced.BulletChart
import com.tencent.kuiklybase.chart.advanced.BulletChartItem
import com.tencent.kuiklybase.chart.advanced.DualAxisBarChart
import com.tencent.kuiklybase.chart.advanced.DualAxisPoint
import com.tencent.kuiklybase.chart.advanced.HistogramBin
import com.tencent.kuiklybase.chart.advanced.HistogramChart
import com.tencent.kuiklybase.chart.advanced.WaterfallChart
import com.tencent.kuiklybase.chart.advanced.WaterfallPoint
import com.tencent.kuiklybase.chart.bar.BarChart
import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.model.ChartSeries

/** 柱状图变体画廊：与折线图 Demo 一致，使用纵向卡片逐项展示。 */
@Page("bar_chart_demo", supportInLocal = true)
internal class BarChartDemoPage : BasePager() {
    private var basic by observableList<ChartSeries>()
    private var grouped by observableList<ChartSeries>()
    private var stacked by observableList<ChartSeries>()
    private var percentStacked by observableList<ChartSeries>()
    private var horizontal by observableList<ChartSeries>()
    private var dualAxis by observableList<DualAxisPoint>()
    private var waterfall by observableList<WaterfallPoint>()
    private var histogram by observableList<HistogramBin>()
    private var bullets by observableList<BulletChartItem>()

    override fun created() {
        super.created()
        val bars = DemoSampleData.barSeries()
        basic.add(
            bars.first().copy(
                points = bars.first().points.mapIndexed { index, point ->
                    point.copy(color = listOf(0xFF1677FF, 0xFF52C41A, 0xFFFFA940, 0xFF722ED1, 0xFF13C2C2)[index])
                },
            ),
        )
        grouped.addAll(bars)
        stacked.addAll(DemoSampleData.stackedBarSeries())
        percentStacked.addAll(normalizeToPercent(DemoSampleData.stackedBarSeries()))
        horizontal.addAll(DemoSampleData.horizontalBarSeries())
        dualAxis.addAll(AdvancedDemoData.dualAxis())
        waterfall.addAll(AdvancedDemoData.waterfall())
        histogram.addAll(AdvancedDemoData.histogram())
        bullets.addAll(AdvancedDemoData.bullets())
    }

    override fun body(): ViewBuilder {
        val page = this
        return chartDemoShell(page, "BarChart") {
            Scroller {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                    showScrollerIndicator(false)
                    paddingBottom(28f)
                }
                demoVariantSection(1, "基础柱状图") {
                    BarChart({ page.basic }) {
                        attr {
                            flex(1f)
                            title = "每日订单量"
                            label { show = true }
                            legend { show = false }
                            interaction { enableTap = true }
                        }
                    }
                }

                demoVariantSection(2, "分组柱状图") {
                    BarChart({ page.grouped }) {
                        attr {
                            flex(1f)
                            title = "渠道销售对比"
                            label { show = false }
                            legend { show = true; interactive = true }
                            interaction { enableTap = true }
                        }
                    }
                }

                demoVariantSection(3, "堆叠柱状图") {
                    BarChart({ page.stacked }) {
                        attr {
                            flex(1f)
                            title = "季度流量构成"
                            stacked = true
                            showTotalLabel = true
                            legend { show = true; interactive = true }
                            interaction { enableTap = true }
                        }
                    }
                }

                demoVariantSection(4, "百分比堆叠柱状图") {
                    BarChart({ page.percentStacked }) {
                        attr {
                            flex(1f)
                            title = "渠道占比"
                            stacked = true
                            showTotalLabel = false
                            legend { show = true; interactive = true }
                            interaction { enableTap = true }
                        }
                    }
                }

                demoVariantSection(5, "横向柱状图") {
                    BarChart({ page.horizontal }) {
                        attr {
                            flex(1f)
                            title = "项目阶段完成度"
                            horizontal = true
                            label { show = true }
                            legend { show = false }
                            interaction { enableTap = true }
                        }
                    }
                }

                demoVariantSection(6, "双轴柱状图") {
                    DualAxisBarChart({ page.dualAxis }) { attr { flex(1f) } }
                }

                demoVariantSection(7, "瀑布图") {
                    WaterfallChart({ page.waterfall }) { attr { flex(1f) } }
                }

                demoVariantSection(8, "直方图") {
                    HistogramChart({ page.histogram }) { attr { flex(1f) } }
                }

                demoVariantSection(9, "子弹图") {
                    BulletChart({ page.bullets }) { attr { flex(1f) } }
                }
            }
        }
    }

    private fun normalizeToPercent(series: List<ChartSeries>): List<ChartSeries> {
        val categoryCount = series.maxOfOrNull { it.points.size } ?: return emptyList()
        val totals = (0 until categoryCount).map { index ->
            series.sumOf { (it.points.getOrNull(index)?.y ?: 0f).toDouble() }.toFloat().coerceAtLeast(1f)
        }
        return series.map { item ->
            item.copy(
                points = item.points.mapIndexed { index, point ->
                    ChartDataPoint(point.label, point.x, point.y / totals[index] * 100f, point.color)
                },
            )
        }
    }
}
