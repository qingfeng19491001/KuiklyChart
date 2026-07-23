package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observableList
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.scatter.ScatterChart

@Page("scatter_chart_demo", supportInLocal = true)
internal class ScatterChartDemoPage : BasePager() {
    private var scatterSeries by observableList<com.tencent.kuiklybase.chart.model.ChartSeries>()

    override fun created() {
        super.created()
        scatterSeries.addAll(DemoSampleData.scatterSeries())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoBody(ctx, "ScatterChart Demo") {
            ScatterChart({ ctx.scatterSeries }) {
                attr {
                    flex(1f)
                    title = "散点分布"
                    xAxis { show = true }
                    yAxis { show = true }
                    grid { show = true }
                    pointRadius = 6f
                }
            }
        }
    }
}
