package com.programmersbox.kmpuiviews.presentation.updatecheck

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/** Desktop has no share sheet, so the report goes to the system clipboard. */
class ClipboardUpdateCheckReportSharer : UpdateCheckReportSharer {
    override fun share(text: String): Boolean {
        val selection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        return true
    }
}
