package com.programmersbox.animeworld

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.ncorti.slidetoact.SlideToActView
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import java.io.File

class DeleteDialog(
    context: Context?,
    var title: String = "",
    val download: Download? = null,
    val file: File? = null,
    var listener: DeleteDialogListener? = null
) : Dialog(context!!) {

    companion object {
        fun deleteDialog(context: Context?, download: Download? = null, file: File? = null, block: DeleteDialog.() -> Unit): DeleteDialog =
            DeleteDialog(context, file = file, download = download).apply(block)

        fun deleteDialog(context: Context?, title: String, block: DeleteDialog.() -> Unit): DeleteDialog =
            DeleteDialog(context, title = title).apply(block)
    }

    class DialogBuilder {
        var title = ""
        var download: Download? = null
        var file: File? = null
        var dialogListener: DeleteDialogListener? = null
        var context: Context? = null

        fun deleteDialog(block: DialogBuilder.() -> Unit): DeleteDialog {
            return DialogBuilder().apply(block).build()
        }

        fun build(): DeleteDialog =
            DeleteDialog(context, title, download, file, dialogListener)
    }

    interface DeleteDialogListener {
        fun onDelete()
        fun onCancel() {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.delete_dialog_layout)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (download != null)
            Fetch.getDefaultInstance().pause(download.id)
        setTitle("Delete $title")
        findViewById<TextView>(R.id.textView6).append(title)

        val allDownloadInfo = findViewById<TextView>(R.id.all_download_info)

        if (download != null) {
            val info = "Current Progress: ${context.getString(R.string.percent_progress, download.progress)}"
            allDownloadInfo.text = info
        }

        if (file != null) {
            allDownloadInfo.text = file.path
        }

        findViewById<SlideToActView>(R.id.slide_button).onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                if (download != null) {
                    Fetch.getDefaultInstance().delete(download.id)
                }
                file?.delete()
                this@DeleteDialog.dismiss()
                listener?.onDelete()
            }
        }

        findViewById<Button>(R.id.delete_dismiss_button).setOnClickListener {
            dismiss()
        }

        setOnDismissListener {
            listener?.onCancel()
            if (download != null)
                Fetch.getDefaultInstance().resume(download.id)
        }
    }

}