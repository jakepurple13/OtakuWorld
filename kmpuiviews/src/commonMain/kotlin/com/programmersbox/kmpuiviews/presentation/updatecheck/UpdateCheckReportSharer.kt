package com.programmersbox.kmpuiviews.presentation.updatecheck

/** Hands an update check report off to the platform (share sheet, clipboard). */
interface UpdateCheckReportSharer {
    fun share(text: String)
}
