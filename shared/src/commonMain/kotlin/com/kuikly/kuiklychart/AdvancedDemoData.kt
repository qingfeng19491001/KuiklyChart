package com.kuikly.kuiklychart

import com.tencent.kuiklybase.chart.advanced.BulletChartItem
import com.tencent.kuiklybase.chart.advanced.DualAxisPoint
import com.tencent.kuiklybase.chart.advanced.HistogramBin
import com.tencent.kuiklybase.chart.advanced.NestedPieRing
import com.tencent.kuiklybase.chart.advanced.NestedPieSlice
import com.tencent.kuiklybase.chart.advanced.PointFigureColumn
import com.tencent.kuiklybase.chart.advanced.SunburstNode
import com.tencent.kuiklybase.chart.advanced.WaterfallPoint
import com.tencent.kuiklybase.chart.model.ChartDataPoint
import com.tencent.kuiklybase.chart.model.ChartSlice

internal object AdvancedDemoData {
    fun dualAxis(): List<DualAxisPoint> = listOf(
        DualAxisPoint("Jan", 72f, 18f), DualAxisPoint("Feb", 91f, 24f),
        DualAxisPoint("Mar", 84f, 21f), DualAxisPoint("Apr", 108f, 31f),
        DualAxisPoint("May", 116f, 35f), DualAxisPoint("Jun", 132f, 42f),
    )

    fun waterfall(): List<WaterfallPoint> = listOf(
        WaterfallPoint("Start", 70f), WaterfallPoint("Sales", 24f),
        WaterfallPoint("Cost", -18f), WaterfallPoint("Service", 31f),
        WaterfallPoint("Tax", -12f), WaterfallPoint("Total", 95f, isTotal = true),
    )

    fun histogram(): List<HistogramBin> =
        listOf(10f, 22f, 41f, 68f, 91f, 76f, 52f, 31f, 17f, 8f).mapIndexed { index, value ->
            HistogramBin("${index * 10}-${(index + 1) * 10}", value)
        }

    fun bullets(): List<BulletChartItem> = listOf(
        BulletChartItem("Revenue", 0.78f, 0.86f),
        BulletChartItem("Profit", 0.62f, 0.72f),
        BulletChartItem("Orders", 0.88f, 0.81f),
    )

    fun halfDonut(): List<ChartSlice> = listOf(
        ChartSlice("Mobile", 38f, 0xFF1677FF), ChartSlice("Desktop", 27f, 0xFF36CFC9),
        ChartSlice("Mini App", 20f, 0xFF9254DE), ChartSlice("Offline", 15f, 0xFFFFA940),
    )

    fun rose(): List<ChartSlice> = listOf(55f, 82f, 66f, 96f, 72f, 48f, 88f, 62f)
        .mapIndexed { index, value ->
            ChartSlice("Region ${index + 1}", value, COLORS[index % COLORS.size])
        }

    fun sunburst(): List<SunburstNode> = listOf(
        SunburstNode("All", 100f, 0xFF1677FF, 0),
        SunburstNode("East", 38f, 0xFF36CFC9, 1),
        SunburstNode("South", 34f, 0xFF9254DE, 1),
        SunburstNode("North", 28f, 0xFFFFA940, 1),
        SunburstNode("Shanghai", 22f, 0xFF69B1FF, 2),
        SunburstNode("Hangzhou", 16f, 0xFF5CDBD3, 2),
        SunburstNode("Shenzhen", 19f, 0xFFB37FEB, 2),
        SunburstNode("Guangzhou", 15f, 0xFFFFBB96, 2),
        SunburstNode("Beijing", 18f, 0xFF95DE64, 2),
        SunburstNode("Chengdu", 10f, 0xFFFFD666, 2),
    )

    fun nestedPie(): List<NestedPieSlice> = listOf(
        NestedPieSlice("Organic", 46f, 0xFF1677FF, NestedPieRing.INNER),
        NestedPieSlice("Campaign", 31f, 0xFF36CFC9, NestedPieRing.INNER),
        NestedPieSlice("Paid", 23f, 0xFF9254DE, NestedPieRing.INNER),
        NestedPieSlice("Search", 18f, 0xFF69B1FF, NestedPieRing.OUTER),
        NestedPieSlice("Recommend", 28f, 0xFF0958D9, NestedPieRing.OUTER),
        NestedPieSlice("Event", 12f, 0xFF5CDBD3, NestedPieRing.OUTER),
        NestedPieSlice("Share", 19f, 0xFF08979C, NestedPieRing.OUTER),
        NestedPieSlice("Feed", 11f, 0xFFB37FEB, NestedPieRing.OUTER),
        NestedPieSlice("Ads", 12f, 0xFF531DAB, NestedPieRing.OUTER),
    )

    fun stockPoints(): List<ChartDataPoint> = listOf(102f, 106f, 104f, 111f, 109f, 114f, 118f, 115f, 111f, 120f, 123f, 119f)
        .mapIndexed { index, value -> ChartDataPoint("D${index + 1}", index.toFloat(), value) }

    fun renko(): List<ChartDataPoint> = listOf(2f, 3f, 4f, 5f, 4f, 5f, 6f, 7f, 6f, 5f, 6f)
        .mapIndexed { index, value -> ChartDataPoint("R${index + 1}", index.toFloat(), value) }

    fun kagi(): List<ChartDataPoint> = listOf(2f, 5f, 3f, 7f, 4f, 8f, 6f, 9f, 5f, 7f, 4f, 8f)
        .mapIndexed { index, value -> ChartDataPoint("K${index + 1}", index.toFloat(), value) }

    fun pointFigure(): List<PointFigureColumn> = listOf(3, -4, 5, -3, 6, -5, 4, -3, 5)
        .mapIndexed { index, value -> PointFigureColumn("P${index + 1}", kotlin.math.abs(value), value > 0) }

    private val COLORS = listOf(0xFF1677FF, 0xFF36CFC9, 0xFF52C41A, 0xFFFFA940, 0xFF9254DE, 0xFFFF4D4F)
}
