package com.programmersbox.kmpuiviews.presentation.updatecheck

import androidx.lifecycle.ViewModelStore
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.updatecheck.FailureKind
import com.programmersbox.kmpuiviews.domain.updatecheck.ItemFailure
import com.programmersbox.kmpuiviews.domain.updatecheck.SourceFailure
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateCheckReport
import com.programmersbox.kmpuiviews.presentation.settings.notifications.FakeWorkRepository
import com.programmersbox.kmpuiviews.testing.FakeUpdateCheckReportStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateCheckErrorsViewModelTest {

    private val viewModelStore = ViewModelStore()

    private class FakeSharer(private val copiedToClipboard: Boolean = false) : UpdateCheckReportSharer {
        val shared = CompletableDeferred<String>()
        override fun share(text: String): Boolean {
            shared.complete(text)
            return copiedToClipboard
        }
    }

    private fun item(
        title: String,
        source: String,
        kind: FailureKind = FailureKind.TimedOut,
        httpCode: Int? = null,
        detail: String = "",
        sourceFailed: Boolean = false,
    ) = ItemFailure(
        url = "https://example.com/$title",
        title = title,
        imageUrl = "",
        source = source,
        kind = kind,
        httpCode = httpCode,
        detail = detail,
        sourceFailed = sourceFailed,
    )

    // 2026-11-05T09:41:00Z
    private val finishedAt = 1_793_871_660_000L

    private fun report(
        sourceFailures: List<SourceFailure> = emptyList(),
        itemFailures: List<ItemFailure> = emptyList(),
    ) = UpdateCheckReport(
        startedAt = finishedAt - 38_000,
        finishedAt = finishedAt,
        favoriteCount = 42,
        checkedCount = 42,
        updatedCount = 3,
        sourceFailures = sourceFailures,
        itemFailures = itemFailures,
    )

    private val mixedReport = report(
        sourceFailures = listOf(SourceFailure("comick", FailureKind.Http, 403, "HTTP 403 Forbidden")),
        itemFailures = listOf(
            item("Blue Lock", "mangapark", detail = "Timed out waiting for 15000 ms"),
            item("Dandadan", "comick", FailureKind.Http, 403, "HTTP 403 Forbidden", sourceFailed = true),
            item("One Piece", "asura", FailureKind.CouldNotRead),
            item("Bleach", "asura", FailureKind.CouldNotRead),
        ),
    )

    private fun viewModel(
        report: UpdateCheckReport?,
        workRepository: FakeWorkRepository = FakeWorkRepository(),
        sharer: UpdateCheckReportSharer = FakeSharer(),
    ) = UpdateCheckErrorsViewModel(
        store = FakeUpdateCheckReportStore(report),
        workRepository = workRepository,
        sharer = sharer,
        sourceRepository = SourceRepository(),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `groups put source failures first`() = runTest {
        val vm = viewModel(mixedReport)

        val groups = vm.groups.value
        assertEquals(listOf("comick", "asura", "mangapark"), groups.map { it.source })
        assertEquals(FailureKind.Http, groups.first().dominantKind)
        assertEquals(listOf("Dandadan"), groups.first().items.map { it.title })
        assertEquals(FailureKind.CouldNotRead, groups[1].dominantKind)
    }

    @Test
    fun `canRetry false without item failures`() = runTest {
        assertFalse(viewModel(report(itemFailures = emptyList())).canRetry.value)
        assertFalse(viewModel(null).canRetry.value)
        assertTrue(viewModel(mixedReport).canRetry.value)
    }

    @Test
    fun `retryFailed calls workRepository once`() = runTest {
        val workRepository = FakeWorkRepository()
        val vm = viewModel(mixedReport, workRepository = workRepository)

        vm.retryFailed()
        vm.messages.first()

        assertEquals(1, workRepository.retryCount)
    }

    @Test
    fun `retryFailed emits rechecking message with count`() = runTest {
        val vm = viewModel(mixedReport)

        vm.retryFailed()

        assertEquals(UpdateCheckErrorsMessage.Rechecking(4), vm.messages.first())
    }

    @Test
    fun `retryFailed emits already running when refused`() = runTest {
        val vm = viewModel(mixedReport, workRepository = FakeWorkRepository(retryAccepted = false))

        vm.retryFailed()

        assertEquals(UpdateCheckErrorsMessage.AlreadyRunning, vm.messages.first())
    }

    @Test
    fun `share emits copied message only when sharer copied`() = runTest {
        val copying = FakeSharer(copiedToClipboard = true)
        val copyingVm = viewModel(mixedReport, sharer = copying)
        copyingVm.share()
        assertEquals(UpdateCheckErrorsMessage.Copied, copyingVm.messages.first())

        val chooser = FakeSharer(copiedToClipboard = false)
        val chooserVm = viewModel(mixedReport, sharer = chooser)
        chooserVm.share()
        chooser.shared.await()
        // Nothing was copied, so no message may be waiting.
        assertNull(withTimeoutOrNull(200) { chooserVm.messages.first() })
    }

    @Test
    fun `share sends text containing every failed title`() = runTest {
        val sharer = FakeSharer()
        val vm = viewModel(mixedReport, sharer = sharer)

        vm.share()

        val text = sharer.shared.await()
        mixedReport.itemFailures.forEach { assertTrue(it.title in text, "missing ${it.title} in:\n$text") }
    }

    @Test
    fun `toShareText matches format`() = runTest {
        val report = report(
            sourceFailures = listOf(SourceFailure("comick", FailureKind.Http, 403, "HTTP 403 Forbidden")),
            itemFailures = listOf(
                item("Dandadan", "comick", FailureKind.Http, 403, "HTTP 403 Forbidden", sourceFailed = true),
                item("Blue Lock", "mangapark", detail = "Timed out waiting for 15000 ms"),
            ),
        )
        val names = mapOf("comick" to "Comick", "mangapark" to "MangaPark")

        val text = report.toShareText(displayName = { names.getValue(it) }, timeZone = TimeZone.UTC)

        assertEquals(
            """
            Update check · 2026-11-05 09:41
            42 favorites · 3 updated · 2 failed

            Comick — Couldn't load Comick's recent updates (HTTP 403 · likely Cloudflare)
              Dandadan — Not checked: source failed
            MangaPark
              Blue Lock — No response after 15s
                Timed out waiting for 15000 ms
            """.trimIndent(),
            text,
        )
    }
}
