package com.kuikly.kuiklychart

import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuiklybase.chart.model.ChartSlice
import com.tencent.kuiklybase.chart.pie.DonutChart
import com.tencent.kuiklybase.chart.pie.PieChart

/** 饼图变体画廊：普通饼、环图、半环、玫瑰、旭日、嵌套与爆炸饼。 */
@Page("pie_chart_demo", supportInLocal = true)
internal class PieChartDemoPage : BasePager() {
    private var slices by observableList<ChartSlice>()

    override fun created() {
        super.created()
        slices.addAll(DemoSampleData.pieSlices())
    }

    override fun body(): ViewBuilder {
        val page = this
        return chartDemoShell(page, "PieChart") {
            Scroller {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                    showScrollerIndicator(false)
                    paddingBottom(28f)
                }
                demoVariantSection(1, "普通饼图") {
                    PieChart({ page.slices }) {
                        attr {
                            flex(1f)
                            title = "访问终端占比"
                            showPercentLabel = true
                            legend { show = true; interactive = true }
                            interaction { enableTap = true }
                        }
                    }
                }

                demoVariantSection(2, "环形图") {
                    DonutChart({ page.slices }) {
                        attr {
                            flex(1f)
                            title = "访问终端构成"
                            innerRadiusRatio = 0.58f
                            centerText = "1,070"
                            showPercentLabel = true
                            legend { show = true; interactive = true }
                            interaction { enableTap = true }
                        }
                    }
                }

                demoVariantSection(3, "半环形图") {
                    SpecialDemoChart(SpecialDemoChartKind.HALF_DONUT)
                }

                demoVariantSection(4, "南丁格尔玫瑰图") {
                    SpecialDemoChart(SpecialDemoChartKind.ROSE)
                }

                demoVariantSection(5, "旭日图") {
                    SpecialDemoChart(SpecialDemoChartKind.SUNBURST)
                }

                demoVariantSection(6, "嵌套饼图") {
                    SpecialDemoChart(SpecialDemoChartKind.NESTED_PIE)
                }

                demoVariantSection(7, "爆炸饼图") {
                    PieChart({ page.slices }) {
                        attr {
                            flex(1f)
                            title = "点击扇区查看重点分类"
                            startAngle = -72f
                            showPercentLabel = true
                            legend { show = true; interactive = true }
                            interaction { enableTap = true }
                        }
                    }
                }
            }
        }
    }
}
