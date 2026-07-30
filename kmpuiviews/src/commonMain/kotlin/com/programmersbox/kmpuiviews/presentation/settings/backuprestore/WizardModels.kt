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
data class ListSubItemState(
    val id: String,
    val name: String,
    val coverUrl: String?,
    val itemCount: Int,
    val requiresBiometric: Boolean,
    val selected: Boolean = true,
)

@Stable
data class WizardItemState(
    val uiInfo: BackupUiInfo,
    val summary: BackupDataSummary? = null,
    val expanded: Boolean = false,
    val selected: Boolean = true,
    val subItems: List<ListSubItemState>? = null,
)
