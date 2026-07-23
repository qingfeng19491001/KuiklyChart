package com.kuikly.kuiklychart

import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.model.ChartSeries
import com.tencent.kuiklybase.chart.model.ChartSlice
import com.tencent.kuiklybase.chart.model.OhlcPoint
import com.tencent.kuiklybase.chart.model.RadarSeries

internal object DemoSampleData {
    fun lineSeries(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "销售额",
            color = 0xFF4F8FFF,
            points = listOf(
                ChartDataPoint("1月", 1f, 120f),
                ChartDataPoint("2月", 2f, 132f),
                ChartDataPoint("3月", 3f, 101f),
                ChartDataPoint("4月", 4f, 134f),
                ChartDataPoint("5月", 5f, 90f),
                ChartDataPoint("6月", 6f, 230f),
                ChartDataPoint("7月", 7f, 210f),
            ),
        ),
        ChartSeries(
            name = "利润",
            color = 0xFF52C41A,
            points = listOf(
                ChartDataPoint("1月", 1f, 40f),
                ChartDataPoint("2月", 2f, 52f),
                ChartDataPoint("3月", 3f, 35f),
                ChartDataPoint("4月", 4f, 48f),
                ChartDataPoint("5月", 5f, 30f),
                ChartDataPoint("6月", 6f, 80f),
                ChartDataPoint("7月", 7f, 72f),
            ),
        ),
    )

    fun barSeries(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "产品A",
            color = 0xFF4F8FFF,
            points = listOf(
                ChartDataPoint("周一", 0f, 120f, color = 0xFF4F8FFF),
                ChartDataPoint("周二", 1f, 200f, color = 0xFF69C0FF),
                ChartDataPoint("周三", 2f, 150f, color = 0xFF36CFC9),
                ChartDataPoint("周四", 3f, 80f, color = 0xFF9254DE),
                ChartDataPoint("周五", 4f, 70f, color = 0xFFFF85C0),
            ),
        ),
        ChartSeries(
            name = "产品B",
            color = 0xFFFF7875,
            points = listOf(
                ChartDataPoint("周一", 0f, 90f),
                ChartDataPoint("周二", 1f, 160f),
                ChartDataPoint("周三", 2f, 110f),
                ChartDataPoint("周四", 3f, 60f),
                ChartDataPoint("周五", 4f, 50f),
            ),
        ),
    )

    fun scatterSeries(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "样本",
            color = 0xFF9254DE,
            points = (0 until 30).map { i ->
                val x = i * 3f + 5f
                val y = (i * 7 % 23) * 4f + 10f
                ChartDataPoint("", x, y)
            },
        ),
    )

    fun stackedBarSeries(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "直接",
            color = 0xFF4F8FFF,
            points = listOf(
                ChartDataPoint("Q1", 0f, 120f),
                ChartDataPoint("Q2", 1f, 132f),
                ChartDataPoint("Q3", 2f, 101f),
                ChartDataPoint("Q4", 3f, 134f),
            ),
        ),
        ChartSeries(
            name = "邮件",
            color = 0xFF52C41A,
            points = listOf(
                ChartDataPoint("Q1", 0f, 60f),
                ChartDataPoint("Q2", 1f, 72f),
                ChartDataPoint("Q3", 2f, 51f),
                ChartDataPoint("Q4", 3f, 84f),
            ),
        ),
        ChartSeries(
            name = "联盟",
            color = 0xFFFFA940,
            points = listOf(
                ChartDataPoint("Q1", 0f, 40f),
                ChartDataPoint("Q2", 1f, 52f),
                ChartDataPoint("Q3", 2f, 31f),
                ChartDataPoint("Q4", 3f, 64f),
            ),
        ),
    )

    fun horizontalBarSeries(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "完成度",
            color = 0xFF4F8FFF,
            points = listOf(
                ChartDataPoint("设计", 0f, 86f, color = 0xFF4F8FFF),
                ChartDataPoint("开发", 1f, 72f, color = 0xFF52C41A),
                ChartDataPoint("测试", 2f, 64f, color = 0xFFFFA940),
                ChartDataPoint("上线", 3f, 91f, color = 0xFFFF7875),
            ),
        ),
    )

    fun stockSeries(): List<OhlcPoint> = listOf(
        OhlcPoint("D1", 0f, 102f, 108f, 100f, 106f),
        OhlcPoint("D2", 1f, 106f, 110f, 103f, 104f),
        OhlcPoint("D3", 2f, 104f, 107f, 99f, 101f),
        OhlcPoint("D4", 3f, 101f, 112f, 100f, 111f),
        OhlcPoint("D5", 4f, 111f, 115f, 108f, 109f),
        OhlcPoint("D6", 5f, 109f, 113f, 105f, 112f),
        OhlcPoint("D7", 6f, 112f, 118f, 110f, 116f),
        OhlcPoint("D8", 7f, 116f, 120f, 112f, 114f),
        OhlcPoint("D9", 8f, 114f, 117f, 108f, 110f),
        OhlcPoint("D10", 9f, 110f, 119f, 109f, 118f),
    )

    fun pieSlices(): List<ChartSlice> = listOf(
        ChartSlice("直接访问", 335f, 0xFF4F8FFF),
        ChartSlice("邮件营销", 310f, 0xFF52C41A),
        ChartSlice("联盟广告", 234f, 0xFFFFA940),
        ChartSlice("视频广告", 135f, 0xFFFF7875),
        ChartSlice("搜索引擎", 1548f, 0xFF9254DE),
    )

    fun radarSeries(): List<RadarSeries> = listOf(
        RadarSeries("预算", listOf(80f, 90f, 70f, 85f, 75f, 88f), 0xFF4F8FFF),
        RadarSeries("实际", listOf(65f, 82f, 60f, 78f, 68f, 80f), 0xFF52C41A),
    )

    /** 类目密集，用于手势缩放/平移演示 */
    fun denseBarSeries(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "访问量",
            color = 0xFF4F8FFF,
            points = (0 until 24).map { i ->
                ChartDataPoint("D$i", i.toFloat(), (30 + (i * 17 % 80)).toFloat())
            },
        ),
    )
}
