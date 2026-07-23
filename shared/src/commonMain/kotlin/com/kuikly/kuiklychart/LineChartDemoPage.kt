package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observableList
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.line.LineChart

@Page("line_chart_demo", supportInLocal = true)
internal class LineChartDemoPage : BasePager() {
    private var lineSeries by observableList<com.tencent.kuiklybase.chart.model.ChartSeries>()

    override fun created() {
        super.created()
        lineSeries.addAll(DemoSampleData.lineSeries())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoBody(ctx, "LineChart Demo") {
            LineChart({ ctx.lineSeries }) {
                attr {
                    flex(1f)
                    title = "销售趋势"
                    smooth = true
                    showPoints = true
                    pointRadius = 4f
                    xAxis { show = true }
                    yAxis { show = true }
                    grid { show = true }
                    legend { show = true }
                    interaction { lockY = true }
                }
            }
        }
    }
}
