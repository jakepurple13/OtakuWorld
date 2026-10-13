package com.programmersbox.kmpuiviews.presentation.settings.extensions

import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpSourceInformation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstalledViewStateTest {
    private val service = object : KmpApiService { override val baseUrl = "" }
    private fun info(pkg: String) = KmpSourceInformation(service, "n", null, pkg)

    @Test
    fun singlePackageGroupCanUninstallAll() =
        assertTrue(InstalledViewState(listOf(info("a"), info("a"))).canUninstallAll)

    @Test
    fun multiPackageGroupCannotUninstallAll() =
        assertFalse(InstalledViewState(listOf(info("a"), info("b"))).canUninstallAll)
}
