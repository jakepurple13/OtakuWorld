package com.programmersbox.kmpuiviews.presentation.settings.extensions

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpmodels.KmpRemoteSources
import com.programmersbox.kmpuiviews.utils.LocalNavHostPadding
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalTestApi::class)
class CustomReposSheetTest {
    private val now = Clock.System.now()

    private fun remotes(count: Int) = List(count) {
        KmpRemoteSources(
            name = "ext$it",
            packageName = "pkg$it",
            version = "1",
            iconUrl = "",
            downloadLink = "",
            sources = emptyList(),
            lang = "en",
        )
    }

    private fun found(count: Int, name: String = "Repo $count", index: Int = 0, catalog: String = "Alpha") =
        CustomRepoStatus.Found(index, catalog, name, remotes(count), now)

    private fun ComposeUiTest.show(
        urls: List<String>,
        statuses: Map<String, CustomRepoStatus>,
        catalogNames: List<String>,
        onAdd: (String) -> Unit = {},
    ) = setContent {
        MaterialTheme {
            CompositionLocalProvider(LocalNavHostPadding provides PaddingValues(0.dp)) {
                CustomReposContent(
                    urls = urls,
                    statuses = statuses,
                    catalogNames = catalogNames,
                    installedPackages = emptySet(),
                    onAdd = onAdd,
                    onRemove = {},
                    onRecheck = {},
                    onRecheckAll = {},
                    onBrowse = {},
                    onCopy = {},
                )
            }
        }
    }

    @Test
    fun oneCatalogShowsBadgeAndSummary() = runComposeUiTest {
        val a = "https://a.example/repo"
        val b = "https://b.example/repo"
        val c = "https://c.example/repo"
        show(
            urls = listOf(a, b, c),
            statuses = mapOf(a to found(100), b to found(20), c to CustomRepoStatus.Failed("timeout", now)),
            catalogNames = listOf("Alpha"),
        )
        onNodeWithText("120 extensions from 2 repos · 1 needs attention. Tap a repo for details.").assertExists()
        onAllNodesWithText("Alpha").assertCountEquals(2)
    }

    @Test
    fun twoCatalogsUnsupportedCopy() = runComposeUiTest {
        val a = "https://a.example/repo"
        show(listOf(a), mapOf(a to CustomRepoStatus.Unsupported(now)), listOf("Alpha", "Beta"))
        onNodeWithText("a.example").performClick()
        onNodeWithText("Neither Alpha nor Beta can read this link. Supported: Alpha, Beta.").assertExists()
    }

    @Test
    fun checkingPillListsAllCatalogs() = runComposeUiTest {
        val a = "https://a.example/repo"
        show(listOf(a), mapOf(a to CustomRepoStatus.Checking), listOf("Alpha", "Beta"))
        onNodeWithText("Checking Alpha + Beta…").assertExists()
    }

    @Test
    fun problemReposSortFirst() = runComposeUiTest {
        val ok1 = "https://ok1.example/repo"
        val bad = "https://bad.example/repo"
        val ok2 = "https://ok2.example/repo"
        show(
            urls = listOf(ok1, bad, ok2),
            statuses = mapOf(ok1 to found(1, "One"), bad to CustomRepoStatus.Failed("nope", now), ok2 to found(2, "Two")),
            catalogNames = listOf("Alpha"),
        )
        // Titles sit inside the clickable card header, so they are only separate nodes in the unmerged tree.
        val titles = onAllNodesWithTag(REPO_TITLE_TAG, useUnmergedTree = true)
        titles[0].assertTextEquals("bad.example")
        titles[1].assertTextEquals("One")
        titles[2].assertTextEquals("Two")
    }

    @Test
    fun failedHasRetryNotBrowse() = runComposeUiTest {
        val failed = "https://failed.example/repo"
        val unsupported = "https://unsupported.example/repo"
        show(
            urls = listOf(failed, unsupported),
            statuses = mapOf(failed to CustomRepoStatus.Failed("nope", now), unsupported to CustomRepoStatus.Unsupported(now)),
            catalogNames = listOf("Alpha"),
        )
        onNodeWithText("failed.example").performClick()
        onNodeWithText("Retry").assertExists()
        onNodeWithText("Browse").assertDoesNotExist()

        onNodeWithText("unsupported.example").performClick()
        onNodeWithText("Alpha can't read this link. Supported: Alpha.").assertExists()
        onNodeWithText("Retry").assertDoesNotExist()
        onNodeWithText("Browse").assertDoesNotExist()
    }

    @Test
    fun duplicateAddExpandsExisting() = runComposeUiTest {
        val saved = "https://a.example/repo"
        val added = mutableListOf<String>()
        show(listOf(saved), mapOf(saved to found(3, "Saved")), listOf("Alpha"), onAdd = { added += it })
        onNodeWithText("Add repo", useUnmergedTree = true).performClick()
        onNode(hasSetTextAction()).performTextInput("https://a.example/repo/index.min.json")
        onNodeWithText("Add").performClick()
        waitForIdle()
        assertTrue(added.isEmpty(), "onAdd called with $added")
        // The existing card is expanded.
        onNodeWithText(saved).assertExists()
    }

    @Test
    fun duplicateAddScrollsToExisting() = runComposeUiTest {
        val urls = List(20) { "https://r$it.example/repo" }
        val target = urls.last()
        show(urls, urls.associateWith { found(1, it.substringAfter("://").substringBefore('.')) }, listOf("Alpha"))
        // The target card starts far below the viewport, so the lazy list has not composed it.
        onNodeWithText(target.substringAfter("://").substringBefore('.'), useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("Add repo", useUnmergedTree = true).performClick()
        onNode(hasSetTextAction()).performTextInput("$target/")
        onNodeWithText("Add").performClick()
        waitForIdle()
        onNodeWithText(target).assertIsDisplayed()
    }

    @Test
    fun failedMessageTruncatedTo120() = runComposeUiTest {
        val a = "https://a.example/repo"
        val long = "x".repeat(150) + "\nsecond line"
        show(listOf(a), mapOf(a to CustomRepoStatus.Failed(long, now)), listOf("Alpha"))
        onNodeWithText("a.example").performClick()
        onNodeWithText("Last check failed: ${"x".repeat(120)}…. Check the link or try again later.").assertExists()
        onNodeWithText("second line", substring = true).assertDoesNotExist()
    }

    @Test
    fun noCatalogNameLiteralsInSource() {
        val dir = "src/commonMain/kotlin/com/programmersbox/kmpuiviews/presentation/settings/extensions"
        listOf("CustomReposSheet.kt", "CustomRepoChecker.kt").forEach { name ->
            val text = File(dir, name).readText()
            listOf("Mihon", "Aniyomi", "CloudStream").forEach { literal ->
                assertEquals(false, literal in text, "$name contains $literal")
            }
        }
    }
}
