package com.tencent.kuiklybase.chart.core.cartesian

import com.tencent.kuikly.core.base.event.Touch
import com.tencent.kuikly.core.base.event.TouchParams
import com.tencent.kuikly.core.views.TextAlign
import com.tencent.kuiklybase.chart.core.resolveDynamicXAxisTickCount
import com.tencent.kuiklybase.chart.core.resolveXAxisTickTextAlign
import com.tencent.kuiklybase.chart.model.ChartSeries
import com.tencent.kuiklybase.chart.model.ChartViewport
import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.config.ChartInteractionConfig
import com.tencent.kuiklybase.chart.config.VisibleAnchor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CartesianEngineTest {
    @Test
    fun layoutEngine_computesPlotWithinBounds() {
        val layout = CartesianLayoutEngine.compute(400f, 300f)
        assertTrue(layout.plot.width > 0f)
        assertTrue(layout.plot.height > 0f)
        assertTrue(layout.plot.left >= 0f)
        assertTrue(layout.plot.top >= 0f)
        assertTrue(layout.plot.right <= 400f)
        assertTrue(layout.plot.bottom <= 300f)
    }

    @Test
    fun scale_mapsDataToPixel() {
        val plot = PlotRect(40f, 36f, 380f, 240f)
        val viewport = ChartViewport(0f, 10f, 0f, 100f)
        val scale = CartesianScale(plot, viewport)
        assertEquals(40f, scale.toPixelX(0f))
        assertEquals(380f, scale.toPixelX(10f))
        assertEquals(240f, scale.toPixelY(0f))
        assertEquals(36f, scale.toPixelY(100f))
    }

    @Test
    fun viewport_fromSeries_addsPadding() {
        val series = listOf(
            ChartSeries(
                name = "A",
                color = 0xFF000000,
                points = listOf(
                    com.tencent.kuiklybase.chart.model.ChartDataPoint("a", 0f, 10f),
                    com.tencent.kuiklybase.chart.model.ChartDataPoint("b", 5f, 50f),
                ),
            ),
        )
        val vp = ChartViewport.fromSeries(series)
        assertTrue(vp.xMin < 0f)
        assertTrue(vp.xMax > 5f)
        assertTrue(vp.yMax > 50f)
    }

    @Test
    fun viewport_handlesSinglePointAndAllNegativeValues() {
        val single = ChartSeries("single", listOf(ChartDataPoint("", 4f, -8f)), 0L)
        val viewport = ChartViewport.fromSeries(listOf(single))
        assertTrue(viewport.xMin < 4f && viewport.xMax > 4f)
        assertTrue(viewport.yMin < -8f && viewport.yMax > -8f)

        val negative = ChartSeries(
            "negative",
            listOf(ChartDataPoint("", -5f, -10f), ChartDataPoint("", -2f, -3f)),
            0L,
        )
        val negativeViewport = ChartViewport.fromSeries(listOf(negative))
        assertTrue(negativeViewport.xMax < 0f)
        assertTrue(negativeViewport.yMax >= 0f)
    }

    @Test
    fun viewport_ignoresNonFinitePoints() {
        val series = ChartSeries(
            "invalid",
            listOf(ChartDataPoint("", Float.NaN, 1f), ChartDataPoint("", 2f, 3f)),
            0L,
        )
        val viewport = ChartViewport.fromSeries(listOf(series))
        assertTrue(viewport.xMin < 2f && viewport.xMax > 2f)
    }

    @Test
    fun dataChange_updatesOnlyAutomaticViewport() {
        val current = ChartViewport(0f, 10f, 0f, 10f)
        val updated = ChartViewport(10f, 20f, 10f, 20f)
        assertEquals(updated, resolveViewportAfterDataChange(current, updated, false))
        assertEquals(current, resolveViewportAfterDataChange(current, updated, true))
    }

    @Test
    fun pinchUsesCumulativeScaleFromGestureStart() {
        val interaction = ChartInteractionConfig().apply { enableScale = true }
        val changes = mutableListOf<ChartViewport>()
        val controller = ChartGestureController(
            interaction,
            ChartViewport(0f, 10f, 0f, 10f),
            ChartViewport(0f, 10f, 0f, 10f),
            changes::add,
        )
        val plot = PlotRect(0f, 0f, 100f, 100f)
        controller.onPinchStart()
        controller.onPinch(2f, 50f, 50f, CartesianScale(plot, controller.currentViewport()))
        controller.onPinch(4f, 50f, 50f, CartesianScale(plot, controller.currentViewport()))
        assertEquals(2.5f, controller.currentViewport().xMax - controller.currentViewport().xMin)
    }

    @Test
    fun panMovesViewportWithoutBrush() {
        val interaction = ChartInteractionConfig().apply {
            enablePan = true
            enableDragSelect = true
            brushZoom = true
            clampToData = false
        }
        val initial = ChartViewport(0f, 10f, 0f, 10f)
        val changes = mutableListOf<ChartViewport>()
        val controller = ChartGestureController(interaction, initial, initial, changes::add)
        val scale = CartesianScale(PlotRect(0f, 0f, 100f, 100f), initial)
        controller.onPanStart(20f, 50f, scale)
        controller.onPanMove(40f, 50f, scale)
        controller.onPanEnd()
        // 未长按武装时单指拖动走平移，不框选
        assertTrue(changes.isNotEmpty())
        assertTrue(controller.currentViewport().xMin < initial.xMin)
    }

    @Test
    fun panWorksFromInitialFocusedWindowWithoutPinch() {
        // clamp 开着，开局居中 55% 窗口，不捏合也应能左右拖
        val interaction = ChartInteractionConfig().apply {
            enablePan = true
            clampToData = true
            lockY = true
            initialVisibleRatio = 0.55f
            initialVisibleAnchor = VisibleAnchor.CENTER
        }
        val bounds = ChartViewport(0f, 10f, 0f, 100f)
        val home = bounds.focusedXWindow(
            interaction.initialVisibleRatio,
            interaction.initialVisibleAnchor,
        )
        assertTrue(home.xMax - home.xMin < bounds.xMax - bounds.xMin)
        assertTrue(home.xMin > bounds.xMin)
        assertTrue(home.xMax < bounds.xMax)

        val changes = mutableListOf<ChartViewport>()
        val controller = ChartGestureController(interaction, home, bounds, changes::add)
        val scale = CartesianScale(PlotRect(0f, 0f, 100f, 100f), home)
        controller.onPanStart(50f, 50f, scale)
        controller.onPanMove(80f, 50f, scale)
        controller.onPanEnd()

        assertTrue(changes.isNotEmpty())
        assertTrue(controller.currentViewport().xMin < home.xMin)
        assertTrue(controller.currentViewport().xMin >= bounds.xMin)
    }

    @Test
    fun panIsNoOpWhenViewportAlreadyFullWidth() {
        // 旧问题：视口=全量时 clamp 会吞掉平移；证明「必须先放大」的根因仍存在于全幅态
        val interaction = ChartInteractionConfig().apply {
            enablePan = true
            clampToData = true
        }
        val full = ChartViewport(0f, 10f, 0f, 10f)
        val controller = ChartGestureController(interaction, full, full) {}
        val scale = CartesianScale(PlotRect(0f, 0f, 100f, 100f), full)
        controller.onPanStart(20f, 50f, scale)
        controller.onPanMove(80f, 50f, scale)
        assertEquals(0f, controller.currentViewport().xMin)
        assertEquals(10f, controller.currentViewport().xMax)
    }

    @Test
    fun longPressBrushZoomsIntoSelection() {
        val interaction = ChartInteractionConfig().apply {
            enablePan = true
            enableDragSelect = true
            brushZoom = true
            clampToData = false
        }
        val initial = ChartViewport(0f, 10f, 0f, 10f)
        val controller = ChartGestureController(interaction, initial, initial) {}
        val scale = CartesianScale(PlotRect(0f, 0f, 100f, 100f), initial)
        controller.onBrushStart(20f, scale)
        controller.onBrushMove(80f, scale)
        val range = assertNotNull(controller.onBrushEnd())
        assertEquals(2f, range.start)
        assertEquals(8f, range.endInclusive)
        // brushZoom 后视口收窄到选区附近
        assertTrue(controller.currentViewport().xMax - controller.currentViewport().xMin < 10f)
        assertTrue(controller.currentViewport().xMin >= 1.5f)
        assertTrue(controller.currentViewport().xMax <= 8.5f)
    }

    @Test
    fun longPressBrushContinuesWhenNormalPanIsDisabled() {
        val interaction = ChartInteractionConfig().apply {
            enablePan = false
            enableDragSelect = true
            brushZoom = false
            clampToData = false
        }
        val initial = ChartViewport(0f, 10f, 0f, 10f)
        val controller = ChartGestureController(interaction, initial, initial) {}
        var finished: ClosedFloatingPointRange<Float>? = null
        val handler = ChartTouchViewportHandler(
            interaction = interaction,
            controllerProvider = { controller },
            scaleProvider = { CartesianScale(PlotRect(0f, 0f, 100f, 100f), initial) },
            onBrushRangeChanged = {},
            onBrushFinished = { finished = it },
            onCrosshair = { _, _ -> },
        )

        handler.beginBrush(20f)
        handler.onNativePan("move", 80f, 50f)
        handler.onNativePan("end", 80f, 50f)

        val range = assertNotNull(finished)
        assertEquals(2f, range.start)
        assertEquals(8f, range.endInclusive)
    }

    @Test
    fun explicitTouchLifecycleEndsAndRestartsPinchWithoutActionField() {
        val interaction = ChartInteractionConfig().apply {
            enableScale = true
            clampToData = false
        }
        val initial = ChartViewport(0f, 10f, 0f, 10f)
        val controller = ChartGestureController(interaction, initial, initial) {}
        val plot = PlotRect(0f, 0f, 100f, 100f)
        val handler = ChartTouchViewportHandler(
            interaction = interaction,
            controllerProvider = { controller },
            scaleProvider = { CartesianScale(plot, controller.currentViewport()) },
            onBrushRangeChanged = {},
            onBrushFinished = {},
            onCrosshair = { _, _ -> },
        )

        handler.onTouchDown(touchParams(40f, 60f))
        handler.onTouchMove(touchParams(30f, 70f))
        handler.onTouchUp(touchParams(30f))
        assertEquals(5f, controller.currentViewport().xMax - controller.currentViewport().xMin)

        handler.onTouchDown(touchParams(40f, 60f))
        handler.onTouchMove(touchParams(40f, 60f))
        assertEquals(5f, controller.currentViewport().xMax - controller.currentViewport().xMin)
    }

    @Test
    fun lockYKeepsVerticalRangeOnPinch() {
        val interaction = ChartInteractionConfig().apply {
            enableScale = true
            lockY = true
            clampToData = false
        }
        val initial = ChartViewport(0f, 10f, 0f, 10f)
        val controller = ChartGestureController(interaction, initial, initial) {}
        val plot = PlotRect(0f, 0f, 100f, 100f)
        controller.onPinchStart()
        controller.onPinch(2f, 50f, 50f, CartesianScale(plot, initial))
        assertEquals(0f, controller.currentViewport().yMin)
        assertEquals(10f, controller.currentViewport().yMax)
        assertEquals(5f, controller.currentViewport().xMax - controller.currentViewport().xMin)
    }

    @Test
    fun focusedXWindow_endsAtRightEdge() {
        val full = ChartViewport(0f, 10f, 0f, 100f)
        val home = full.focusedXWindow(0.5f, com.tencent.kuiklybase.chart.config.VisibleAnchor.END)
        assertEquals(5f, home.xMin)
        assertEquals(10f, home.xMax)
        assertEquals(0f, home.yMin)
        assertEquals(100f, home.yMax)
    }

    @Test
    fun tooltipPosition_usesCanvasOffsetPlusLocalPoint() {
        val tip = resolveTooltipPosition(12f, 34f, 50f, 80f)
        assertEquals(62f, tip.first)
        assertEquals(86f, tip.second)
    }

    @Test
    fun tooltipPosition_flipsToLeftNearRightEdge() {
        val tip = resolveTooltipPosition(
            canvasOffsetX = 0f,
            canvasOffsetY = 0f,
            localX = 280f,
            localY = 80f,
            containerWidth = 300f,
            tooltipWidth = 100f,
        )
        assertEquals(172f, tip.first)
        assertEquals(52f, tip.second)
    }

    @Test
    fun tooltipPosition_staysRightWhenSpaceAllows() {
        val tip = resolveTooltipPosition(
            canvasOffsetX = 0f,
            canvasOffsetY = 0f,
            localX = 40f,
            localY = 80f,
            containerWidth = 300f,
            tooltipWidth = 100f,
        )
        assertEquals(48f, tip.first)
    }

    @Test
    fun xAxisTickAlignment_keepsEdgeLabelsInsideCanvas() {
        val plot = PlotRect(40f, 8f, 288f, 180f)
        assertEquals(TextAlign.LEFT, resolveXAxisTickTextAlign(40f, plot))
        assertEquals(TextAlign.CENTER, resolveXAxisTickTextAlign(160f, plot))
        assertEquals(TextAlign.RIGHT, resolveXAxisTickTextAlign(288f, plot))
    }

    @Test
    fun dynamicXAxisTickCount_keepsShortLabelsWhenTheyFit() {
        val labels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        assertEquals(7, resolveDynamicXAxisTickCount(248f, labels, 12f))
    }

    @Test
    fun dynamicXAxisTickCount_samplesLongLabelsWhenSpaceIsLimited() {
        val labels = List(8) { index -> "2026-08-${index + 1}" }
        val count = resolveDynamicXAxisTickCount(248f, labels, 12f)
        assertTrue(count in 2 until labels.size)
    }

    @Test
    fun viewport_fromStackedSeries_sumsCategoryTotals() {
        val series = listOf(
            ChartSeries(
                "A",
                listOf(ChartDataPoint("Q1", 0f, 10f), ChartDataPoint("Q2", 1f, 20f)),
                0xFF0000FF,
            ),
            ChartSeries(
                "B",
                listOf(ChartDataPoint("Q1", 0f, 5f), ChartDataPoint("Q2", 1f, 15f)),
                0xFFFF0000,
            ),
        )
        val vp = ChartViewport.fromSeries(series, isCategoryX = true, stacked = true)
        assertTrue(vp.yMax > 35f)
    }

    @Test
    fun viewport_fromOhlc_coversHighLow() {
        val points = listOf(
            com.tencent.kuiklybase.chart.model.OhlcPoint("D1", 0f, 10f, 15f, 8f, 12f),
            com.tencent.kuiklybase.chart.model.OhlcPoint("D2", 1f, 12f, 18f, 11f, 14f),
        )
        val vp = ChartViewport.fromOhlc(points)
        assertTrue(vp.yMin < 8f)
        assertTrue(vp.yMax > 18f)
    }

    @Test
    fun axisTicksFromSeries_preferPointLabels() {
        val series = listOf(
            ChartSeries(
                "sales",
                listOf(
                    ChartDataPoint("一月", 0f, 10f),
                    ChartDataPoint("二月", 1f, 20f),
                    ChartDataPoint("", 2f, 15f),
                ),
                0xFF4F8FFF,
            ),
        )
        val ticks = com.tencent.kuiklybase.chart.core.ChartCanvasRenderer.axisTicksFromSeries(series)
        assertEquals(3, ticks.size)
        assertEquals("一月", ticks[0].text)
        assertEquals("二月", ticks[1].text)
        assertEquals("2", ticks[2].text)
    }

    @Test
    fun hitTester_findsNearestPoint() {
        val series = listOf(
            ChartSeries(
                name = "A",
                color = 0xFF000000,
                points = listOf(
                    com.tencent.kuiklybase.chart.model.ChartDataPoint("", 0f, 0f),
                    com.tencent.kuiklybase.chart.model.ChartDataPoint("", 10f, 10f),
                ),
            ),
        )
        val plot = PlotRect(0f, 0f, 100f, 100f)
        val scale = CartesianScale(plot, ChartViewport(0f, 10f, 0f, 10f))
        val hit = CartesianHitTester.nearestPoint(series, scale, 100f, 0f, threshold = 20f)
        assertNotNull(hit)
        assertEquals(1, hit.pointIndex)
    }

    @Test
    fun selectedPointCrosshair_recomputesAfterViewportChanges() {
        val series = listOf(
            ChartSeries("趋势", listOf(ChartDataPoint("中点", 5f, 50f)), 0L),
        )
        val plot = PlotRect(40f, 10f, 240f, 210f)
        val selection = com.tencent.kuiklybase.chart.model.ChartSelection.Cartesian(0, 0, "中点")

        val full = resolveCartesianSelectionCrosshair(
            series, selection, plot, ChartViewport(0f, 10f, 0f, 100f),
        )
        val zoomed = resolveCartesianSelectionCrosshair(
            series, selection, plot, ChartViewport(4f, 8f, 0f, 100f),
        )

        assertEquals(140f, full?.first)
        assertEquals(90f, zoomed?.first)
        assertEquals(110f, zoomed?.second)
    }

    @Test
    fun selectedPointCrosshair_hidesWhenPointLeavesPlot() {
        val series = listOf(
            ChartSeries("trend", listOf(ChartDataPoint("start", 1f, 50f)), 0L),
        )
        val crosshair = resolveCartesianSelectionCrosshair(
            series,
            com.tencent.kuiklybase.chart.model.ChartSelection.Cartesian(0, 0, "start"),
            PlotRect(40f, 10f, 240f, 210f),
            ChartViewport(4f, 8f, 0f, 100f),
        )

        assertEquals(null, crosshair)
    }

    @Test
    fun stackedBarHitTester_selectsActualStackSegment() {
        val series = listOf(
            ChartSeries("bottom", listOf(ChartDataPoint("A", 0f, 40f)), 0L),
            ChartSeries("top", listOf(ChartDataPoint("A", 0f, 60f)), 0L),
        )
        val scale = CartesianScale(PlotRect(0f, 0f, 100f, 100f), ChartViewport(-1f, 1f, 0f, 100f))

        val hit = CartesianHitTester.nearestBar(
            series,
            scale,
            x = 50f,
            y = 20f,
            stacked = true,
        )

        assertNotNull(hit)
        assertEquals(1, hit.seriesIndex)
    }

    @Test
    fun groupedBarHitTester_selectsSecondSeriesBar() {
        val series = listOf(
            ChartSeries("direct", listOf(ChartDataPoint("A", 0f, 40f)), 0L),
            ChartSeries("partner", listOf(ChartDataPoint("A", 0f, 60f)), 0L),
        )
        val scale = CartesianScale(PlotRect(0f, 0f, 100f, 100f), ChartViewport(-1f, 1f, 0f, 100f))

        val hit = CartesianHitTester.nearestBar(series, scale, x = 27f, y = 30f, grouped = true)

        assertNotNull(hit)
        assertEquals(1, hit.seriesIndex)
    }

    @Test
    fun stackedHorizontalBarHitTester_selectsActualStackSegment() {
        val series = listOf(
            ChartSeries("left", listOf(ChartDataPoint("A", 0f, 40f)), 0L),
            ChartSeries("right", listOf(ChartDataPoint("A", 0f, 60f)), 0L),
        )
        val scale = CartesianScale(PlotRect(0f, 0f, 100f, 100f), ChartViewport(0f, 100f, -1f, 1f))

        val hit = CartesianHitTester.nearestHorizontalBar(
            series,
            scale,
            x = 80f,
            y = 50f,
            stacked = true,
        )

        assertNotNull(hit)
        assertEquals(1, hit.seriesIndex)
    }

    @Test
    fun barHitTester_ignoresBlankArea() {
        val series = listOf(
            ChartSeries("bars", listOf(ChartDataPoint("A", 0f, 40f)), 0L),
        )
        val scale = CartesianScale(PlotRect(0f, 0f, 100f, 100f), ChartViewport(-1f, 1f, 0f, 100f))

        assertEquals(null, CartesianHitTester.nearestBar(series, scale, x = 50f, y = 80f))
    }

    @Test
    fun horizontalBarHitTester_ignoresBlankArea() {
        val series = listOf(
            ChartSeries("bars", listOf(ChartDataPoint("A", 0f, 40f)), 0L),
        )
        val scale = CartesianScale(PlotRect(0f, 0f, 100f, 100f), ChartViewport(0f, 100f, -1f, 1f))

        assertEquals(null, CartesianHitTester.nearestHorizontalBar(series, scale, x = 80f, y = 20f))
    }

    @Test
    fun interactiveLegendFiltersWithoutMutatingSourceSeries() {
        val source = listOf(
            ChartSeries("A", emptyList(), 0L),
            ChartSeries("B", emptyList(), 0L),
        )
        val hidden = toggleHiddenSeries(emptySet(), "A")

        assertEquals(listOf("B"), filterVisibleSeries(source, hidden).map { it.name })
        assertEquals(listOf("A", "B"), source.map { it.name })
        assertTrue(toggleHiddenSeries(hidden, "A").isEmpty())
    }

    private fun touchParams(vararg xCoordinates: Float): TouchParams {
        val touches = xCoordinates.mapIndexed { index, x ->
            Touch(x, 50f, x, 50f, index.toFloat(), index.toLong())
        }
        return TouchParams(
            x = xCoordinates.firstOrNull() ?: 0f,
            y = 50f,
            pageX = xCoordinates.firstOrNull() ?: 0f,
            pageY = 50f,
            timestamp = 0L,
            pointerId = 0,
            action = "",
            touches = touches,
            consumed = false,
        )
    }
}
