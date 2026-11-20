package com.programmersbox.kmpuiviews.presentation.onboarding.steps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.platform
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.presentation.onboarding.HeroShape
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingStepSpec
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingTags
import com.programmersbox.kmpuiviews.presentation.onboarding.frame.StepLayout
import com.programmersbox.kmpuiviews.presentation.onboarding.motion.onboardingReducedMotion
import com.programmersbox.kmpuiviews.presentation.settings.moresettings.MoreSettingsViewModel
import com.programmersbox.supabaseintegration.ui.SupabaseRoutes
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.koin.compose.viewmodel.koinViewModel

/** Returns a function that opens the backup picker and reports the chosen file (null on cancel). */
typealias BackupPicker = @Composable (onResult: (PlatformFile?) -> Unit) -> (() -> Unit)

private val DefaultBackupPicker: BackupPicker = { onResult ->
    val launcher = rememberFilePickerLauncher(type = FileKitType.File("zip")) { onResult(it) }
    val launch: () -> Unit = { launcher.launch() }
    launch
}

private val DefaultBackupImporter: @Composable () -> (PlatformFile) -> Unit = {
    val vm: MoreSettingsViewModel = koinViewModel()
    val import: (PlatformFile) -> Unit = vm::importFullBackup
    import
}

/**
 * Step 6: Supabase sync, backup restore and (Android only) biometric lock.
 *
 * Sync "On" can't be read from the Supabase auth state here (it needs the sync module in DI), so
 * it flips once the user opens the Supabase route. "Restored" flips after a file is chosen and
 * handed to the importer; the import itself runs in the background and reports no count.
 */
fun OnboardingScope.syncStep(
    navController: NavigationActions,
    showBiometric: Boolean = platform() == "Android",
    backupPicker: BackupPicker = DefaultBackupPicker,
    backupImporter: @Composable () -> (PlatformFile) -> Unit = DefaultBackupImporter,
) {
    var syncOn by mutableStateOf(false)
    var restored by mutableStateOf(false)
    val spec = OnboardingStepSpec(
        id = "sync",
        title = "Keep it safe",
        body = "Bring your data with you and lock what is private.",
        icon = Icons.Default.Cloud,
        shape = HeroShape.Bun,
        summaryLabel = "Sync & privacy",
        summary = {
            listOfNotNull(
                "Sync on".takeIf { syncOn },
                "Backup restored".takeIf { restored },
            ).joinToString(" · ").ifEmpty { "Nothing turned on" }
        },
    )
    step(spec) {
        val import = backupImporter()
        val launchPicker = backupPicker { file ->
            if (file != null) {
                import(file)
                restored = true
            }
        }
        Box(Modifier.fillMaxSize().testTag(OnboardingTags.step(spec.id))) {
            StepLayout(spec) {
                SyncRow(
                    icon = Icons.Default.CloudSync,
                    title = "Sync with Supabase",
                    sub = "Favorites on every device",
                    idle = "Set up",
                    done = "On",
                    isDone = syncOn,
                ) {
                    syncOn = true
                    navController.navigate(SupabaseRoutes)
                }
                SyncRow(
                    icon = Icons.Default.Restore,
                    title = "Restore from backup",
                    sub = "Import a .zip backup",
                    idle = "Choose",
                    done = "Done",
                    isDone = restored,
                    onClick = launchPicker,
                )
                if (showBiometric) {
                    SyncRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric lock",
                        sub = "Fingerprint, face or PIN for locked lists",
                        idle = null,
                        done = "",
                        isDone = false,
                        onClick = navController::security,
                    )
                }
                if (restored) {
                    Text(
                        "Backup restored",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncRow(
    icon: ImageVector,
    title: String,
    sub: String,
    idle: String?,
    done: String,
    isDone: Boolean,
    onClick: () -> Unit,
) {
    val reduced = onboardingReducedMotion()
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = scheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(icon, null, tint = scheme.primary, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            if (idle != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isDone) scheme.primary else scheme.secondaryContainer,
                ) {
                    AnimatedContent(
                        targetState = isDone,
                        transitionSpec = {
                            if (reduced) {
                                fadeIn() togetherWith fadeOut()
                            } else {
                                (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith
                                        (fadeOut() + scaleOut(targetScale = 0.8f))
                            }
                        },
                        label = "sync pill",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    ) { on ->
                        Text(
                            if (on) done else idle,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (on) scheme.onPrimary else scheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}
