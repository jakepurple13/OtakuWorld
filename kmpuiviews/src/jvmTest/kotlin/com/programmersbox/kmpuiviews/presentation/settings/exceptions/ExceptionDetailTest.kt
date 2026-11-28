package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.programmersbox.favoritesdatabase.ExceptionItem
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures
import com.programmersbox.kmpuiviews.domain.exceptions.parseException
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ExceptionDetailTest {
    private class FakeApi(override val baseUrl: String) : KmpApiService

    private val comick = ExceptionRow(
        item = ExceptionItem(time = 1_000, message = ExceptionFixtures.comickJson, id = 7),
        parsed = parseException(ExceptionFixtures.comickJson),
        source = KmpSourceInformation(FakeApi("https://comick.io"), "Comick", null, "eu.kanade.tachiyomi.extension.all.comickfun"),
        sourceName = "Comick",
        extensionVersion = "1.4.63",
        seenThisWeek = 1,
    )

    @Test
    fun showsHeadlineFactsAndSourceMenu() = runComposeUiTest {
        var action: SourceAction? = null
        setContent {
            MaterialTheme {
                ExceptionDetail(
                    row = comick,
                    explanation = AiTextState.Idle,
                    explainAvailable = false,
                    twoPane = false,
                    onBack = {},
                    onDelete = {},
                    onCopy = {},
                    onShareOrSave = {},
                    onExplain = {},
                    onSourceAction = { action = it },
                    sharedKeys = null,
                )
            }
        }
        onNodeWithText("Comick sent something the app couldn't read").assertIsDisplayed()
        onNodeWithText("all.comickfun").assertIsDisplayed()
        onNodeWithText("JsonDecodingException").assertIsDisplayed()
        onNodeWithText("Stack trace").assertIsDisplayed()
        onNode(hasText("Source") and hasClickAction()).performClick()
        onNodeWithText("Suggested").assertIsDisplayed()
        onNodeWithText("Open website").performClick()
        assertEquals(SourceAction.OpenWebsite, action)
    }
}
