package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import com.programmersbox.favoritesdatabase.ExceptionItem
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionFixtures
import com.programmersbox.kmpuiviews.domain.exceptions.ExceptionKind
import com.programmersbox.kmpuiviews.domain.exceptions.parseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SourceActionsTest {
    private class FakeApi(override val baseUrl: String) : KmpApiService

    private fun row(text: String, installed: Boolean) = ExceptionRow(
        item = ExceptionItem(time = 1, message = text, id = 1),
        parsed = parseException(text),
        source = if (installed) KmpSourceInformation(FakeApi("https://x.org"), "X", null, "pkg") else null,
        sourceName = "X",
        extensionVersion = null,
        seenThisWeek = 1,
    )

    @Test
    fun suggestedByKind() {
        assertEquals(SourceAction.OpenWebsite, suggestedAction(ExceptionKind.Blocked))
        assertEquals(SourceAction.OpenWebsite, suggestedAction(ExceptionKind.Parsing))
        assertEquals(SourceAction.OpenWebsite, suggestedAction(ExceptionKind.Network))
        assertEquals(SourceAction.CheckUpdate, suggestedAction(ExceptionKind.Source))
        assertNull(suggestedAction(ExceptionKind.App))
    }

    @Test
    fun appErrorHasNoActions() {
        assertTrue(availableActions(row(ExceptionFixtures.backupFailure, installed = false)).isEmpty())
    }

    @Test
    fun uninstalledOnlyChecksForUpdate() {
        assertEquals(listOf(SourceAction.CheckUpdate), availableActions(row(ExceptionFixtures.mangaDexTimeout, installed = false)))
    }

    @Test
    fun installedListsAllSuggestedFirst() {
        val sourceError = availableActions(row(ExceptionFixtures.weebCentral, installed = true))
        assertEquals(SourceAction.CheckUpdate, sourceError.first())
        assertEquals(4, sourceError.size)
        assertEquals(SourceAction.OpenWebsite, availableActions(row(ExceptionFixtures.asura403, installed = true)).first())
    }
}
