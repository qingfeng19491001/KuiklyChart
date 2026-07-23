package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observableList
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.pie.DonutChart

@Page("donut_chart_demo", supportInLocal = true)
internal class DonutChartDemoPage : BasePager() {
    private var slices by observableList<com.tencent.kuiklybase.chart.model.ChartSlice>()

    override fun created() {
        super.created()
        slices.addAll(DemoSampleData.pieSlices())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoBody(ctx, "DonutChart Demo") {
            DonutChart({ ctx.slices }) {
                attr {
                    flex(1f)
                    centerText = "总计"
                    ringWidth = 36f
                    legend { show = true }
                    interaction { enableTap = true }
                }
            }
        }
    }
}
