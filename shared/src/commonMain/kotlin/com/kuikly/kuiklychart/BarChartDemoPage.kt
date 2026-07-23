package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observableList
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.bar.BarChart

@Page("bar_chart_demo", supportInLocal = true)
internal class BarChartDemoPage : BasePager() {
    private var barSeries by observableList<com.tencent.kuiklybase.chart.model.ChartSeries>()

    override fun created() {
        super.created()
        barSeries.addAll(DemoSampleData.barSeries())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoBody(ctx, "BarChart Demo") {
            BarChart({ ctx.barSeries }) {
                attr {
                    flex(1f)
                    title = "周销售对比"
                    xAxis { show = true }
                    yAxis { show = true }
                    grid { show = true }
                    legend { show = true }
                    label { show = true }
                }
            }
        }
    }
}
