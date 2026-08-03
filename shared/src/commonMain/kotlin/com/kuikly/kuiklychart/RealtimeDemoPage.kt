package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.clearTimeout
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.model.ChartSeries
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.line.LineChart

@Page("realtime_demo", supportInLocal = true)
internal class RealtimeDemoPage : BasePager() {
    private var tick = 0
    private var realtimeSeries by observableList<ChartSeries>()
    private var timeoutRef: String? = null

    override fun created() {
        super.created()
        realtimeSeries.add(
            ChartSeries(
                name = "实时数据",
                color = 0xFF4F8FFF,
                points = listOf(ChartDataPoint("", 0f, 50f)),
            ),
        )
        scheduleUpdate()
    }

    private fun scheduleUpdate() {
        // Prefer PagerScope extensions over deprecated top-level timer APIs.
        timeoutRef = this.setTimeout(800) {
            tick += 1
            val lastX = realtimeSeries.firstOrNull()?.points?.lastOrNull()?.x ?: 0f
            val lastY = realtimeSeries.firstOrNull()?.points?.lastOrNull()?.y ?: 50f
            val newX = lastX + 1f
            val newY = (lastY + (tick % 5) * 8f - 16f).coerceIn(20f, 180f)
            val updated = listOf(
                ChartSeries(
                    name = "实时数据",
                    color = 0xFF4F8FFF,
                    points = realtimeSeries.first().points + ChartDataPoint("", newX, newY),
                ).let { s ->
                    if (s.points.size > 12) {
                        s.copy(points = s.points.takeLast(12))
                    } else s
                },
            )
            realtimeSeries[0] = updated[0]
            scheduleUpdate()
        }
    }

    override fun onDestroyPager() {
        timeoutRef?.let { this.clearTimeout(it) }
        timeoutRef = null
        super.onDestroyPager()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoBody(ctx, "Realtime") {
            LineChart({ ctx.realtimeSeries }) {
                attr {
                    flex(1f)
                    title = "实时更新"
                    xAxis { show = true }
                    yAxis { show = true }
                    grid { show = true }
                }
            }
        }
    }
}
