# Stock Chart Variants Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade all eight stock-chart variants with consistent broker-style interaction and themes, while adding multi-period MA and optional volume panels to the professional K-line variant.

**Architecture:** Keep `StockChartView` on `CartesianInteractiveView` and keep the seven advanced stock variants on `AdvancedChartView`, but share pure stock math, normalized viewport rules, theme presets, selection semantics, and interaction defaults. The Demo owns period tabs/dropdowns and swaps providers; public chart components only render the data and configuration they receive.

**Tech Stack:** Kotlin Multiplatform, Kuikly UI DSL, Kuikly Canvas, kotlin.test, Gradle KMP.

---

## File map

- Create `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/stock/StockChartMath.kt`: moving averages, volume range, split-plot geometry, normalized stock viewport helpers.
- Create `KuiklyChart/src/commonTest/kotlin/com/tencent/kuiklybase/chart/stock/StockChartMathTest.kt`: pure calculation coverage.
- Modify `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/model/OhlcPoint.kt`: optional volume.
- Modify `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/config/ChartConfigs.kt`: stock theme preset and shared interaction flags.
- Modify `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/config/ChartComponentContracts.kt`: MA/volume configuration and preset resolution.
- Modify `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/core/ChartCanvasRenderer.kt`: reusable stock lines, volume bars, labels and guides.
- Modify `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/core/cartesian/CartesianInteractiveView.kt`: long-press inspect entry for K-line/candlestick charts.
- Modify `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/stock/StockChartView.kt`: professional K-line rendering and tooltip.
- Modify `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/advanced/AdvancedChartContracts.kt`: common selection and interaction contract.
- Modify `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/advanced/AdvancedChartView.kt`: all seven stock variants use the shared viewport, inspect, crosshair and theme behavior.
- Create `KuiklyChart/src/commonTest/kotlin/com/tencent/kuiklybase/chart/advanced/StockVariantInteractionTest.kt`: viewport, hit-test and tooltip coverage for all advanced stock kinds.
- Modify `shared/src/commonMain/kotlin/com/kuikly/kuiklychart/DemoSampleData.kt`: OHLCV period data.
- Modify `shared/src/commonMain/kotlin/com/kuikly/kuiklychart/AdvancedDemoData.kt`: denser stock-variant data.
- Modify `shared/src/commonMain/kotlin/com/kuikly/kuiklychart/StockChartDemoPage.kt`: tabs, dropdown, theme switch and unified interaction configuration.
- Modify `shared/src/commonMain/kotlin/com/kuikly/kuiklychart/DemoGallery.kt`: optional taller professional card.
- Modify `shared/src/commonTest/kotlin/com/kuikly/kuiklychart/DemoLayoutTest.kt`: period menu and card layout helpers.
- Modify `README.md` and `CHANGELOG.md`: document the public capabilities.

### Task 1: Add OHLCV data and pure stock calculations

**Files:**
- Modify: `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/model/OhlcPoint.kt`
- Create: `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/stock/StockChartMath.kt`
- Create: `KuiklyChart/src/commonTest/kotlin/com/tencent/kuiklybase/chart/stock/StockChartMathTest.kt`

- [ ] **Step 1: Write failing calculation tests**

```kotlin
class StockChartMathTest {
    private val data = listOf(
        OhlcPoint("D1", 0f, 10f, 12f, 9f, 10f, 100f),
        OhlcPoint("D2", 1f, 10f, 14f, 10f, 12f, 200f),
        OhlcPoint("D3", 2f, 12f, 15f, 11f, 14f, 300f),
    )

    @Test fun movingAverage_startsWhenWindowIsFull() {
        assertEquals(listOf(null, 11f, 13f), stockMovingAverage(data, 2))
    }

    @Test fun volumeBounds_ignoreMissingVolume() {
        assertEquals(0f..300f, stockVolumeBounds(data))
    }

    @Test fun splitPlots_hideVolumeWhenDisabled() {
        val split = splitStockPlots(PlotRect(40f, 8f, 300f, 220f), false, 0.24f)
        assertEquals(null, split.volume)
        assertEquals(220f, split.price.bottom)
    }
}
```

- [ ] **Step 2: Run the new test and verify failure**

Run: `.\gradlew.bat :KuiklyChart:allTests`

Expected: FAIL because `stockMovingAverage`, `stockVolumeBounds`, and `splitStockPlots` do not exist and `OhlcPoint` has no `volume` parameter.

- [ ] **Step 3: Add the compatible OHLCV field and minimal math**

```kotlin
data class OhlcPoint(
    val label: String,
    val x: Float,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float? = null,
)

internal data class StockPlots(val price: PlotRect, val volume: PlotRect?)

internal fun stockMovingAverage(points: List<OhlcPoint>, period: Int): List<Float?> {
    require(period > 0)
    var sum = 0f
    return points.mapIndexed { index, point ->
        sum += point.close
        if (index >= period) sum -= points[index - period].close
        if (index + 1 >= period) sum / period else null
    }
}

internal fun stockVolumeBounds(points: List<OhlcPoint>): ClosedFloatingPointRange<Float> {
    val max = points.mapNotNull { it.volume }.maxOrNull()?.coerceAtLeast(0f) ?: 0f
    return 0f..max
}

internal fun splitStockPlots(plot: PlotRect, showVolume: Boolean, ratio: Float): StockPlots {
    if (!showVolume) return StockPlots(plot, null)
    val safeRatio = ratio.coerceIn(0.16f, 0.4f)
    val gap = 12f
    val volumeHeight = (plot.height - gap) * safeRatio
    val divider = plot.bottom - volumeHeight - gap
    return StockPlots(
        price = plot.copy(bottom = divider),
        volume = PlotRect(plot.left, divider + gap, plot.right, plot.bottom),
    )
}
```

- [ ] **Step 4: Run the focused test and verify pass**

Run: `.\gradlew.bat :KuiklyChart:allTests`

Expected: PASS.

- [ ] **Step 5: Commit the data/math seam**

```powershell
git add KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/model/OhlcPoint.kt KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/stock/StockChartMath.kt KuiklyChart/src/commonTest/kotlin/com/tencent/kuiklybase/chart/stock/StockChartMathTest.kt
git commit -m "feat(stock): add OHLCV calculations"
```

### Task 2: Define stock themes and K-line indicator configuration

**Files:**
- Modify: `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/config/ChartConfigs.kt`
- Modify: `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/config/ChartComponentContracts.kt`
- Modify: `KuiklyChart/src/commonTest/kotlin/com/tencent/kuiklybase/chart/config/ChartComponentContractsTest.kt`

- [ ] **Step 1: Write failing configuration tests**

```kotlin
@Test fun stockDefaults_remainPureKline() {
    val attr = StockChartAttr()
    assertFalse(attr.movingAverages.show)
    assertFalse(attr.volumePanel.show)
    assertEquals(StockThemePreset.LIGHT, attr.preset)
}

@Test fun darkStockPreset_resolvesBrokerPalette() {
    val theme = resolveStockTheme(ChartThemeOptions(), StockThemePreset.DARK)
    assertEquals(0xFF101620, theme.backgroundColor)
    assertEquals(0xFFE15A5A, theme.upColor)
    assertEquals(0xFF28AD78, theme.downColor)
}
```

- [ ] **Step 2: Run and verify failure**

Run: `.\gradlew.bat :KuiklyChart:allTests`

Expected: FAIL with unresolved stock configuration symbols.

- [ ] **Step 3: Add exact public configuration types**

```kotlin
enum class StockThemePreset { LIGHT, DARK }

data class StockAverageLine(
    val period: Int,
    val color: Long,
    val label: String = "MA$period",
)

class StockMovingAverageConfig {
    var show: Boolean = false
    val lines = mutableListOf<StockAverageLine>()
    fun line(period: Int, color: Long, label: String = "MA$period") {
        require(period > 0)
        lines += StockAverageLine(period, color, label)
    }
}

class StockVolumePanelConfig {
    var show: Boolean = false
    var heightRatio: Float = 0.24f
    val averageLines = mutableListOf<StockAverageLine>()
    fun average(period: Int, color: Long, label: String = "VMA$period") {
        require(period > 0)
        averageLines += StockAverageLine(period, color, label)
    }
}

class StockChartAttr : CartesianChartAttr() {
    var candleWidthRatio: Float = 0.6f
    var preset: StockThemePreset = StockThemePreset.LIGHT
    val movingAverages = StockMovingAverageConfig()
    val volumePanel = StockVolumePanelConfig()
    fun movingAverages(block: StockMovingAverageConfig.() -> Unit) = movingAverages.apply(block)
    fun volumePanel(block: StockVolumePanelConfig.() -> Unit) = volumePanel.apply(block)
    // Preserve the existing interaction defaults in init.
}
```

Implement `resolveStockTheme` with fixed LIGHT and DARK palettes, then overlay explicit `ChartThemeOptions` values only when callers changed them from their defaults.

Add `enableLongPressInspect: Boolean = false` to `ChartInteractionConfig`; `StockChartAttr.init` sets it to `true`, so non-stock Cartesian charts retain their existing behavior.

- [ ] **Step 4: Run configuration tests**

Run: `.\gradlew.bat :KuiklyChart:allTests`

Expected: PASS.

- [ ] **Step 5: Commit public contracts**

```powershell
git add KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/config KuiklyChart/src/commonTest/kotlin/com/tencent/kuiklybase/chart/config/ChartComponentContractsTest.kt
git commit -m "feat(stock): add indicator and theme contracts"
```

### Task 3: Render professional K-line, MA and optional volume panel

**Files:**
- Modify: `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/core/ChartCanvasRenderer.kt`
- Modify: `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/core/cartesian/CartesianInteractiveView.kt`
- Modify: `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/stock/StockChartView.kt`
- Extend test: `KuiklyChart/src/commonTest/kotlin/com/tencent/kuiklybase/chart/stock/StockChartMathTest.kt`

- [ ] **Step 1: Add failing visible-window tests**

```kotlin
@Test fun visibleAveragePoints_keepOriginalXCoordinates() {
    val averages = stockMovingAverage(data, 2)
    assertEquals(listOf(1f to 11f, 2f to 13f), stockAveragePoints(data, averages))
}

@Test fun volumePanel_isSuppressedWhenEveryVolumeIsNull() {
    val noVolume = data.map { it.copy(volume = null) }
    assertFalse(shouldShowVolumePanel(noVolume, configured = true))
}
```

- [ ] **Step 2: Run and verify failure**

Run: `.\gradlew.bat :KuiklyChart:allTests`

Expected: FAIL because the visible-average helpers are absent.

- [ ] **Step 3: Implement helpers and renderer entry points**

Add pure helpers:

```kotlin
internal fun stockAveragePoints(
    source: List<OhlcPoint>,
    values: List<Float?>,
): List<Pair<Float, Float>> = source.zip(values).mapNotNull { (point, value) ->
    value?.let { point.x to it }
}

internal fun shouldShowVolumePanel(points: List<OhlcPoint>, configured: Boolean): Boolean =
    configured && points.any { it.volume != null }
```

Add renderer functions with no internal state:

```kotlin
fun drawStockAverageLine(
    ctx: ContextApi,
    plot: PlotRect,
    viewport: ChartViewport,
    points: List<Pair<Float, Float>>,
    color: Long,
)

fun drawStockVolumes(
    ctx: ContextApi,
    plot: PlotRect,
    xViewport: ChartViewport,
    points: List<OhlcPoint>,
    maxVolume: Float,
    candleWidthRatio: Float,
    theme: ChartTheme,
)
```

Implement both using `withPlotClip`, `CartesianScale`, the same X coordinate as each candle, and up/down color chosen from `close >= open`.

- [ ] **Step 4: Update `StockChartView.drawPlot`**

Resolve the stock theme, split the plot, draw price grid/axis, candles and configured MA lines in `plots.price`; draw volume grid, bars and configured volume averages in `plots.volume`. Override `selectionCrosshair` so its Y coordinate uses the selected close in the price plot.

Format Tooltip as:

```kotlin
buildString {
    append(candle.label.ifEmpty { candle.x.toString() })
    append("  O:${candle.open} H:${candle.high} L:${candle.low} C:${candle.close}")
    candle.volume?.let { append("  VOL:$it") }
}
```

In `CartesianInteractiveView`, route long-press start to `onPlotClick(params.x, params.y)` when `enableLongPressInspect` is true. Keep the existing brush-selection branch when `enableDragSelect` is true, with brush selection taking precedence if both flags are enabled.

- [ ] **Step 5: Run focused and existing cartesian tests**

Run: `.\gradlew.bat :KuiklyChart:allTests`

Expected: PASS.

- [ ] **Step 6: Commit professional K-line rendering**

```powershell
git add KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/core/ChartCanvasRenderer.kt KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/core/cartesian/CartesianInteractiveView.kt KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/stock/StockChartView.kt KuiklyChart/src/commonTest/kotlin/com/tencent/kuiklybase/chart/stock/StockChartMathTest.kt
git commit -m "feat(stock): render MA and volume panels"
```

### Task 4: Extract normalized interaction rules for seven advanced stock variants

**Files:**
- Modify: `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/advanced/AdvancedChartContracts.kt`
- Create: `KuiklyChart/src/commonTest/kotlin/com/tencent/kuiklybase/chart/advanced/StockVariantInteractionTest.kt`

- [ ] **Step 1: Write failing viewport and selection tests**

```kotlin
class StockVariantInteractionTest {
    @Test fun everyStockKind_supportsHorizontalViewport() {
        val kinds = listOf(
            AdvancedChartKind.OHLC, AdvancedChartKind.STOCK_AREA,
            AdvancedChartKind.STOCK_LINE, AdvancedChartKind.RENKO,
            AdvancedChartKind.KAGI, AdvancedChartKind.POINT_FIGURE,
        )
        assertTrue(kinds.all(::supportsStockViewport))
    }

    @Test fun viewport_clampsAndKeepsMinimumSpan() {
        assertEquals(0.84f..1f, normalizeStockViewport(0.95f, 1.2f, 0.16f))
    }

    @Test fun tooltip_usesVariantSpecificFields() {
        val item = AdvancedChartSelection(AdvancedChartKind.POINT_FIGURE, 2, "P3", 5f)
        assertEquals("P3  列高:5", formatStockSelection(item))
    }
}
```

- [ ] **Step 2: Run and verify failure**

Run: `.\gradlew.bat :KuiklyChart:allTests`

Expected: FAIL because the shared helpers do not exist.

- [ ] **Step 3: Add interaction flags and pure helpers**

```kotlin
class AdvancedChartInteractionConfig {
    var enableTap: Boolean = true
    var enableLongPressInspect: Boolean = true
    var enableCrosshair: Boolean = true
    var enablePan: Boolean = true
    var enableScale: Boolean = true
    var enableReset: Boolean = true
    var clampToData: Boolean = true
    var minimumVisibleRatio: Float = 0.16f
    var initialVisibleRatio: Float = 0.65f
}

class AdvancedChartAttr : ComposeAttr() {
    var preset: StockThemePreset = StockThemePreset.LIGHT
    val theme = ChartThemeOptions()
    val interaction = AdvancedChartInteractionConfig()
    fun theme(block: ChartThemeOptions.() -> Unit) = theme.apply(block)
    fun interaction(block: AdvancedChartInteractionConfig.() -> Unit) = interaction.apply(block)
}

internal fun supportsStockViewport(kind: AdvancedChartKind): Boolean = kind in setOf(
    AdvancedChartKind.OHLC, AdvancedChartKind.STOCK_AREA, AdvancedChartKind.STOCK_LINE,
    AdvancedChartKind.RENKO, AdvancedChartKind.KAGI, AdvancedChartKind.POINT_FIGURE,
)
```

Implement `normalizeStockViewport` and `formatStockSelection` as exhaustive pure functions; preserve non-stock advanced chart defaults by applying the new defaults only in stock component factory functions.

- [ ] **Step 4: Run focused tests**

Run: `.\gradlew.bat :KuiklyChart:allTests`

Expected: PASS.

- [ ] **Step 5: Commit interaction contracts**

```powershell
git add KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/advanced/AdvancedChartContracts.kt KuiklyChart/src/commonTest/kotlin/com/tencent/kuiklybase/chart/advanced
git commit -m "feat(stock): unify advanced interaction contracts"
```

### Task 5: Apply unified inspect, crosshair and viewport behavior to all advanced stock variants

**Files:**
- Modify: `KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/advanced/AdvancedChartView.kt`
- Extend test: `KuiklyChart/src/commonTest/kotlin/com/tencent/kuiklybase/chart/advanced/StockVariantInteractionTest.kt`

- [ ] **Step 1: Add failing hit-test mapping tests**

```kotlin
@Test fun visibleIndex_mapsBackToSourceIndex() {
    assertEquals(8, sourceIndexForVisibleSlot(slot = 2, count = 12, start = 0.5f, end = 1f))
}

@Test fun clearingSelection_emitsNullSelection() {
    assertEquals(-1, toggleAdvancedSelection(current = 3, hit = -1))
}
```

- [ ] **Step 2: Run and verify failure**

Run: `.\gradlew.bat :KuiklyChart:allTests`

Expected: FAIL because the mapping helpers are absent.

- [ ] **Step 3: Implement one state path in `AdvancedChartView`**

Use `supportsStockViewport(kind)` for all six advanced stock kinds. Route pan, pinch and reset through `normalizeStockViewport`. Store `inspectX`/`inspectY`; on click or long-press start, call one `inspectAt(x, y)` function that hit-tests the visible range, updates `selectedIndex`, formats Tooltip, and emits `onSelectionChange`.

Add to `AdvancedChartEvent`:

```kotlin
var onSelectionChange: ((AdvancedChartSelection?) -> Unit)? = null
fun selectionChange(handler: (AdvancedChartSelection?) -> Unit) {
    onSelectionChange = handler
}
```

- [ ] **Step 4: Make every renderer consume the visible range**

For OHLC, Renko and point figure, slice or map points through `visibleIndexRange(count, viewportStart, viewportEnd)` before computing slots. Keep Kagi, area and line on the same helper. Draw one selected vertical guide for all variants; draw horizontal crosshair only when the selection has a continuous numeric Y value.

- [ ] **Step 5: Use one themed Tooltip view**

Replace hard-coded `0xE62C3542` with a resolved overlay palette derived from `StockThemePreset`; place the Tooltip on the opposite side of the selected X coordinate so it does not cover the selected item.

- [ ] **Step 6: Run all advanced tests**

Run: `.\gradlew.bat :KuiklyChart:allTests`

Expected: PASS.

- [ ] **Step 7: Commit the seven-variant interaction implementation**

```powershell
git add KuiklyChart/src/commonMain/kotlin/com/tencent/kuiklybase/chart/advanced KuiklyChart/src/commonTest/kotlin/com/tencent/kuiklybase/chart/advanced
git commit -m "feat(stock): align interactions across variants"
```

### Task 6: Build the professional multi-period Demo card

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/kuikly/kuiklychart/DemoSampleData.kt`
- Modify: `shared/src/commonMain/kotlin/com/kuikly/kuiklychart/AdvancedDemoData.kt`
- Modify: `shared/src/commonMain/kotlin/com/kuikly/kuiklychart/StockChartDemoPage.kt`
- Modify: `shared/src/commonMain/kotlin/com/kuikly/kuiklychart/DemoGallery.kt`
- Modify: `shared/src/commonTest/kotlin/com/kuikly/kuiklychart/DemoLayoutTest.kt`

- [ ] **Step 1: Write failing Demo helper tests**

```kotlin
@Test fun stockPeriods_exposeTabsAndMoreMenu() {
    assertEquals(listOf("日K", "周K", "月K"), stockPrimaryPeriods().map { it.label })
    assertEquals(listOf("1分", "5分", "15分", "30分", "60分", "120分", "季K", "年K"),
        stockMorePeriods().map { it.label })
}

@Test fun professionalCard_isTallerForVolumePanel() {
    assertEquals(356f, stockVariantCardHeight(professional = true))
    assertEquals(236f, stockVariantCardHeight(professional = false))
}
```

- [ ] **Step 2: Run and verify failure**

Run: `.\gradlew.bat :shared:allTests`

Expected: FAIL because period and height helpers do not exist.

- [ ] **Step 3: Add period models and deterministic sample providers**

```kotlin
internal enum class StockPeriod(val label: String) {
    MIN_1("1分"), MIN_5("5分"), MIN_15("15分"), MIN_30("30分"),
    MIN_60("60分"), MIN_120("120分"), DAY("日K"), WEEK("周K"),
    MONTH("月K"), QUARTER("季K"), YEAR("年K"),
}

internal fun stockPrimaryPeriods() = listOf(StockPeriod.DAY, StockPeriod.WEEK, StockPeriod.MONTH)
internal fun stockMorePeriods() = listOf(
    StockPeriod.MIN_1, StockPeriod.MIN_5, StockPeriod.MIN_15, StockPeriod.MIN_30,
    StockPeriod.MIN_60, StockPeriod.MIN_120, StockPeriod.QUARTER, StockPeriod.YEAR,
)
```

Add `DemoSampleData.stockSeries(period: StockPeriod)` returning at least 30 OHLCV points per period so pan and zoom are visible. Expand the other seven providers to at least 24 items.

- [ ] **Step 4: Compose the professional card**

In `StockChartDemoPage`, store observable `selectedPeriod`, `showMorePeriods`, and `darkStockTheme`. The first card renders:

```kotlin
stockPeriodTabs(
    selected = { page.selectedPeriod },
    moreOpen = { page.showMorePeriods },
    onSelect = { period -> page.selectPeriod(period) },
    onToggleMore = { page.showMorePeriods = !page.showMorePeriods },
)

StockChart({ page.candles }) {
    attr {
        flex(1f)
        preset = if (page.darkStockTheme) StockThemePreset.DARK else StockThemePreset.LIGHT
        movingAverages {
            show = true
            line(5, 0xFF262626)
            line(10, 0xFFFAAD14)
            line(20, 0xFF1677FF)
            line(30, 0xFFEB2F96)
        }
        volumePanel {
            show = true
            average(5, 0xFF595959)
            average(10, 0xFFFAAD14)
        }
        interaction { enableCrosshair = true }
    }
}
```

Close the dropdown after selection and replace `candles` with the selected period data. Configure the other seven cards with the same theme preset and interaction defaults, but no MA/volume configuration.

- [ ] **Step 5: Run Demo tests**

Run: `.\gradlew.bat :shared:allTests`

Expected: PASS.

- [ ] **Step 6: Commit the Demo experience**

```powershell
git add shared/src/commonMain/kotlin/com/kuikly/kuiklychart shared/src/commonTest/kotlin/com/kuikly/kuiklychart/DemoLayoutTest.kt
git commit -m "feat(demo): showcase professional stock charts"
```

### Task 7: Document, verify and review the complete change

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Document exact public usage**

Add an OHLCV example and a `StockChart` configuration example containing `preset`, `movingAverages`, `volumePanel`, and `interaction`. Document that period controls are owned by the caller and that all stock variants share inspect/pan/zoom/reset behavior.

- [ ] **Step 2: Run the full common test suites**

Run: `.\gradlew.bat :KuiklyChart:allTests :shared:allTests`

Expected: BUILD SUCCESSFUL with all common tests passing.

- [ ] **Step 3: Run Android compilation**

Run: `.\gradlew.bat :KuiklyChart:compileDebugKotlinAndroid :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`

Expected: BUILD SUCCESSFUL and an Android debug APK under `androidApp/build/outputs/apk/debug/`.

- [ ] **Step 4: Inspect the working tree and review the diff**

Run: `git status --short` and `git diff --check HEAD~4..HEAD`.

Expected: no unstaged files, no whitespace errors, and only the planned stock chart, Demo, test and documentation changes.

- [ ] **Step 5: Commit documentation if it was not included earlier**

```powershell
git add README.md CHANGELOG.md
git commit -m "docs: describe professional stock chart interactions"
```

- [ ] **Step 6: Run required completion review**

Use `superpowers:requesting-code-review`, address all confirmed findings, rerun the focused tests for touched areas, then use `superpowers:verification-before-completion` before reporting success.
