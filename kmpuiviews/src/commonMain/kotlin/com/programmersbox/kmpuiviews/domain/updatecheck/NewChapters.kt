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
        // On equal times keep list order: in an oldest-first list the later chapter is newer.
        val oldestFirst = chapters.first().uploadedTime!! < chapters.last().uploadedTime!!
        chapters.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<KmpChapterModel>> { it.value.uploadedTime }
                    .thenBy { if (oldestFirst) -it.index else it.index }
            )
            .map { it.value }
            .take(count)
    } else {
        chapters.take(count)
    }
}
