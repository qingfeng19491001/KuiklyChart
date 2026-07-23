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
}
