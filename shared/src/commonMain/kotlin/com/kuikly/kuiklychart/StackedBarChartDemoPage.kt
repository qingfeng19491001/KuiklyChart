package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observableList
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.bar.BarChart

@Page("stacked_bar_chart_demo", supportInLocal = true)
internal class StackedBarChartDemoPage : BasePager() {
    private var stackedSeries by observableList<com.tencent.kuiklybase.chart.model.ChartSeries>()

    override fun created() {
        super.created()
        stackedSeries.addAll(DemoSampleData.stackedBarSeries())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoBody(ctx, "Stacked Bar Demo") {
            BarChart({ ctx.stackedSeries }) {
                attr {
                    flex(1f)
                    title = "渠道构成（堆叠）"
                    stacked = true
                    showTotalLabel = true
                    xAxis { show = true }
                    yAxis { show = true }
                    grid { show = true }
                    legend { show = true }
                }
            }
        }
    }
}
