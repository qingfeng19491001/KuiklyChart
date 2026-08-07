package com.tencent.kuiklybase.chart.model

/** 股票 / K 线数据点（开高低收）。 */
data class OhlcPoint(
    val label: String,
    val x: Float,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float? = null,
)
