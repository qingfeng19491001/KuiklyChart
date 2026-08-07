package com.kuikly.kuiklychart

import kotlin.test.Test
import kotlin.test.assertEquals

class DemoLayoutTest {
    @Test
    fun responsiveChartHeight_usesWidthBasedRatioOnTallScreens() {
        assertEquals(324f, responsiveChartHeight(360f, 780f, 24f))
    }

    @Test
    fun responsiveChartHeight_isCappedByAvailableHeightOnShortScreens() {
        assertEquals(220f, responsiveChartHeight(720f, 300f, 24f))
    }

    @Test
    fun stockPeriods_exposeTabsAndMoreMenu() {
        assertEquals(listOf("日K", "周K", "月K"), stockPrimaryPeriods().map { it.label })
        assertEquals(
            listOf("1分", "5分", "15分", "30分", "60分", "120分", "季K", "年K"),
            stockMorePeriods().map { it.label },
        )
    }

    @Test
    fun professionalCard_isTallerForVolumePanel() {
        assertEquals(356f, stockVariantCardHeight(professional = true))
        assertEquals(236f, stockVariantCardHeight(professional = false))
    }
}
