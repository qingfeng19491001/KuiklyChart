package com.tencent.kuiklybase.chart.advanced

import com.tencent.kuiklybase.chart.config.StockThemePreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StockVariantInteractionTest {
    @Test
    fun everyAdvancedStockKind_supportsHorizontalViewport() {
        val kinds = listOf(
            AdvancedChartKind.OHLC,
            AdvancedChartKind.STOCK_AREA,
            AdvancedChartKind.STOCK_LINE,
            AdvancedChartKind.RENKO,
            AdvancedChartKind.KAGI,
            AdvancedChartKind.POINT_FIGURE,
        )

        assertTrue(kinds.all(::supportsStockViewport))
    }

    @Test
    fun viewport_clampsAndKeepsMinimumSpan() {
        val endAnchored = normalizeStockViewport(0.95f, 1.2f, 0.16f)
        assertEquals(0.84f, endAnchored.start, 0.0001f)
        assertEquals(1f, endAnchored.endInclusive, 0.0001f)

        val startAnchored = normalizeStockViewport(-0.2f, 0.4f, 0.16f)
        assertEquals(0f, startAnchored.start, 0.0001f)
        assertEquals(0.4f, startAnchored.endInclusive, 0.0001f)
    }

    @Test
    fun visibleRange_mapsSlotsBackToSourceIndices() {
        assertEquals(6..11, visibleStockIndexRange(12, 0.5f, 1f))
        assertEquals(8, sourceIndexForVisibleSlot(slot = 2, count = 12, start = 0.5f, end = 1f))
    }

    @Test
    fun selectionToggle_clearsRepeatedOrMissedSelection() {
        assertEquals(4, toggleAdvancedSelection(current = 2, hit = 4))
        assertEquals(-1, toggleAdvancedSelection(current = 4, hit = 4))
        assertEquals(-1, toggleAdvancedSelection(current = 4, hit = -1))
    }

    @Test
    fun tooltip_usesVariantSpecificSummary() {
        val pointFigure = AdvancedChartSelection(
            kind = AdvancedChartKind.POINT_FIGURE,
            index = 2,
            label = "P3",
            value = 5f,
            summary = "列高:5",
        )
        assertEquals("P3  列高:5", formatStockSelection(pointFigure))

        val price = AdvancedChartSelection(AdvancedChartKind.KAGI, 1, "K2", 7.5f)
        assertEquals("K2  价格:7.5", formatStockSelection(price))
    }

    @Test
    fun advancedStockDefaults_enableBrokerInteractions() {
        val attr = AdvancedChartAttr()
        applyStockInteractionDefaults(attr, AdvancedChartKind.OHLC)

        assertEquals(StockThemePreset.LIGHT, attr.preset)
        assertTrue(attr.interaction.enableLongPressInspect)
        assertTrue(attr.interaction.enableCrosshair)
        assertTrue(attr.interaction.enablePan)
        assertTrue(attr.interaction.enableScale)
        assertTrue(attr.interaction.enableReset)
    }
}
