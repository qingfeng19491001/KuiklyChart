package com.tencent.kuiklybase.chart.core.cartesian

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.ContextApi
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chart.config.SeriesCartesianChartAttr
import com.tencent.kuiklybase.chart.core.ChartCanvasRenderer
import com.tencent.kuiklybase.chart.core.toChartColor
import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.model.ChartSelection
import com.tencent.kuiklybase.chart.model.ChartSeries
import com.tencent.kuiklybase.chart.model.ChartViewport

abstract class CartesianChartView<A : SeriesCartesianChartAttr>(
    protected val seriesProvider: () -> ObservableList<ChartSeries>,
) : CartesianInteractiveView<A>() {
    protected var isCategoryX: Boolean = false
    protected var useCategoryHit: Boolean = false
    protected var useHorizontalHit: Boolean = false

    private var lastSeriesSnapshot: List<ChartSeries>? = null

    protected open fun computeDefaultViewport(data: List<ChartSeries>): ChartViewport {
        return ChartViewport.fromSeries(data, isCategoryX = isCategoryX)
    }

    override fun onBeforeResetViewport() {
        lastSeriesSnapshot = null
    }

    override fun syncDataFromProvider() {
        val data = seriesProvider().toList()
        val bounds = computeDefaultViewport(data)
        val changed = data != lastSeriesSnapshot
        lastSeriesSnapshot = data
        applyViewportBounds(bounds, changed)
    }

    protected fun drawChartAxes(
        context: ContextApi,
        layout: CartesianLayout,
        viewport: ChartViewport,
        series: List<ChartSeries>,
        horizontal: Boolean = false,
        preferCategoryLabels: Boolean = isCategoryX || horizontal,
    ) {
        val labeled = series.any { s -> s.points.any { it.label.isNotEmpty() } }
        val categoryTicks = when {
            preferCategoryLabels || labeled -> ChartCanvasRenderer.axisTicksFromSeries(series)
            else -> null
        }
        ChartCanvasRenderer.drawAxes(
            context,
            layout,
            viewport,
            config.theme.resolved(),
            config.xAxis.show,
            config.yAxis.show,
            xTicks = if (horizontal) null else categoryTicks,
            yTicks = if (horizontal) categoryTicks else null,
        )
    }

    override fun renderBelowCanvas(parent: ViewContainer<*, *>) {
        val ctx = this
        parent.apply {
            vif({ ctx.attr.legend.show }) {
                View {
                    attr {
                        flexDirection(FlexDirection.ROW)
                        justifyContent(FlexJustifyContent.CENTER)
                        alignItems(FlexAlign.CENTER)
                        padding(8f)
                        flexWrapWrap()
                    }
                    vfor({ ctx.seriesProvider() }) { series ->
                        View {
                            attr {
                                flexDirection(FlexDirection.ROW)
                                alignItems(FlexAlign.CENTER)
                                marginRight(12f)
                                marginBottom(4f)
                            }
                            View {
                                attr {
                                    size(10f, 10f)
                                    borderRadius(5f)
                                    backgroundColor(series.color.toChartColor())
                                    marginRight(4f)
                                }
                            }
                            Text {
                                attr {
                                    text(series.name)
                                    fontSize(ctx.attr.theme.fontSize)
                                    color(ctx.attr.theme.textColor.toChartColor())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun drawPlot(
        context: ContextApi,
        width: Float,
        height: Float,
        layout: CartesianLayout,
        viewport: ChartViewport,
        selection: ChartSelection?,
    ) {
        drawChart(
            context,
            width,
            height,
            layout,
            viewport,
            selection,
            seriesProvider().toList(),
        )
    }

    override fun onPlotClick(x: Float, y: Float) {
        val data = seriesProvider().toList()
        if (data.isEmpty() || canvasWidth <= 0f) return
        val scale = currentScale()
        val hit = when {
            useHorizontalHit -> CartesianHitTester.nearestHorizontalBar(data, scale, x, y)
            useCategoryHit -> CartesianHitTester.nearestBar(data, scale, x, y)
            else -> CartesianHitTester.nearestPoint(data, scale, x, y)
        } ?: return
        val point = data[hit.seriesIndex].points[hit.pointIndex]
        selection = ChartSelection.Cartesian(hit.seriesIndex, hit.pointIndex, point.label)
        event.onSelectionChange?.invoke(selection)
        event.onPointClick?.invoke(point, hit.seriesIndex, hit.pointIndex)
        val seriesName = data[hit.seriesIndex].name
        showSelectionTooltip(
            text = buildTooltipText(seriesName, point),
            localX = x,
            localY = y,
            crossX = scale.toPixelX(point.x),
            crossY = scale.toPixelY(point.y),
        )
    }

    protected open fun buildTooltipText(seriesName: String, point: ChartDataPoint): String {
        val label = point.label.ifEmpty { point.x.toString() }
        return if (seriesName.isNotEmpty()) "$seriesName · $label: ${point.y}" else "$label: ${point.y}"
    }

    protected abstract fun drawChart(
        context: ContextApi,
        width: Float,
        height: Float,
        layout: CartesianLayout,
        viewport: ChartViewport,
        selection: ChartSelection?,
        series: List<ChartSeries>,
    )
}

internal fun resolveViewportAfterDataChange(
    current: ChartViewport,
    newDefault: ChartViewport,
    hasUserViewportOverride: Boolean,
): ChartViewport = if (hasUserViewportOverride) current else newDefault

/** Root-relative tooltip position from canvas offset + local click point. */
internal fun resolveTooltipPosition(
    canvasOffsetX: Float,
    canvasOffsetY: Float,
    localX: Float,
    localY: Float,
    yOffset: Float = 28f,
): Pair<Float, Float> = canvasOffsetX + localX to canvasOffsetY + localY - yOffset

internal fun <A : SeriesCartesianChartAttr, T : CartesianChartView<A>> ViewContainer<*, *>.cartesianChartView(
    view: T,
    init: T.() -> Unit,
) {
    addChild(view, init)
}
