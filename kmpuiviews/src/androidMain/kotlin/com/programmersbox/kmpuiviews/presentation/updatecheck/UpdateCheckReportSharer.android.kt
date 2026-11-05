package com.programmersbox.kmpuiviews.presentation.updatecheck

import android.content.Context
import android.content.Intent

class AndroidUpdateCheckReportSharer(
    private val context: Context,
) : UpdateCheckReportSharer {
    override fun share(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(
            Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
