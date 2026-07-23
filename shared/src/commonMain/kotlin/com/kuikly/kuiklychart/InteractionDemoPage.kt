package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.bar.BarChart
import com.tencent.kuiklybase.chart.config.VisibleAnchor
import com.tencent.kuiklybase.chart.line.LineChart

/**
 * 进阶手势对照（需显式开启框选 / 局部视口）：拖动平移、捏合缩放、长按框选放大、双击复位。
 */
@Page("interaction_demo", supportInLocal = true)
internal class InteractionDemoPage : BasePager() {
    private var lineSeries by observableList<com.tencent.kuiklybase.chart.model.ChartSeries>()
    private var denseBarSeries by observableList<com.tencent.kuiklybase.chart.model.ChartSeries>()

    override fun created() {
        super.created()
        lineSeries.addAll(DemoSampleData.lineSeries())
        denseBarSeries.addAll(DemoSampleData.denseBarSeries())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoShell(ctx, "视口手势综合 Demo") {
            View {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                    margin(12f)
                }
                Text {
                    attr {
                        text("进阶手势：单指平移 · 双指捏合 · 长按框选放大 · 双击复位")
                        fontSize(12f)
                        color(Color(0xFF666666))
                        marginBottom(8f)
                    }
                }
                View {
                    attr {
                        flex(1f)
                        marginBottom(8f)
                    }
                    LineChart({ ctx.lineSeries }) {
                        attr {
                            flex(1f)
                            title = "Line · lockY"
                            smooth = true
                            showPoints = true
                            xAxis { show = true }
                            yAxis { show = true }
                            grid { show = true }
                            legend { show = true }
                            interaction {
                                lockY = true
                                enableDragSelect = true
                                brushZoom = true
                                initialVisibleRatio = 0.55f
                                initialVisibleAnchor = VisibleAnchor.CENTER
                            }
                        }
                    }
                }
                View {
                    attr { flex(1f) }
                    BarChart({ ctx.denseBarSeries }) {
                        attr {
                            flex(1f)
                            title = "Bar · 24 类目"
                            xAxis { show = true }
                            yAxis { show = true }
                            grid { show = true }
                            interaction {
                                lockY = true
                                enableDragSelect = true
                                brushZoom = true
                                initialVisibleRatio = 0.55f
                            }
                        }
                    }
                }
            }
        }
    }
}
