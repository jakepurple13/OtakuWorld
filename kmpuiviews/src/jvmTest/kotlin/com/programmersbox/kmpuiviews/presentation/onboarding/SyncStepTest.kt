package com.programmersbox.kmpuiviews.presentation.onboarding

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.LocalReducedMotionOverride
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.BackupPicker
import com.programmersbox.kmpuiviews.presentation.onboarding.steps.syncStep
import com.programmersbox.kmpuiviews.utils.LocalWindowSizeClass
import com.programmersbox.supabaseintegration.ui.SupabaseRoutes
import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
)
class SyncStepTest {
    private val calls = mutableListOf<String>()
    private val nav: NavigationActions = Proxy.newProxyInstance(
        NavigationActions::class.java.classLoader,
        arrayOf(NavigationActions::class.java),
    ) { _, method, args ->
        calls += if (method.name == "navigate") "navigate:${args?.firstOrNull()}" else method.name
        null
    } as NavigationActions

    private val imported = mutableListOf<PlatformFile>()
    private var pickerResult: (PlatformFile?) -> Unit = {}
    private val picker: BackupPicker = { onResult ->
        pickerResult = onResult
        val launch: () -> Unit = {}
        launch
    }

    private val signedIn = mutableStateOf(false)

    private fun ComposeUiTest.show(showBiometric: Boolean) {
        val scope = buildOnboardingScope {
            syncStep(nav, showBiometric, picker, { { imported += it } }, { signedIn.value })
        }
        setContent {
            MaterialExpressiveTheme {
                CompositionLocalProvider(
                    LocalReducedMotionOverride provides true,
                    LocalWindowSizeClass provides WindowSizeClass.calculateFromSize(DpSize(400.dp, 1600.dp)),
                ) { scope[0].content() }
            }
        }
    }

    @Test
    fun biometric_row_opens_security() = runComposeUiTest {
        show(showBiometric = true)
        onNodeWithText("Biometric lock").performClick()
        assertEquals(listOf("security"), calls)
    }

    @Test
    fun supabase_row_navigates() = runComposeUiTest {
        show(showBiometric = true)
        onNodeWithText("Sync with Supabase").performClick()
        assertEquals(listOf("navigate:$SupabaseRoutes"), calls)
        onAllNodesWithText("On").assertCountEquals(0)
    }

    @Test
    fun on_state_follows_signed_in() = runComposeUiTest {
        show(showBiometric = false)
        onAllNodesWithText("On").assertCountEquals(0)
        runOnIdle { signedIn.value = true }
        waitForIdle()
        onNodeWithText("On").assertExists()
    }

    @Test
    fun summary_reflects_sync_and_restore() = runComposeUiTest {
        val scope = buildOnboardingScope {
            syncStep(nav, false, picker, { { imported += it } }, { signedIn.value })
        }
        setContent { Text(scope[0].spec.summary!!()) }
        onNodeWithText("Nothing turned on").assertExists()
        runOnIdle { signedIn.value = true }
        waitForIdle()
        onNodeWithText("Sync on").assertExists()
    }

    @Test
    fun restore_cancel_keeps_state() = runComposeUiTest {
        show(showBiometric = true)
        onNodeWithText("Restore from backup").performClick()
        runOnIdle { pickerResult(null) }
        waitForIdle()
        onNodeWithText("Choose").assertExists()
        onAllNodesWithText("Restoring…").assertCountEquals(0)
        assertEquals(0, imported.size)
    }

    @Test
    fun restore_pick_imports_and_shows_restoring() = runComposeUiTest {
        show(showBiometric = true)
        val file = PlatformFile(File("backup.zip"))
        runOnIdle { pickerResult(file) }
        waitForIdle()
        onNodeWithText("Restoring…").assertExists()
        onNodeWithText("Restoring your backup in the background").assertExists()
        assertEquals(listOf(file), imported)
    }

    @Test
    fun biometric_hidden_off_android() = runComposeUiTest {
        show(showBiometric = false)
        onAllNodesWithText("Biometric lock").assertCountEquals(0)
    }
}
