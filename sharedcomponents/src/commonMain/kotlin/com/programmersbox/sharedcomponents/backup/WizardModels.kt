package com.programmersbox.sharedcomponents.backup

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

data class WizardItemState(
    val uiInfo: BackupUiInfo,
    val summary: BackupDataSummary? = null,
    val expanded: Boolean = false,
    val selected: Boolean = true,
)
