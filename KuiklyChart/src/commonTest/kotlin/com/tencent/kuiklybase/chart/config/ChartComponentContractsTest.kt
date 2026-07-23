package com.tencent.kuiklybase.chart.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChartComponentContractsTest {
    @Test
    fun cartesianEvent_exposesOnlyCartesianCallbacks() {
        val event = CartesianChartEvent()
        assertNull(event.onPointClick)
        assertNull(event.onViewportChange)
        assertNull(event.onSelectionChange)
        assertNull(event.onDragSelect)
        // Compile-time contract: CartesianChartEvent has no slice/radar fields.
        // Runtime smoke: registration helpers exist and assign handlers.
        event.pointClick { _, _, _ -> }
        event.dragSelect { }
        event.viewportChange { }
        event.selectionChange { }
        assertTrue(event.onPointClick != null)
        assertTrue(event.onDragSelect != null)
        assertTrue(event.onViewportChange != null)
        assertTrue(event.onSelectionChange != null)
    }

    @Test
    fun pieAndRadarEvents_areSplitByChartType() {
        val pie = PieChartEvent()
        pie.sliceClick { _, _ -> }
        assertTrue(pie.onSliceClick != null)

        val radar = RadarChartEvent()
        radar.radarClick { _, _, _ -> }
        assertTrue(radar.onRadarClick != null)
    }

    @Test
    fun themeOptions_resolved_returnsImmutableSnapshot() {
        val options = ChartThemeOptions().apply {
            primaryColor = 0xFF6C5CE7
            fontSize = 14f
        }
        val snapshot = options.resolved()
        assertEquals(0xFF6C5CE7, snapshot.primaryColor)
        assertEquals(14f, snapshot.fontSize)
        options.primaryColor = 0xFF000000
        assertEquals(0xFF6C5CE7, snapshot.primaryColor)
    }
}
