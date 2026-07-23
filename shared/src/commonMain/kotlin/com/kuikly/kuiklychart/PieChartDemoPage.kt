package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observableList
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.pie.PieChart

@Page("pie_chart_demo", supportInLocal = true)
internal class PieChartDemoPage : BasePager() {
    private var slices by observableList<com.tencent.kuiklybase.chart.model.ChartSlice>()

    override fun created() {
        super.created()
        slices.addAll(DemoSampleData.pieSlices())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoBody(ctx, "PieChart Demo") {
            PieChart({ ctx.slices }) {
                attr {
                    flex(1f)
                    legend { show = true }
                    interaction { enableTap = true }
                }
            }
        }
    }
}
