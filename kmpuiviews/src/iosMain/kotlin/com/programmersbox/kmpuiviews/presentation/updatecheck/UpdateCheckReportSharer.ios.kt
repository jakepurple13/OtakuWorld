package com.programmersbox.kmpuiviews.presentation.updatecheck

class NoOpUpdateCheckReportSharer : UpdateCheckReportSharer {
    override fun share(text: String): Boolean = false
}
