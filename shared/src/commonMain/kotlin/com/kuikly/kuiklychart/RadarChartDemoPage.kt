package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observableList
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.radar.RadarChart

@Page("radar_chart_demo", supportInLocal = true)
internal class RadarChartDemoPage : BasePager() {
    private var radarSeries by observableList<com.tencent.kuiklybase.chart.model.RadarSeries>()

    override fun created() {
        super.created()
        radarSeries.addAll(DemoSampleData.radarSeries())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoBody(ctx, "RadarChart Demo") {
            RadarChart({ ctx.radarSeries }) {
                attr {
                    flex(1f)
                    title = "能力评估（点选高亮 + Tooltip）"
                    legend { show = true }
                    dimensions {
                        dimension("销售", 100f)
                        dimension("管理", 100f)
                        dimension("技术", 100f)
                        dimension("客服", 100f)
                        dimension("研发", 100f)
                        dimension("市场", 100f)
                    }
                    interaction { enableTap = true }
                }
            }
        }
    }
}
