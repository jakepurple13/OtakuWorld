package com.programmersbox.mangaworld

import androidx.core.content.FileProvider
import com.programmersbox.uiviews.OtakuApp
import org.koin.core.module.Module
import org.koin.dsl.module

class MangaApp : OtakuApp() {
    override val buildModules: Module = module { includes(appModule) }

    override fun createFirebaseIds(): FirebaseIds = FirebaseIds(
        documentId = "favoriteManga",
        chaptersId = "chaptersRead",
        collectionId = "mangaworld",
        itemId = "mangaUrl",
        readOrWatchedId = "chapterCount",
    )
}

class GenericFileProvider : FileProvider()