package com.programmersbox.animeworldbaselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a Baseline Profile for AnimeWorld covering:
 * 1. Cold startup (app launch to first meaningful frame)
 * 2. First-scroll on the main browse list
 * 3. Navigate to a detail screen and back
 *
 * Run via: ./gradlew :animeworld:generateNoFirebaseReleaseBaselineProfile
 *
 * Add Modifier.testTag("browse_list") to the main LazyColumn/LazyVerticalGrid in
 * kmpuiviews and Modifier.testTag("detail_screen") to the detail screen composable
 * to make the journey more precise.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndScroll() {
        rule.collect(packageName = "com.programmersbox.animeworld") {
            pressHome()
            startActivityAndWait()

            // Wait for content to load
            Thread.sleep(2_000)

            // Scroll the main browse list — finds the first scrollable container
            val list = device.findObject(By.scrollable(true))
            if (list != null) {
                list.setGestureMargin(device.displayWidth / 5)
                list.fling(Direction.DOWN)
                list.fling(Direction.DOWN)
                list.fling(Direction.UP)
            }

            // Tap the first list item to warm up detail screen composition
            val firstItem = device.findObject(By.clickable(true).depth(4))
            if (firstItem != null) {
                firstItem.click()
                Thread.sleep(1_500)
                device.pressBack()
            }
        }
    }
}
