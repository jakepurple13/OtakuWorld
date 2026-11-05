package com.programmersbox.kmpuiviews.domain.updatecheck

import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel

/**
 * The chapters of [info] that are new since the favorite had [oldCount] chapters, newest first.
 * Sources disagree on list order, so [KmpChapterModel.uploadedTime] decides when every chapter has one;
 * otherwise the head of the list is taken as newest.
 */
fun newChapters(info: KmpInfoModel, oldCount: Int): List<KmpChapterModel> {
    val count = info.chapters.size - oldCount
    if (count <= 0) return emptyList()
    val chapters = info.chapters
    return if (chapters.all { it.uploadedTime != null }) {
        chapters.sortedByDescending { it.uploadedTime }.take(count)
    } else {
        chapters.take(count)
    }
}
