package com.kuikly.kuiklychart

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Scroller
import com.kuikly.kuiklychart.base.BasePager
import com.tencent.kuiklybase.chart.config.ChartAnnotationConfig
import com.tencent.kuiklybase.chart.config.ChartInteractionConfig
import com.tencent.kuiklybase.chart.config.ChartThresholdConfig
import com.tencent.kuiklybase.chart.line.LineChart
import com.tencent.kuiklybase.chart.model.ChartSeries

/**
 * 折线图变体 Demo：纵向滑动的卡片列表，覆盖基础 / 平滑 / 多系列 / 阈值 / 缺失值 / 填充 / 注释 7 种用法。
 *
 * 卡片之间互不耦合，各 demo 单独维护 series 与 attr，便于阅读每个变体的最小可行写法。
 */
@Page("line_chart_demo", supportInLocal = true)
internal class LineChartDemoPage : BasePager() {

    private var basic by observableList<ChartSeries>()
    private var smooth by observableList<ChartSeries>()
    private var multi by observableList<ChartSeries>()
    private var threshold by observableList<ChartSeries>()
    private var connectNulls by observableList<ChartSeries>()
    private var withFill by observableList<ChartSeries>()
    private var annotated by observableList<ChartSeries>()

    override fun created() {
        super.created()
        basic.addAll(DemoSampleData.lineBasic())
        smooth.addAll(DemoSampleData.lineSmooth())
        multi.addAll(DemoSampleData.lineMultiSeries())
        threshold.addAll(DemoSampleData.lineThreshold())
        connectNulls.addAll(DemoSampleData.lineConnectNulls())
        withFill.addAll(DemoSampleData.lineWithFill())
        annotated.addAll(DemoSampleData.lineWithAnnotation())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return chartDemoShell(ctx, "LineChart") {
            Scroller {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                    showScrollerIndicator(false)
                    paddingBottom(24f)
                }

                ctx.run {
                    this@Scroller.basicCard()
                    this@Scroller.smoothCard()
                    this@Scroller.multiCard()
                    this@Scroller.thresholdCard()
                    this@Scroller.connectNullsCard()
                    this@Scroller.fillCard()
                    this@Scroller.annotationCard()
                }
            }
        }
    }

    // —— 7 个 Demo 卡片 —— //

    private fun ViewContainer<*, *>.basicCard() {
        val page = this@LineChartDemoPage
        page.run {
            this@basicCard.demoVariantSection(1, "基础折线图") {
                LineChart({ page.basic }) {
                    attr {
                        flex(1f)
                        title = "基础折线图"
                        smooth = false
                        showPoints = true
                        pointRadius = 4f
                        xAxis { show = true }
                        yAxis { show = true }
                        grid { show = true }
                        legend { show = false }
                        interaction { enableLineDemoInteraction() }
                        tooltip {
                            formatter { context ->
                                "${context.label} · ${context.items.firstOrNull()?.point?.y ?: "-"}"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ViewContainer<*, *>.smoothCard() {
        val page = this@LineChartDemoPage
        page.run {
            this@smoothCard.demoVariantSection(2, "平滑折线图") {
                LineChart({ page.smooth }) {
                    attr {
                        flex(1f)
                        title = "平滑折线图"
                        smooth = true
                        showPoints = true
                        pointRadius = 4f
                        xAxis { show = true }
                        yAxis { show = true }
                        grid { show = true }
                        legend { show = false }
                        interaction { enableLineDemoInteraction() }
                    }
                }
            }
        }
    }

    private fun ViewContainer<*, *>.multiCard() {
        val ctx = this@LineChartDemoPage
        ctx.run {
            this@multiCard.demoVariantSection(3, "系列折线图") {
                LineChart({ ctx.multi }) {
                    attr {
                        flex(1f)
                        title = "门店销售对比"
                        smooth = false
                        showPoints = true
                        pointRadius = 3f
                        xAxis { show = true }
                        yAxis { show = true }
                        grid { show = true }
                        legend {
                            show = true
                            interactive = true
                        }
                        interaction { enableLineDemoInteraction() }
                        tooltip { sharedByX = true }
                    }
                }
            }
        }
    }

    private fun ViewContainer<*, *>.thresholdCard() {
        val page = this@LineChartDemoPage
        page.run {
            this@thresholdCard.demoVariantSection(4, "阈值折线图") {
                LineChart({ page.threshold }) {
                    attr {
                        flex(1f)
                        title = "温度趋势（警戒 55°）"
                        smooth = false
                        showPoints = true
                        pointRadius = 4f
                        xAxis { show = true }
                        yAxis { show = true }
                        grid { show = true }
                        legend { show = false }
                        interaction { enableLineDemoInteraction() }
                        thresholds {
                            add(
                                ChartThresholdConfig(
                                    value = 55f,
                                    label = "警戒 55°",
                                    color = 0xFFFAAD14,
                                    dashWidth = 4f,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun ViewContainer<*, *>.connectNullsCard() {
        val page = this@LineChartDemoPage
        page.run {
            this@connectNullsCard.demoVariantSection(5, "连接空值") {
                LineChart({ page.connectNulls }) {
                    attr {
                        flex(1f)
                        title = "股价（含 3、4 月缺失）"
                        smooth = false
                        showPoints = true
                        pointRadius = 4f
                        connectNulls = true
                        xAxis { show = true }
                        yAxis { show = true }
                        grid { show = true }
                        legend { show = false }
                        interaction { enableLineDemoInteraction() }
                    }
                }
            }
        }
    }

    private fun ViewContainer<*, *>.fillCard() {
        val page = this@LineChartDemoPage
        page.run {
            this@fillCard.demoVariantSection(6, "添加填充色") {
                LineChart({ page.withFill }) {
                    attr {
                        flex(1f)
                        title = "访问量（带填充）"
                        smooth = true
                        showPoints = true
                        pointRadius = 4f
                        fillBelow = true
                        xAxis { show = true }
                        yAxis { show = true }
                        grid { show = true }
                        legend { show = false }
                        interaction { enableLineDemoInteraction() }
                    }
                }
            }
        }
    }

    private fun ViewContainer<*, *>.annotationCard() {
        val page = this@LineChartDemoPage
        page.run {
            this@annotationCard.demoVariantSection(7, "文本标记") {
                LineChart({ page.annotated }) {
                    attr {
                        flex(1f)
                        title = "搜索热度（关键点位注释）"
                        smooth = false
                        showPoints = true
                        pointRadius = 4f
                        xAxis { show = true }
                        yAxis { show = true }
                        grid { show = true }
                        legend { show = true }
                        interaction { enableLineDemoInteraction() }
                        annotations {
                            add(
                                ChartAnnotationConfig(
                                    text = "5 月峰值 96",
                                    dataX = 5f,
                                    dataY = 96f,
                                    dx = 12f,
                                    dy = -10f,
                                    color = 0xFF2C3542,
                                    connector = true,
                                    anchorPoint = true,
                                    fontSize = 11f,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun ChartInteractionConfig.enableLineDemoInteraction() {
    enableTap = true
    enablePan = true
    enableScale = true
    enableReset = true
    enableCrosshair = true
    enableDragSelect = true
    brushZoom = true
    lockY = true
    initialVisibleRatio = 1f
    clampToData = false
}
