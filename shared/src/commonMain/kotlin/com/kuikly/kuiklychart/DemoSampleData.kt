package com.kuikly.kuiklychart

import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.model.ChartSeries
import com.tencent.kuiklybase.chart.model.ChartSlice
import com.tencent.kuiklybase.chart.model.OhlcPoint
import com.tencent.kuiklybase.chart.model.RadarSeries

internal object DemoSampleData {
    fun lineSeries(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "订单金额",
            color = 0xFF1677FF,
            points = listOf(
                ChartDataPoint("1月", 1f, 148f),
                ChartDataPoint("2月", 2f, 172f),
                ChartDataPoint("3月", 3f, 159f),
                ChartDataPoint("4月", 4f, 196f),
                ChartDataPoint("5月", 5f, 188f),
                ChartDataPoint("6月", 6f, 241f),
                ChartDataPoint("7月", 7f, 226f),
            ),
        ),
        ChartSeries(
            name = "复购金额",
            color = 0xFF52C41A,
            points = listOf(
                ChartDataPoint("1月", 1f, 54f),
                ChartDataPoint("2月", 2f, 63f),
                ChartDataPoint("3月", 3f, 58f),
                ChartDataPoint("4月", 4f, 77f),
                ChartDataPoint("5月", 5f, 69f),
                ChartDataPoint("6月", 6f, 92f),
                ChartDataPoint("7月", 7f, 87f),
            ),
        ),
    )

    fun barSeries(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "直营网店",
            color = 0xFF1677FF,
            points = listOf(
                ChartDataPoint("周一", 0f, 136f),
                ChartDataPoint("周二", 1f, 184f),
                ChartDataPoint("周三", 2f, 161f),
                ChartDataPoint("周四", 3f, 208f),
                ChartDataPoint("周五", 4f, 193f),
            ),
        ),
        ChartSeries(
            name = "合作门店",
            color = 0xFF69B1FF,
            points = listOf(
                ChartDataPoint("周一", 0f, 98f),
                ChartDataPoint("周二", 1f, 121f),
                ChartDataPoint("周三", 2f, 117f),
                ChartDataPoint("周四", 3f, 146f),
                ChartDataPoint("周五", 4f, 139f),
            ),
        ),
    )

    fun scatterSeries(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "门店样本",
            color = 0xFF722ED1,
            points = (0 until 30).map { i ->
                val x = i * 3f + 5f
                val y = (i * 11 % 29) * 3f + 18f
                ChartDataPoint("", x, y)
            },
        ),
    )

    fun stackedBarSeries(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "自然流量",
            color = 0xFF1677FF,
            points = listOf(
                ChartDataPoint("Q1", 0f, 142f),
                ChartDataPoint("Q2", 1f, 156f),
                ChartDataPoint("Q3", 2f, 171f),
                ChartDataPoint("Q4", 3f, 188f),
            ),
        ),
        ChartSeries(
            name = "内容推荐",
            color = 0xFF52C41A,
            points = listOf(
                ChartDataPoint("Q1", 0f, 73f),
                ChartDataPoint("Q2", 1f, 91f),
                ChartDataPoint("Q3", 2f, 86f),
                ChartDataPoint("Q4", 3f, 104f),
            ),
        ),
        ChartSeries(
            name = "活动引流",
            color = 0xFFFFA940,
            points = listOf(
                ChartDataPoint("Q1", 0f, 38f),
                ChartDataPoint("Q2", 1f, 47f),
                ChartDataPoint("Q3", 2f, 62f),
                ChartDataPoint("Q4", 3f, 58f),
            ),
        ),
    )

    fun horizontalBarSeries(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "完成度",
            color = 0xFF1677FF,
            points = listOf(
                ChartDataPoint("需求确认", 0f, 92f),
                ChartDataPoint("视觉设计", 1f, 78f),
                ChartDataPoint("功能开发", 2f, 67f),
                ChartDataPoint("灰度验证", 3f, 54f),
            ),
        ),
    )

    fun stockSeries(period: StockPeriod = StockPeriod.DAY): List<OhlcPoint> {
        val scale = when (period) {
            StockPeriod.MIN_1 -> 0.18f
            StockPeriod.MIN_5 -> 0.26f
            StockPeriod.MIN_15 -> 0.34f
            StockPeriod.MIN_30 -> 0.42f
            StockPeriod.MIN_60 -> 0.52f
            StockPeriod.MIN_120 -> 0.64f
            StockPeriod.DAY -> 0.9f
            StockPeriod.WEEK -> 1.4f
            StockPeriod.MONTH -> 2.1f
            StockPeriod.QUARTER -> 3.2f
            StockPeriod.YEAR -> 4.6f
        }
        var previousClose = 102f + period.ordinal * 1.7f
        return (0 until 48).map { index ->
            val direction = ((index * 7 + period.ordinal * 3) % 9 - 4) * scale
            val open = previousClose + ((index % 3) - 1) * scale * 0.35f
            val close = open + direction
            val wick = (1f + index % 4) * scale * 0.55f
            val point = OhlcPoint(
                label = "${period.label}${index + 1}",
                x = index.toFloat(),
                open = open,
                high = maxOf(open, close) + wick,
                low = minOf(open, close) - wick * 0.8f,
                close = close,
                volume = 80_000f + ((index * 37 + period.ordinal * 113) % 160) * 1_250f,
            )
            previousClose = close
            point
        }
    }

    fun pieSlices(): List<ChartSlice> = listOf(
        ChartSlice("移动端", 486f, 0xFF1677FF),
        ChartSlice("桌面端", 274f, 0xFF69B1FF),
        ChartSlice("小程序", 193f, 0xFF36CFC9),
        ChartSlice("线下终端", 117f, 0xFF9254DE),
    )

    fun radarSeries(): List<RadarSeries> = listOf(
        RadarSeries("本季度", listOf(76f, 84f, 69f, 91f, 73f, 82f), 0xFF1677FF),
        RadarSeries("上季度", listOf(68f, 79f, 74f, 83f, 66f, 77f), 0xFF52C41A),
    )

    /** 类目密集，用于手势缩放/平移演示 */
    fun denseBarSeries(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "访问量",
            color = 0xFF1677FF,
            points = (0 until 24).map { i ->
                ChartDataPoint("D$i", i.toFloat(), (30 + (i * 17 % 80)).toFloat())
            },
        ),
    )
    /** Demo 1 · 基础折线图：方形数据点 + lineWidth=2。 */
    fun lineBasic(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "趋势",
            color = 0xFF1677FF,
            points = listOf(
                ChartDataPoint("周一", 1f, 4.2f),
                ChartDataPoint("周二", 2f, 5.1f),
                ChartDataPoint("周三", 3f, 4.7f),
                ChartDataPoint("周四", 4f, 6.4f),
                ChartDataPoint("周五", 5f, 7.2f),
                ChartDataPoint("周六", 6f, 8.8f),
                ChartDataPoint("周日", 7f, 8.1f),
            ),
        ),
    )

    /** Demo 2 · 平滑折线图：smooth=true + domainMin=0。 */
    fun lineSmooth(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "销量",
            color = 0xFF36CFC9,
            points = listOf(
                ChartDataPoint("08:00", 8f, 12f),
                ChartDataPoint("10:00", 10f, 18f),
                ChartDataPoint("12:00", 12f, 16f),
                ChartDataPoint("14:00", 14f, 24f),
                ChartDataPoint("16:00", 16f, 29f),
                ChartDataPoint("18:00", 18f, 25f),
                ChartDataPoint("20:00", 20f, 31f),
            ),
        ),
    )

    /** Demo 3 · 多系列折线图：3 个分部门数据 + 图例点击隐藏/显示。 */
    fun lineMultiSeries(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "华东",
            color = 0xFF1677FF,
            points = listOf(
                ChartDataPoint("1月", 1f, 146f),
                ChartDataPoint("2月", 2f, 158f),
                ChartDataPoint("3月", 3f, 151f),
                ChartDataPoint("4月", 4f, 179f),
                ChartDataPoint("5月", 5f, 192f),
                ChartDataPoint("6月", 6f, 211f),
            ),
        ),
        ChartSeries(
            name = "华南",
            color = 0xFFFAAD14,
            points = listOf(
                ChartDataPoint("1月", 1f, 112f),
                ChartDataPoint("2月", 2f, 126f),
                ChartDataPoint("3月", 3f, 138f),
                ChartDataPoint("4月", 4f, 131f),
                ChartDataPoint("5月", 5f, 149f),
                ChartDataPoint("6月", 6f, 167f),
            ),
        ),
        ChartSeries(
            name = "华北",
            color = 0xFF13C2C2,
            points = listOf(
                ChartDataPoint("1月", 1f, 83f),
                ChartDataPoint("2月", 2f, 91f),
                ChartDataPoint("3月", 3f, 88f),
                ChartDataPoint("4月", 4f, 103f),
                ChartDataPoint("5月", 5f, 116f),
                ChartDataPoint("6月", 6f, 124f),
            ),
        ),
    )

    /** Demo 4 · 阈值折线图：固定数值区间，含警戒线 + 颜色阈值切换。 */
    fun lineThreshold(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "温度",
            color = 0xFF8C8C8C,
            points = listOf(
                ChartDataPoint("6/1", 0f, 34f),
                ChartDataPoint("6/2", 1f, 46f),
                ChartDataPoint("6/3", 2f, 58f),
                ChartDataPoint("6/4", 3f, 66f),
                ChartDataPoint("6/5", 4f, 72f),
                ChartDataPoint("6/6", 5f, 61f),
                ChartDataPoint("6/7", 6f, 53f),
                ChartDataPoint("6/8", 7f, 41f),
            ),
        ),
    )

    /** Demo 5 · 连接空值：第 3、4 个月数据缺失。 */
    fun lineConnectNulls(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "设备在线率",
            color = 0xFF722ED1,
            points = listOf(
                ChartDataPoint("09:00", 9f, 91f),
                ChartDataPoint("10:00", 10f, 94f),
                ChartDataPoint("11:00", 11f, Float.NaN),
                ChartDataPoint("12:00", 12f, Float.NaN),
                ChartDataPoint("13:00", 13f, 96f),
                ChartDataPoint("14:00", 14f, 95f),
                ChartDataPoint("15:00", 15f, 97f),
                ChartDataPoint("16:00", 16f, 96f),
            ),
        ),
    )

    /** Demo 6 · 添加填充色：折线下方线性渐变区域。 */
    fun lineWithFill(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "访问量",
            color = 0xFF1677FF,
            points = listOf(
                ChartDataPoint("周一", 1f, 32f),
                ChartDataPoint("周二", 2f, 38f),
                ChartDataPoint("周三", 3f, 35f),
                ChartDataPoint("周四", 4f, 47f),
                ChartDataPoint("周五", 5f, 53f),
                ChartDataPoint("周六", 6f, 61f),
                ChartDataPoint("周日", 7f, 57f),
            ),
        ),
    )

    /** Demo 7 · 文本标记：在最高点处添加注释与引线。 */
    fun lineWithAnnotation(): List<ChartSeries> = listOf(
        ChartSeries(
            name = "搜索热度",
            color = 0xFF1677FF,
            points = listOf(
                ChartDataPoint("1月", 1f, 38f),
                ChartDataPoint("2月", 2f, 52f),
                ChartDataPoint("3月", 3f, 61f),
                ChartDataPoint("4月", 4f, 80f),
                ChartDataPoint("5月", 5f, 96f),
                ChartDataPoint("6月", 6f, 70f),
                ChartDataPoint("7月", 7f, 58f),
            ),
        ),
        ChartSeries(
            name = "分享量",
            color = 0xFFFF7A45,
            points = listOf(
                ChartDataPoint("1月", 1f, 22f),
                ChartDataPoint("2月", 2f, 35f),
                ChartDataPoint("3月", 3f, 42f),
                ChartDataPoint("4月", 4f, 60f),
                ChartDataPoint("5月", 5f, 78f),
                ChartDataPoint("6月", 6f, 52f),
                ChartDataPoint("7月", 7f, 40f),
            ),
        ),
    )
}
