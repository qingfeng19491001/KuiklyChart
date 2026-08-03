package com.tencent.kuiklybase.chart.core

import com.tencent.kuiklybase.chart.core.cartesian.PlotRect
import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.model.ChartSeries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LineRenderingTest {

    @Test
    fun annotationTextPosition_keepsGlyphBoundsInsidePlot() {
        val position = resolveAnnotationTextPosition(
            desiredX = 230f,
            desiredBaselineY = 5f,
            textWidth = 60f,
            textAscent = 11f,
            textDescent = 3f,
            plot = PlotRect(40f, 10f, 240f, 210f),
            padding = 2f,
        )

        assertEquals(178f, position.first)
        assertEquals(23f, position.second)
    }

    @Test
    fun containsNonFinitePoint_detectsMissingCoordinates() {
        val finite = ChartSeries(
            name = "finite",
            points = listOf(ChartDataPoint("", 1f, 2f)),
            color = 0xFF000000,
        )
        val missingY = ChartSeries(
            name = "missing",
            points = listOf(ChartDataPoint("", 2f, Float.NaN)),
            color = 0xFF000000,
        )

        assertFalse(ChartCanvasRenderer.containsNonFinitePoint(listOf(finite)))
        assertTrue(ChartCanvasRenderer.containsNonFinitePoint(listOf(finite, missingY)))
    }

    @Test
    fun lineSegments_connectsAcrossMissingPointsWhenEnabled() {
        val segments = ChartCanvasRenderer.lineSegments(
            listOf(1f to 10f, null, null, 4f to 40f),
            connectNulls = true,
        )

        assertEquals(listOf(listOf(1f to 10f, 4f to 40f)), segments)
    }

    @Test
    fun lineSegments_breaksAtMissingPointsWhenDisabled() {
        val segments = ChartCanvasRenderer.lineSegments(
            listOf(null, 1f to 10f, 2f to 20f, null, null, 5f to 50f, null),
            connectNulls = false,
        )

        assertEquals(
            listOf(
                listOf(1f to 10f, 2f to 20f),
                listOf(5f to 50f),
            ),
            segments,
        )
    }
}
