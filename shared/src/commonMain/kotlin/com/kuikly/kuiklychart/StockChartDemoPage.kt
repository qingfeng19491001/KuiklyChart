package com.kuikly.kuiklychart

import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chart.advanced.KagiChart
import com.tencent.kuiklybase.chart.advanced.OhlcChart
import com.tencent.kuiklybase.chart.advanced.PointFigureChart
import com.tencent.kuiklybase.chart.advanced.PointFigureColumn
import com.tencent.kuiklybase.chart.advanced.RenkoChart
import com.tencent.kuiklybase.chart.advanced.StockAreaChart
import com.tencent.kuiklybase.chart.advanced.StockLineChart
import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.model.OhlcPoint
import com.tencent.kuiklybase.chart.stock.StockChart

internal enum class StockPeriod(val label: String) {
    MIN_1("1分"),
    MIN_5("5分"),
    MIN_15("15分"),
    MIN_30("30分"),
    MIN_60("60分"),
    MIN_120("120分"),
    DAY("日K"),
    WEEK("周K"),
    MONTH("月K"),
    QUARTER("季K"),
    YEAR("年K"),
}

internal fun stockPrimaryPeriods(): List<StockPeriod> =
    listOf(StockPeriod.DAY, StockPeriod.WEEK, StockPeriod.MONTH)

internal fun stockMorePeriods(): List<StockPeriod> = listOf(
    StockPeriod.MIN_1,
    StockPeriod.MIN_5,
    StockPeriod.MIN_15,
    StockPeriod.MIN_30,
    StockPeriod.MIN_60,
    StockPeriod.MIN_120,
    StockPeriod.QUARTER,
    StockPeriod.YEAR,
)

/** 股票图变体画廊：专业 K 线额外展示周期、均线和成交量能力。 */
@Page("stock_chart_demo", supportInLocal = true)
internal class StockChartDemoPage : BasePager() {
    private var candles by observableList<OhlcPoint>()
    private var stockPoints by observableList<ChartDataPoint>()
    private var renko by observableList<ChartDataPoint>()
    private var kagi by observableList<ChartDataPoint>()
    private var pointFigure by observableList<PointFigureColumn>()
    private var selectedPeriod by observable(StockPeriod.DAY)
    private var showMorePeriods by observable(false)

    override fun created() {
        super.created()
        candles.addAll(DemoSampleData.stockSeries(selectedPeriod))
        stockPoints.addAll(AdvancedDemoData.stockPoints())
        renko.addAll(AdvancedDemoData.renko())
        kagi.addAll(AdvancedDemoData.kagi())
        pointFigure.addAll(AdvancedDemoData.pointFigure())
    }

    private fun selectPeriod(period: StockPeriod) {
        selectedPeriod = period
        showMorePeriods = false
        candles.clear()
        candles.addAll(DemoSampleData.stockSeries(period))
    }

    override fun body(): ViewBuilder {
        val page = this
        return chartDemoShell(page, "StockChart") {
            Scroller {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                    showScrollerIndicator(false)
                    paddingBottom(28f)
                }
                stockDemoVariantSection(
                    1,
                    "专业 K 线图",
                    "多周期 · MA5/10/20/30 · 成交量 · 长按准星",
                    stockVariantCardHeight(professional = true),
                ) {
                    View {
                        attr { flex(1f) }
                        View {
                            attr {
                                height(34f)
                                flexDirectionRow()
                                alignItemsCenter()
                            }
                            View {
                                attr {
                                    flex(1f)
                                    flexDirectionRow()
                                }
                                stockPrimaryPeriods().forEach { period ->
                                    View {
                                        attr {
                                            height(32f)
                                            width(48f)
                                            allCenter()
                                        }
                                        event { click { page.selectPeriod(period) } }
                                        Text {
                                            attr {
                                                text(period.label)
                                                fontSize(13f)
                                                color(Color(if (page.selectedPeriod == period) 0xFF1677FF else 0xFF595959))
                                            }
                                        }
                                        vif({ page.selectedPeriod == period }) {
                                            View {
                                                attr {
                                                    positionAbsolute()
                                                    bottom(0f)
                                                    left(12f)
                                                    right(12f)
                                                    height(2f)
                                                    borderRadius(1f)
                                                    backgroundColor(Color(0xFF1677FF))
                                                }
                                            }
                                        }
                                    }
                                }
                                View {
                                    attr {
                                        height(32f)
                                        width(58f)
                                        allCenter()
                                    }
                                    event { click { page.showMorePeriods = !page.showMorePeriods } }
                                    Text {
                                        attr {
                                            text(if (page.showMorePeriods) "更多▲" else "更多▼")
                                            fontSize(13f)
                                            color(
                                                Color(
                                                    if (page.selectedPeriod in stockMorePeriods()) 0xFF1677FF
                                                    else 0xFF595959,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        StockChart({ page.candles }) {
                            attr {
                                flex(1f)
                                title = "${page.selectedPeriod.label} · 专业 K 线"
                                candleWidthRatio = 0.58f
                                movingAverages {
                                    show = true
                                    line(5, 0xFF595959)
                                    line(10, 0xFFFAAD14)
                                    line(20, 0xFF1677FF)
                                    line(30, 0xFFEB2F96)
                                }
                                volumePanel {
                                    show = true
                                    average(5, 0xFF595959)
                                    average(10, 0xFFFAAD14)
                                }
                                interaction { enableCrosshair = true }
                            }
                        }

                        vif({ page.showMorePeriods }) {
                            View {
                                attr {
                                    positionAbsolute()
                                    top(34f)
                                    right(42f)
                                    width(170f)
                                    padding(8f)
                                    zIndex(20)
                                    flexDirectionRow()
                                    borderRadius(8f)
                                    backgroundColor(Color(0xFFFFFFFF))
                                }
                                stockMorePeriods().chunked(4).forEach { column ->
                                    View {
                                        attr { flex(1f) }
                                        column.forEach { period ->
                                            View {
                                                attr {
                                                    height(30f)
                                                    borderRadius(5f)
                                                    allCenter()
                                                    backgroundColor(
                                                        Color(
                                                            if (page.selectedPeriod == period) 0x1A1677FF
                                                            else 0x00000000,
                                                        ),
                                                    )
                                                }
                                                event { click { page.selectPeriod(period) } }
                                                Text {
                                                    attr {
                                                        text(period.label)
                                                        fontSize(12f)
                                                        color(
                                                            Color(
                                                                if (page.selectedPeriod == period) 0xFF1677FF
                                                                else 0xFF595959,
                                                            ),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                stockDemoVariantSection(2, "OHLC 图", "左短线表示开盘，右短线表示收盘") {
                    OhlcChart({ page.candles }) {
                        attr {
                            flex(1f)
                        }
                    }
                }

                stockDemoVariantSection(3, "蜡烛柱图", "加宽实体柱，突出单周期涨跌区间") {
                    StockChart({ page.candles }) {
                        attr {
                            flex(1f)
                            title = "蜡烛柱走势"
                            candleWidthRatio = 0.78f
                        }
                    }
                }

                stockDemoVariantSection(4, "面积股票图", "收盘价趋势与渐变面积填充") {
                    StockAreaChart({ page.stockPoints }) {
                        attr {
                            flex(1f)
                        }
                    }
                }

                stockDemoVariantSection(5, "折线股票图", "轻量展示连续收盘价走势") {
                    StockLineChart({ page.stockPoints }) {
                        attr {
                            flex(1f)
                        }
                    }
                }

                stockDemoVariantSection(6, "Renko 砖形图", "忽略时间间隔，以固定价格变化生成砖块") {
                    RenkoChart({ page.renko }) {
                        attr {
                            flex(1f)
                        }
                    }
                }

                stockDemoVariantSection(7, "Kagi 图", "通过转折与线宽变化表达趋势反转") {
                    KagiChart({ page.kagi }) {
                        attr {
                            flex(1f)
                        }
                    }
                }

                stockDemoVariantSection(8, "点数图", "使用 X / O 列过滤小幅价格波动") {
                    PointFigureChart({ page.pointFigure }) {
                        attr {
                            flex(1f)
                        }
                    }
                }
            }
        }
    }
}
