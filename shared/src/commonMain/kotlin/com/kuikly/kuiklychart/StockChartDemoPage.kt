package com.kuikly.kuiklychart

import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Scroller
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

/** 股票图变体画廊：统一提供点击提示、选中高亮和时间序列浏览反馈。 */
@Page("stock_chart_demo", supportInLocal = true)
internal class StockChartDemoPage : BasePager() {
    private var candles by observableList<OhlcPoint>()
    private var stockPoints by observableList<ChartDataPoint>()
    private var renko by observableList<ChartDataPoint>()
    private var kagi by observableList<ChartDataPoint>()
    private var pointFigure by observableList<PointFigureColumn>()

    override fun created() {
        super.created()
        candles.addAll(DemoSampleData.stockSeries())
        stockPoints.addAll(AdvancedDemoData.stockPoints())
        renko.addAll(AdvancedDemoData.renko())
        kagi.addAll(AdvancedDemoData.kagi())
        pointFigure.addAll(AdvancedDemoData.pointFigure())
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
                stockDemoVariantSection(1, "K 线图", "开高低收 · 红涨绿跌 · 时间窗口浏览") {
                    StockChart({ page.candles }) {
                        attr {
                            flex(1f)
                            title = "日 K 线"
                            candleWidthRatio = 0.54f
                            interaction {
                                enableTap = true
                                enablePan = true
                                enableScale = true
                                enableReset = true
                                lockY = true
                                initialVisibleRatio = 0.72f
                                clampToData = false
                            }
                        }
                    }
                }

                stockDemoVariantSection(2, "OHLC 图", "左短线表示开盘，右短线表示收盘") {
                    OhlcChart({ page.candles }) { attr { flex(1f) } }
                }

                stockDemoVariantSection(3, "蜡烛柱图", "加宽实体柱，突出单周期涨跌区间") {
                    StockChart({ page.candles }) {
                        attr {
                            flex(1f)
                            title = "蜡烛柱走势"
                            candleWidthRatio = 0.78f
                            theme {
                                upColor = 0xFFFF4D4F
                                downColor = 0xFF00A870
                            }
                            interaction {
                                enableTap = true
                                enablePan = true
                                enableScale = true
                                enableReset = true
                                lockY = true
                                initialVisibleRatio = 0.72f
                                clampToData = false
                            }
                        }
                    }
                }

                stockDemoVariantSection(4, "面积股票图", "收盘价趋势与渐变面积填充") {
                    StockAreaChart({ page.stockPoints }) { attr { flex(1f) } }
                }

                stockDemoVariantSection(5, "折线股票图", "轻量展示连续收盘价走势") {
                    StockLineChart({ page.stockPoints }) { attr { flex(1f) } }
                }

                stockDemoVariantSection(6, "Renko 砖形图", "忽略时间间隔，以固定价格变化生成砖块") {
                    RenkoChart({ page.renko }) { attr { flex(1f) } }
                }

                stockDemoVariantSection(7, "Kagi 图", "通过转折与线宽变化表达趋势反转") {
                    KagiChart({ page.kagi }) { attr { flex(1f) } }
                }

                stockDemoVariantSection(8, "点数图", "使用 X / O 列过滤小幅价格波动") {
                    PointFigureChart({ page.pointFigure }) { attr { flex(1f) } }
                }
            }
        }
    }
}
