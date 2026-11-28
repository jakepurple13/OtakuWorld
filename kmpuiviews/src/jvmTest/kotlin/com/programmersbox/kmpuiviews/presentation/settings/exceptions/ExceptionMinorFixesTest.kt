package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.hasText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.programmersbox.favoritesdatabase.ExceptionItem
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures
import com.programmersbox.kmpuiviews.domain.exceptions.parseException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ExceptionMinorFixesTest {

    private fun row(text: String, sourceName: String?) = ExceptionRow(
        item = ExceptionItem(time = 1_000, message = text, id = 9),
        parsed = parseException(text),
        source = null,
        sourceName = sourceName,
        extensionVersion = null,
        seenThisWeek = 1,
    )

    // Minor 1: with the extension not installed, its id shows once (as Extension), not also as Source.
    @Test
    fun uninstalledExtensionShowsIdOnce() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ExceptionDetail(
                    row = row(ExceptionFixtures.mangaDexTimeout, "en.mangadex"),
                    explanation = AiTextState.Idle, explainAvailable = false, twoPane = false,
                    onBack = {}, onDelete = {}, onCopy = {}, onShareOrSave = {}, onExplain = {},
                    onSourceAction = {}, sharedKeys = null,
                )
            }
        }
        onAllNodesWithText("en.mangadex").assertCountEquals(1)
    }

    // Minor 2: a very long message wraps inside the trace block instead of making one huge line.
    @Test
    fun longMessageWraps() = runComposeUiTest {
        val html = "kotlinx.serialization.json.internal.JsonDecodingException: Unexpected token\nJSON input: " +
            "<html>" + "x".repeat(4_000) + "</html>\n\tat a.b.C.d(C.kt:1)"
        setContent {
            MaterialTheme {
                Box(Modifier.width(360.dp)) { StackTraceBlock(parseException(html)) }
            }
        }
        // Layout width, not clipped bounds: a horizontally scrolled line is laid out at full length.
        val width = onNode(hasText("JSON input", substring = true)).fetchSemanticsNode().size.width
        assertTrue(width <= with(density) { 360.dp.roundToPx() }, "message is $width px wide")
    }

    // Timeline rows cap a huge message; the detail page shows all of it.
    @Test
    fun rowMessageIsCapped() = runComposeUiTest {
        val html = "kotlinx.serialization.json.internal.JsonDecodingException: Unexpected token\nJSON input: " +
            "<html>" + "x".repeat(4_000) + "</html>\n\tat a.b.C.d(C.kt:1)"
        setContent {
            MaterialTheme {
                Box(Modifier.width(360.dp)) { StackTraceBlock(parseException(html), messageMaxLines = 6) }
            }
        }
        val height = onNode(hasText("JSON input", substring = true)).fetchSemanticsNode().size.height
        val lineHeight = with(density) { 16.sp.toPx() } // M3 bodySmall line height
        assertTrue(height <= lineHeight * 6.5f, "message is $height px tall")
    }

    // Device bug: a row reported its exit twice, which closed its own Undo snackbar and committed the delete.
    @Test
    fun rowReportsExitOnce() = runComposeUiTest {
        val target = row(ExceptionFixtures.mangaDexTimeout, "MangaDex")
        val days = androidx.compose.runtime.mutableStateOf(listOf(ExceptionDay(kotlinx.datetime.LocalDate(2026, 11, 27), listOf(target))))
        val exiting = androidx.compose.runtime.mutableStateOf(emptySet<Long>())
        var exits = 0
        setContent {
            MaterialTheme {
                ExceptionTimeline(
                    days = days.value,
                    hourly = List(24) { 0 }, digest = AiTextState.Idle, digestAvailable = false, explainAvailable = false,
                    twoPane = false, selected = null, onSelect = {}, onMoreInfo = {}, onDelete = {}, onCopy = {},
                    onSummarize = {}, explanation = { kotlinx.coroutines.flow.MutableStateFlow(AiTextState.Idle) }, onExplain = {},
                    exiting = exiting.value,
                    // Like the screen: drop from exiting, then the view model hides the row a moment later.
                    onExited = {
                        exits++
                        exiting.value = exiting.value - it
                    },
                )
            }
        }
        exiting.value = setOf(target.id)
        waitForIdle()
        days.value = emptyList()
        waitForIdle()
        kotlin.test.assertEquals(1, exits)
    }

    private class FakeSnackbar(label: String?) : SnackbarData {
        override val visuals = object : SnackbarVisuals {
            override val message = "m"
            override val actionLabel = label
            override val withDismissAction = false
            override val duration = SnackbarDuration.Short
        }
        override fun performAction() = Unit
        override fun dismiss() = Unit
    }

    // Minor 5: ⌘Z only acts on an Undo snackbar, not "Report copied".
    @Test
    fun undoKeyOnlyTargetsUndoSnackbar() {
        assertTrue(isUndoSnackbar(FakeSnackbar("Undo"), undoLabel = "Undo"))
        assertFalse(isUndoSnackbar(FakeSnackbar(null), undoLabel = "Undo"))
        assertFalse(isUndoSnackbar(null, undoLabel = "Undo"))
    }
}
