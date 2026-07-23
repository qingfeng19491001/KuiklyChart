package com.tencent.kuiklybase.chart.core.cartesian

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.layoutFrameDidChange
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.ContextApi
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chart.config.CartesianChartAttr
import com.tencent.kuiklybase.chart.config.CartesianChartEvent
import com.tencent.kuiklybase.chart.core.toChartColor
import com.tencent.kuiklybase.chart.model.ChartSelection
import com.tencent.kuiklybase.chart.model.ChartViewport

/**
 * 笛卡尔图交互壳：视口 / 手势 / Tooltip / 框选与十字准星。
 * 系列图与 K 线共用，子类只负责数据同步、绘制与点击命中。
 */
abstract class CartesianInteractiveView<A : CartesianChartAttr> :
    ComposeView<A, CartesianChartEvent>() {

    var viewport by observable(ChartViewport(0f, 1f, 0f, 1f))
        private set
    var dataBounds by observable(ChartViewport(0f, 1f, 0f, 1f))
        private set
    var homeViewport by observable(ChartViewport(0f, 1f, 0f, 1f))
        private set
    var selection by observable<ChartSelection?>(null)
        protected set

    protected var dragSelection by observable<ClosedFloatingPointRange<Float>?>(null)
    protected var crosshairX by observable<Float?>(null)
    protected var crosshairY by observable<Float?>(null)
    protected var tooltipText by observable("")
    protected var tooltipX by observable(0f)
    protected var tooltipY by observable(0f)
    protected var showTooltip by observable(false)
    protected var canvasWidth by observable(0f)
    protected var canvasHeight by observable(0f)
    protected var canvasOffsetX by observable(0f)
    protected var canvasOffsetY by observable(0f)

    private var gestureController: ChartGestureController? = null
    private var touchHandler: ChartTouchViewportHandler? = null
    private var hasUserViewportOverride = false

    protected val config: A
        get() = attr

    override fun createEvent() = CartesianChartEvent()

    protected fun applyViewportBounds(bounds: ChartViewport, fingerprintChanged: Boolean) {
        val home = bounds.focusedXWindow(
            ratio = attr.interaction.initialVisibleRatio,
            anchor = attr.interaction.initialVisibleAnchor,
        )
        if (!fingerprintChanged && bounds == dataBounds && home == homeViewport) return
        dataBounds = bounds
        homeViewport = home
        viewport = resolveViewportAfterDataChange(viewport, home, hasUserViewportOverride)
        bindGestureController()
    }

    protected fun resetViewport() {
        hasUserViewportOverride = false
        onBeforeResetViewport()
        syncDataFromProvider()
        viewport = homeViewport
        dragSelection = null
        crosshairX = null
        crosshairY = null
        showTooltip = false
        selection = null
        bindGestureController()
        event.onViewportChange?.invoke(viewport)
        event.onSelectionChange?.invoke(null)
    }

    protected open fun onBeforeResetViewport() {}

    protected fun ensureGestureReady() {
        if (gestureController == null || touchHandler == null) {
            bindGestureController()
        }
    }

    private fun bindGestureController() {
        gestureController = ChartGestureController(
            interaction = attr.interaction,
            viewport = viewport,
            defaultViewport = dataBounds,
            onViewportChanged = { newViewport ->
                hasUserViewportOverride = true
                viewport = newViewport
                event.onViewportChange?.invoke(newViewport)
            },
        )
        if (touchHandler == null) {
            touchHandler = ChartTouchViewportHandler(
                interaction = attr.interaction,
                controllerProvider = { gestureController },
                scaleProvider = { currentScale() },
                onBrushRangeChanged = { dragSelection = it },
                onBrushFinished = { range ->
                    if (range != null) event.onDragSelect?.invoke(range)
                },
                onCrosshair = { x, y ->
                    crosshairX = x
                    crosshairY = y
                },
                preferNativePan = true,
            )
        }
    }

    protected fun currentScale(): CartesianScale {
        val w = canvasWidth.coerceAtLeast(1f)
        val h = canvasHeight.coerceAtLeast(1f)
        return CartesianScale(CartesianLayoutEngine.compute(w, h).plot, viewport)
    }

    protected fun showSelectionTooltip(
        text: String,
        localX: Float,
        localY: Float,
        crossX: Float?,
        crossY: Float? = null,
    ) {
        tooltipText = text
        val tip = resolveTooltipPosition(canvasOffsetX, canvasOffsetY, localX, localY)
        tooltipX = tip.first
        tooltipY = tip.second
        showTooltip = true
        if (attr.interaction.enableCrosshair) {
            crosshairX = crossX
            crosshairY = crossY
        }
    }

    override fun created() {
        super.created()
        syncDataFromProvider()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                    backgroundColor(ctx.attr.theme.backgroundColor.toChartColor())
                }
                vif({ ctx.attr.title.isNotEmpty() }) {
                    Text {
                        attr {
                            text(ctx.attr.title)
                            fontSize(16f)
                            fontWeightSemiBold()
                            color(ctx.attr.theme.textColor.toChartColor())
                            marginBottom(4f)
                            marginLeft(12f)
                        }
                    }
                }
                View {
                    attr {
                        flex(1f)
                    }
                    event {
                        layoutFrameDidChange { frame ->
                            ctx.canvasOffsetX = frame.x
                            ctx.canvasOffsetY = frame.y
                            if (frame.width > 0f) ctx.canvasWidth = frame.width
                            if (frame.height > 0f) ctx.canvasHeight = frame.height
                        }
                        click { params ->
                            if (!ctx.attr.interaction.enableTap) return@click
                            if (ctx.touchHandler?.wasMoved() == true) return@click
                            ctx.onPlotClick(params.x, params.y)
                        }
                        doubleClick {
                            if (!ctx.attr.interaction.enableReset) return@doubleClick
                            ctx.resetViewport()
                        }
                        longPress { params ->
                            if (!ctx.attr.interaction.enableDragSelect) return@longPress
                            if (params.state == "start") {
                                ctx.ensureGestureReady()
                                ctx.gestureController?.armBrush()
                                ctx.touchHandler?.beginBrush(params.x)
                            }
                        }
                        pan { params ->
                            if (!ctx.attr.interaction.enablePan) return@pan
                            ctx.ensureGestureReady()
                            ctx.touchHandler?.onNativePan(params.state, params.x, params.y)
                        }
                        touchDown { params ->
                            ctx.ensureGestureReady()
                            ctx.touchHandler?.onTouch(params)
                        }
                        touchMove { params ->
                            ctx.touchHandler?.onTouch(params)
                        }
                        touchUp { params ->
                            ctx.touchHandler?.onTouch(params)
                        }
                    }
                    Canvas({ attr { flex(1f) } }) { context, width, height ->
                        ctx.canvasWidth = width
                        ctx.canvasHeight = height
                        ctx.syncDataFromProvider()
                        val layout = CartesianLayoutEngine.compute(width, height)
                        val currentViewport = ctx.viewport
                        ctx.drawPlot(context, width, height, layout, currentViewport, ctx.selection)
                        CartesianOverlayRenderer.drawBrush(
                            context, layout, currentViewport, ctx.dragSelection,
                        )
                        CartesianOverlayRenderer.drawCrosshair(
                            context,
                            layout,
                            ctx.attr.interaction.enableCrosshair,
                            ctx.crosshairX,
                            ctx.crosshairY,
                        )
                    }
                }
                ctx.renderBelowCanvas(this)
                vif({ ctx.showTooltip }) {
                    View {
                        attr {
                            positionAbsolute()
                            left(ctx.tooltipX)
                            top(ctx.tooltipY)
                            backgroundColor(Color(0xEE1F2A37))
                            borderRadius(6f)
                            padding(8f, 10f, 8f, 10f)
                        }
                        Text {
                            attr {
                                text(ctx.tooltipText)
                                fontSize(12f)
                                color(Color.WHITE)
                            }
                        }
                    }
                }
            }
        }
    }

    /** 图例等画布下方内容；默认无。 */
    protected open fun renderBelowCanvas(parent: ViewContainer<*, *>) {}

    protected abstract fun syncDataFromProvider()

    protected abstract fun drawPlot(
        context: ContextApi,
        width: Float,
        height: Float,
        layout: CartesianLayout,
        viewport: ChartViewport,
        selection: ChartSelection?,
    )

    protected abstract fun onPlotClick(x: Float, y: Float)
}

internal object CartesianOverlayRenderer {
    fun drawBrush(
        context: ContextApi,
        layout: CartesianLayout,
        currentViewport: ChartViewport,
        range: ClosedFloatingPointRange<Float>?,
    ) {
        if (range == null) return
        val scale = CartesianScale(layout.plot, currentViewport)
        val left = scale.toPixelX(range.start).coerceIn(layout.plot.left, layout.plot.right)
        val right = scale.toPixelX(range.endInclusive).coerceIn(layout.plot.left, layout.plot.right)
        val l = minOf(left, right)
        val r = maxOf(left, right)
        val plot = layout.plot
        context.beginPath()
        context.moveTo(plot.left, plot.top)
        context.lineTo(l, plot.top)
        context.lineTo(l, plot.bottom)
        context.lineTo(plot.left, plot.bottom)
        context.closePath()
        context.fillStyle(Color(0x33101827))
        context.fill()
        context.beginPath()
        context.moveTo(r, plot.top)
        context.lineTo(plot.right, plot.top)
        context.lineTo(plot.right, plot.bottom)
        context.lineTo(r, plot.bottom)
        context.closePath()
        context.fillStyle(Color(0x33101827))
        context.fill()
        context.beginPath()
        context.moveTo(l, plot.top)
        context.lineTo(r, plot.top)
        context.lineTo(r, plot.bottom)
        context.lineTo(l, plot.bottom)
        context.closePath()
        context.fillStyle(Color(0x334F8FFF))
        context.fill()
        context.beginPath()
        context.strokeStyle(Color(0xFF4F8FFF))
        context.lineWidth(1.5f)
        context.moveTo(l, plot.top)
        context.lineTo(l, plot.bottom)
        context.moveTo(r, plot.top)
        context.lineTo(r, plot.bottom)
        context.stroke()
    }

    fun drawCrosshair(
        context: ContextApi,
        layout: CartesianLayout,
        enabled: Boolean,
        cx: Float?,
        cy: Float?,
    ) {
        if (!enabled) return
        if (cx == null && cy == null) return
        val plot = layout.plot
        context.strokeStyle(Color(0x994F8FFF))
        context.lineWidth(1f)
        if (cx != null) {
            val x = cx.coerceIn(plot.left, plot.right)
            context.beginPath()
            context.moveTo(x, plot.top)
            context.lineTo(x, plot.bottom)
            context.stroke()
        }
        if (cy != null) {
            val y = cy.coerceIn(plot.top, plot.bottom)
            context.beginPath()
            context.moveTo(plot.left, y)
            context.lineTo(plot.right, y)
            context.stroke()
        }
    }
}
