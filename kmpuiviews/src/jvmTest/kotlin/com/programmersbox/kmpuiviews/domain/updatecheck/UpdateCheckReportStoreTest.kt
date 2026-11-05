package com.programmersbox.kmpuiviews.domain.updatecheck

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateCheckReportStoreTest {

    private val full = UpdateCheckReport(
        startedAt = 1L,
        finishedAt = 2L,
        favoriteCount = 10,
        checkedCount = 8,
        updatedCount = 3,
        sourceFailures = listOf(SourceFailure("S", FailureKind.Http, 503, "HTTP 503")),
        itemFailures = listOf(
            ItemFailure(
                url = "u",
                title = "t",
                imageUrl = "i",
                source = "S",
                kind = FailureKind.TimedOut,
                httpCode = null,
                detail = "d",
                sourceFailed = true,
            )
        ),
        stoppedEarly = "why",
        retriedAt = 5L,
    )

    @Test
    fun `round trip keeps all fields`() {
        assertEquals(full, decodeUpdateCheckReport(full.encode()))
    }

    @Test
    fun `blank string decodes to null`() {
        assertNull(decodeUpdateCheckReport(""))
        assertNull(decodeUpdateCheckReport("   "))
    }

    @Test
    fun `corrupt json decodes to null`() {
        assertNull(decodeUpdateCheckReport("{\"startedAt\":"))
    }

    @Test
    fun `unknown fields are ignored`() {
        val withExtra = full.encode().removeSuffix("}") + ",\"futureField\":42}"
        assertEquals(full, decodeUpdateCheckReport(withExtra))
    }
}
