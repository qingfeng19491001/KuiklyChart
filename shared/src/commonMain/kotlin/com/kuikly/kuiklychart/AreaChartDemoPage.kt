package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observableList
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.area.AreaChart

@Page("area_chart_demo", supportInLocal = true)
internal class AreaChartDemoPage : BasePager() {
    private var areaSeries by observableList<com.tencent.kuiklybase.chart.model.ChartSeries>()

    override fun created() {
        super.created()
        areaSeries.addAll(DemoSampleData.lineSeries())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoBody(ctx, "AreaChart Demo") {
            AreaChart({ ctx.areaSeries }) {
                attr {
                    flex(1f)
                    title = "面积图"
                    smooth = true
                    showPoints = true
                    gradientFill = true
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
