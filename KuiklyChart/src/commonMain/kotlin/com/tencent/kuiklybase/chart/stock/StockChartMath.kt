package com.tencent.kuiklybase.chart.stock

import com.tencent.kuiklybase.chart.core.cartesian.PlotRect
import com.tencent.kuiklybase.chart.model.OhlcPoint

internal data class StockPlots(
    val price: PlotRect,
    val volume: PlotRect?,
)

internal fun stockMovingAverage(
    points: List<OhlcPoint>,
    period: Int,
): List<Float?> {
    require(period > 0) { "period must be positive" }
    var sum = 0f
    return points.mapIndexed { index, point ->
        sum += point.close
        if (index >= period) {
            sum -= points[index - period].close
        }
        if (index + 1 >= period) sum / period else null
    }
}

internal fun stockVolumeMovingAverage(
    points: List<OhlcPoint>,
    period: Int,
): List<Float?> {
    require(period > 0) { "period must be positive" }
    val volumes = points.map { it.volume }
    return volumes.indices.map { index ->
        val start = index - period + 1
        if (start < 0) return@map null
        val window = volumes.subList(start, index + 1)
        if (window.any { it == null }) null else window.filterNotNull().average().toFloat()
    }
}

internal fun stockAveragePoints(
    source: List<OhlcPoint>,
    values: List<Float?>,
): List<Pair<Float, Float>> = source.zip(values).mapNotNull { (point, value) ->
    value?.let { point.x to it }
}

internal fun shouldShowVolumePanel(
    points: List<OhlcPoint>,
    configured: Boolean,
): Boolean = configured && points.any { it.volume != null }

internal fun stockVolumeBounds(points: List<OhlcPoint>): ClosedFloatingPointRange<Float> {
    val maximum = points.mapNotNull { it.volume }.maxOrNull()?.coerceAtLeast(0f) ?: 0f
    return 0f..maximum
}

internal fun splitStockPlots(
    plot: PlotRect,
    showVolume: Boolean,
    volumeRatio: Float,
): StockPlots {
    if (!showVolume) return StockPlots(plot, null)

    val gap = 12f
    val usableHeight = (plot.height - gap).coerceAtLeast(0f)
    val volumeHeight = usableHeight * volumeRatio.coerceIn(0.16f, 0.4f)
    val priceBottom = plot.bottom - gap - volumeHeight
    return StockPlots(
        price = plot.copy(bottom = priceBottom),
        volume = PlotRect(
            left = plot.left,
            top = priceBottom + gap,
            right = plot.right,
            bottom = plot.bottom,
        ),
    )
}
