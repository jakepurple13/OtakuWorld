package com.programmersbox.kmpuiviews.presentation.settings.backuprestore

import androidx.compose.runtime.Stable
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo

sealed interface BackupWizardStep {
    data object SelectItems : BackupWizardStep
    data object Review : BackupWizardStep
    data object Executing : BackupWizardStep
    data object Complete : BackupWizardStep
}

sealed interface RestoreWizardStep {
    data object PickFile : RestoreWizardStep
    data object SelectItems : RestoreWizardStep
    data object Review : RestoreWizardStep
    data object Executing : RestoreWizardStep
    data object Complete : RestoreWizardStep
}

@Stable
data class WizardItemState(
    val uiInfo: BackupUiInfo,
    val summary: BackupDataSummary? = null,
    val expanded: Boolean = false,
    val selected: Boolean = true,
)
