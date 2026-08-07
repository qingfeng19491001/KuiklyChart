package com.tencent.kuiklybase.chart.stock

import com.tencent.kuiklybase.chart.core.cartesian.PlotRect
import com.tencent.kuiklybase.chart.model.OhlcPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StockChartMathTest {
    private val data = listOf(
        OhlcPoint("D1", 0f, 10f, 12f, 9f, 10f, 100f),
        OhlcPoint("D2", 1f, 10f, 14f, 10f, 12f, 200f),
        OhlcPoint("D3", 2f, 12f, 15f, 11f, 14f, 300f),
    )

    @Test
    fun movingAverage_startsWhenWindowIsFull() {
        assertEquals(listOf(null, 11f, 13f), stockMovingAverage(data, 2))
    }

    @Test
    fun volumeBounds_ignoreMissingVolume() {
        assertEquals(0f..300f, stockVolumeBounds(data))
        assertEquals(0f..200f, stockVolumeBounds(data.mapIndexed { index, point ->
            if (index == 2) point.copy(volume = null) else point
        }))
    }

    @Test
    fun splitPlots_hideVolumeWhenDisabled() {
        val plot = PlotRect(40f, 8f, 300f, 220f)
        val split = splitStockPlots(plot, showVolume = false, volumeRatio = 0.24f)

        assertEquals(plot, split.price)
        assertNull(split.volume)
    }

    @Test
    fun changedStockData_resetsInspectionState() {
        val next = data.mapIndexed { index, point -> point.copy(label = "N$index") }

        assertFalse(stockDataChanged(data, data.toList()))
        assertTrue(stockDataChanged(data, next))
    }

    @Test
    fun splitPlots_reserveGapAndVolumeHeight() {
        val split = splitStockPlots(
            PlotRect(40f, 8f, 300f, 220f),
            showVolume = true,
            volumeRatio = 0.25f,
        )

        assertEquals(158f, split.price.bottom)
        assertEquals(170f, split.volume?.top)
        assertEquals(220f, split.volume?.bottom)
    }

    @Test
    fun averagePoints_keepOriginalXCoordinates() {
        val averages = stockMovingAverage(data, 2)

        assertEquals(listOf(1f to 11f, 2f to 13f), stockAveragePoints(data, averages))
    }

    @Test
    fun volumeMovingAverage_usesOnlyCompleteWindows() {
        assertEquals(listOf(null, 150f, 250f), stockVolumeMovingAverage(data, 2))
    }

    @Test
    fun volumePanel_requiresConfiguredVolumeData() {
        assertTrue(shouldShowVolumePanel(data, configured = true))
        assertFalse(shouldShowVolumePanel(data, configured = false))
        assertFalse(shouldShowVolumePanel(data.map { it.copy(volume = null) }, configured = true))
    }
}
