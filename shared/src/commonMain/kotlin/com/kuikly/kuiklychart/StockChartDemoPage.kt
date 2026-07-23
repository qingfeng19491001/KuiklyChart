package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observableList
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.model.OhlcPoint
import com.tencent.kuiklybase.chart.stock.StockChart

@Page("stock_chart_demo", supportInLocal = true)
internal class StockChartDemoPage : BasePager() {
    private var candles by observableList<OhlcPoint>()

    override fun created() {
        super.created()
        candles.addAll(DemoSampleData.stockSeries())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoBody(ctx, "StockChart Demo") {
            StockChart({ ctx.candles }) {
                attr {
                    flex(1f)
                    title = "K 线（股票图）"
                    xAxis { show = true }
                    yAxis { show = true }
                    grid { show = true }
                    interaction {
                        enableTap = true
                        enableScale = true
                        enablePan = true
                        enableReset = true
                    }
                }
            }
        }
    }
}
