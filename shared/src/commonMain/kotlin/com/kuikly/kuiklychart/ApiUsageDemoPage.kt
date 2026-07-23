package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observableList
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.line.LineChart

@Page("api_usage_demo", supportInLocal = true)
internal class ApiUsageDemoPage : BasePager() {
    private var lineSeries by observableList<com.tencent.kuiklybase.chart.model.ChartSeries>()

    override fun created() {
        super.created()
        lineSeries.addAll(DemoSampleData.lineSeries().take(1))
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoBody(ctx, "API Usage Demo") {
            LineChart({ ctx.lineSeries }) {
                attr {
                    flex(1f)
                    title = "销售趋势"
                    xAxis { show = true }
                    yAxis { show = true }
                    grid { show = true }
                }
                event {
                    pointClick { _, _, _ -> }
                }
            }
        }
    }
}
