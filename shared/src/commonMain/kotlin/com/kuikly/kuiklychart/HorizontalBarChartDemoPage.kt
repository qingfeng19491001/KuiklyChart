package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observableList
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.horizontalbar.HorizontalBarChart

@Page("horizontal_bar_chart_demo", supportInLocal = true)
internal class HorizontalBarChartDemoPage : BasePager() {
    private var series by observableList<com.tencent.kuiklybase.chart.model.ChartSeries>()

    override fun created() {
        super.created()
        series.addAll(DemoSampleData.horizontalBarSeries())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoBody(ctx, "HorizontalBar Demo") {
            HorizontalBarChart({ ctx.series }) {
                attr {
                    flex(1f)
                    title = "阶段完成度（条形图）"
                    label { show = true }
                    xAxis { show = true }
                    yAxis { show = true }
                    grid { show = true }
                    legend { show = true }
                }
            }
        }
    }
}
