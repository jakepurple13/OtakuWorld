package com.programmersbox.novelworldbaselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Macrobenchmarks for NovelWorld.
 *
 * Run via: ./gradlew :NovelWorldbaselineprofile:connectedNoFirebaseReleaseAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
 *
 * Results land in: NovelWorldbaselineprofile/build/outputs/connected_android_test_additional_output/
 *
 * IMPORTANT: Run on a physical low-end device or aosp_cf_x86_64_phone-userdebug.
 * Debug builds produce non-representative numbers — always use release.
 *
 * ReportDrawnWhen { viewModel.filteredSourceList.isNotEmpty() } is wired in RecentScreen
 * so timeToFullDisplay fires when the first real content frame renders.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupCompilationNone() = startupBenchmark(CompilationMode.None())

    @Test
    fun startupCompilationBaselineProfiles() =
        startupBenchmark(CompilationMode.Partial(BaselineProfileMode.Require))

    @Test
    fun warmStartCompilationBaselineProfiles() = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        startupMode = StartupMode.WARM,
        iterations = 10,
        setupBlock = { pressHome() },
        measureBlock = { startActivityAndWait() },
    )

    @Test
    fun hotStartCompilationBaselineProfiles() = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        startupMode = StartupMode.HOT,
        iterations = 10,
        setupBlock = { pressHome() },
        measureBlock = { startActivityAndWait() },
    )

    @Test
    fun scrollListFrameTiming() = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = { pressHome() },
        measureBlock = {
            startActivityAndWait()
            Thread.sleep(1_000)
            val list = device.findObject(By.scrollable(true))
            if (list != null) {
                list.setGestureMargin(device.displayWidth / 5)
                repeat(3) { list.fling(Direction.DOWN) }
            }
        },
    )

    private fun startupBenchmark(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.COLD,
        iterations = 10,
        setupBlock = { pressHome() },
        measureBlock = { startActivityAndWait() },
    )

    companion object {
        private const val PACKAGE_NAME = "com.programmersbox.novelworld"
    }
}
