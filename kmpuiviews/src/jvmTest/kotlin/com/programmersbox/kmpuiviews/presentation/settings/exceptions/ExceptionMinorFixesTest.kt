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
