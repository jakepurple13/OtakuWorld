package com.programmersbox.kmpuiviews.presentation.updatecheck

/** Hands an update check report off to the platform (share sheet, clipboard). */
interface UpdateCheckReportSharer {
    /** Returns true when the report was copied to the clipboard instead of opening a share sheet. */
    fun share(text: String): Boolean
}
