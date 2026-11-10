package com.programmersbox.kmpuiviews.presentation.quickactions

enum class QuickActionGroup { Tools, GoTo }

/** Quick actions in display order. */
enum class QuickAction(val group: QuickActionGroup) {
    CheckForUpdates(QuickActionGroup.Tools),
    ScanQrCode(QuickActionGroup.Tools),
    GlobalSearch(QuickActionGroup.GoTo),
    Lists(QuickActionGroup.GoTo),
    SavedNotifications(QuickActionGroup.GoTo),
    BackgroundTasks(QuickActionGroup.GoTo),
    UpdateErrors(QuickActionGroup.GoTo),
}
