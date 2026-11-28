package com.programmersbox.manga.shared.reader

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.programmersbox.datastore.mangasettings.PageIndicatorStyle
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3ExpressiveApi::class)
class PageIndicatorOverlayTest {

    private fun ComposeUiTest.show(style: PageIndicatorStyle, currentPage: Int, pages: Int, showItems: Boolean) =
        setContent {
            MaterialExpressiveTheme {
                PageIndicatorOverlay(
                    style = style,
                    currentPage = currentPage,
                    pages = pages,
                    showItems = showItems,
                    scrolling = false,
                    topPadding = 0.dp,
                    bottomPadding = 0.dp,
                    onOpenPages = {},
                    onJumpToPage = {},
                )
            }
        }

    private fun stepsMatcher(steps: Int) = SemanticsMatcher("slider has $steps steps") {
        it.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.steps == steps
    }

    @Test
    fun scrubberSnapsToEachPageInShortChapters() = runComposeUiTest {
        show(PageIndicatorStyle.Scrubber, currentPage = 4, pages = 24, showItems = true)
        waitForIdle()
        onNode(stepsMatcher(22)).assertExists()
    }

    @Test
    fun scrubberIsContinuousInLongChapters() = runComposeUiTest {
        show(PageIndicatorStyle.Scrubber, currentPage = 4, pages = 200, showItems = true)
        waitForIdle()
        onNode(stepsMatcher(0)).assertExists()
    }

    @Test
    fun ringAnnouncesPageAndOpensPages() = runComposeUiTest {
        show(PageIndicatorStyle.Ring, currentPage = 4, pages = 24, showItems = false)
        onNodeWithContentDescription("Page 5 of 24").assertHasClickAction()
    }

    @Test
    fun dotWindowAnnouncesPageAndOpensPages() = runComposeUiTest {
        show(PageIndicatorStyle.Dots, currentPage = 4, pages = 24, showItems = false)
        onNodeWithContentDescription("Page 5 of 24").assertHasClickAction()
    }

    @Test
    fun idleRingKeepsA48dpTouchTarget() = runComposeUiTest {
        show(PageIndicatorStyle.Ring, currentPage = 4, pages = 24, showItems = false)
        mainClock.advanceTimeBy(2_000)
        onNodeWithContentDescription("Page 5 of 24").assert(
            SemanticsMatcher("has a click action") { SemanticsActions.OnClick in it.config }
        )
        val bounds = onNodeWithContentDescription("Page 5 of 24").fetchSemanticsNode().touchBoundsInRoot
        val minPx = with(density) { 48.dp.toPx() }
        check(bounds.width >= minPx - 1 && bounds.height >= minPx - 1) { "touch bounds $bounds smaller than 48dp" }
    }
}
