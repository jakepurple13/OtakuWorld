package com.programmersbox.kmpuiviews.domain.updatecheck

import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel

/** The chapters of [info] that are new since the favorite had [oldCount] chapters. */
fun newChapters(info: KmpInfoModel, oldCount: Int): List<KmpChapterModel> =
    info.chapters.take((info.chapters.size - oldCount).coerceAtLeast(0))
