package com.tencent.kuiklybase.chart.core.cartesian

import com.tencent.kuiklybase.chart.model.ChartSeries
import kotlin.math.abs
import kotlin.math.hypot

internal object CartesianHitTester {
    data class HitResult(val seriesIndex: Int, val pointIndex: Int)

    fun nearestPoint(
        series: List<ChartSeries>,
        scale: CartesianScale,
        x: Float,
        y: Float,
        threshold: Float = 16f,
    ): HitResult? {
        var best: HitResult? = null
        var bestDist = threshold
        series.forEachIndexed { sIdx, s ->
            s.points.forEachIndexed { pIdx, p ->
                val px = scale.toPixelX(p.x)
                val py = scale.toPixelY(p.y)
                val dist = hypot(px - x, py - y)
                if (dist <= bestDist) {
                    bestDist = dist
                    best = HitResult(sIdx, pIdx)
                }
            }
        }
        return best
    }

    /** 兼容旧调用：按类目命中，默认首系列。 */
    fun nearestCategory(
        series: List<ChartSeries>,
        scale: CartesianScale,
        x: Float,
        threshold: Float = 24f,
    ): HitResult? {
        if (series.isEmpty()) return null
        val points = series.first().points
        var best: HitResult? = null
        var bestDist = threshold
        points.forEachIndexed { pIdx, p ->
            val px = scale.toPixelX(p.x)
            val dist = abs(px - x)
            if (dist <= bestDist) {
                bestDist = dist
                best = HitResult(0, pIdx)
            }
        }
        return best
    }

    /** 分组柱：按类目 + 纵向距离选择最近柱。 */
    fun nearestBar(
        series: List<ChartSeries>,
        scale: CartesianScale,
        x: Float,
        y: Float,
        threshold: Float = 28f,
    ): HitResult? {
        if (series.isEmpty()) return null
        var best: HitResult? = null
        var bestScore = Float.POSITIVE_INFINITY
        series.forEachIndexed { sIdx, s ->
            s.points.forEachIndexed { pIdx, p ->
                val px = scale.toPixelX(p.x)
                val top = scale.toPixelY(p.y)
                val bottom = scale.toPixelY(0f)
                val midY = (top + bottom) / 2f
                val dx = abs(px - x)
                if (dx > threshold) return@forEachIndexed
                val dy = if (y in minOf(top, bottom)..maxOf(top, bottom)) 0f else abs(midY - y)
                val score = dx + dy * 0.5f
                if (score < bestScore) {
                    bestScore = score
                    best = HitResult(sIdx, pIdx)
                }
            }
        }
        return best
    }

    /** 水平条：类目在 Y（点的 x），数值在 X（点的 y）。 */
    fun nearestHorizontalBar(
        series: List<ChartSeries>,
        scale: CartesianScale,
        x: Float,
        y: Float,
        threshold: Float = 28f,
    ): HitResult? {
        if (series.isEmpty()) return null
        var best: HitResult? = null
        var bestScore = Float.POSITIVE_INFINITY
        series.forEachIndexed { sIdx, s ->
            s.points.forEachIndexed { pIdx, p ->
                val py = scale.toPixelY(p.x)
                val left = scale.toPixelX(0f)
                val right = scale.toPixelX(p.y)
                val midX = (left + right) / 2f
                val dy = abs(py - y)
                if (dy > threshold) return@forEachIndexed
                val dx = if (x in minOf(left, right)..maxOf(left, right)) 0f else abs(midX - x)
                val score = dy + dx * 0.5f
                if (score < bestScore) {
                    bestScore = score
                    best = HitResult(sIdx, pIdx)
                }
            }
        }
        return best
    }
}
