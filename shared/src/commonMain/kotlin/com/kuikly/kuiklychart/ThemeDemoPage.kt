package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observableList
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.bar.BarChart

@Page("theme_demo", supportInLocal = true)
internal class ThemeDemoPage : BasePager() {
    private var barSeries by observableList<com.tencent.kuiklybase.chart.model.ChartSeries>()

    override fun created() {
        super.created()
        barSeries.addAll(DemoSampleData.barSeries())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoBody(ctx, "Theme Demo") {
            BarChart({ ctx.barSeries }) {
                attr {
                    flex(1f)
                    title = "主题定制"
                    xAxis { show = true }
                    yAxis { show = true }
                    grid { show = true }
                    legend { show = true }
                    label { show = true }
                    theme {
                        primaryColor = 0xFF6C5CE7
                        axisColor = 0xFF666666
                        gridColor = 0xFFE8E0F5
                        textColor = 0xFF2D3436
                        backgroundColor = 0xFFF8F6FF
                        fontSize = 12f
                        lineWidth = 2.5f
                    }
                    interaction { enableTap = true }
                }
            }
        }
    }
}
