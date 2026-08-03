package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Scroller
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.area.AreaChart
import com.tencent.kuiklybase.chart.config.AreaMode

@Page("area_chart_demo", supportInLocal = true)
internal class AreaChartDemoPage : BasePager() {
    private var areaSeries by observableList<com.tencent.kuiklybase.chart.model.ChartSeries>()

    override fun created() {
        super.created()
        areaSeries.addAll(DemoSampleData.lineMultiSeries())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoShell(ctx, "AreaChart") {
            Scroller {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                    showScrollerIndicator(false)
                    paddingBottom(24f)
                }
                val modes = listOf(
                    AreaMode.BASIC to "基础面积图", AreaMode.STACKED to "堆叠面积图",
                    AreaMode.PERCENT_STACKED to "百分比堆叠面积图", AreaMode.SPLINE to "平滑面积图",
                    AreaMode.RANGE to "范围面积图", AreaMode.STEP to "阶梯面积图",
                    AreaMode.STREAM to "河流图", AreaMode.OVERLAPPED to "重叠面积图",
                    AreaMode.POLAR to "极坐标面积图", AreaMode.RIDGELINE to "山脉图",
                )
                modes.forEachIndexed { index, item ->
                    this@AreaChartDemoPage.run {
                        this@Scroller.demoVariantSection(index + 1, item.second) {
                            AreaChart({ ctx.areaSeries }) {
                                attr {
                                    flex(1f)
                                    title = item.second
                                    mode = item.first
                                    smooth = item.first == AreaMode.SPLINE
                                    showPoints = item.first == AreaMode.BASIC || item.first == AreaMode.SPLINE
                                    gradientFill = true
                                    xAxis { show = true }
                                    yAxis { show = true }
                                    grid { show = true }
                                    legend { show = true; interactive = true }
                                    interaction { enableTap = true; enableCrosshair = true; lockY = true }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
