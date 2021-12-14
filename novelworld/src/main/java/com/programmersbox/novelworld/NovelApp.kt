package com.programmersbox.novelworld

import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.OtakuApp
import org.koin.core.context.loadKoinModules

class NovelApp : OtakuApp() {
    override fun onCreated() {

        loadKoinModules(appModule)

        FirebaseDb.DOCUMENT_ID = "favoriteNovels"
        FirebaseDb.CHAPTERS_ID = "novelsChaptersRead"
        FirebaseDb.COLLECTION_ID = "novelworld"
        FirebaseDb.ITEM_ID = "novelUrl"
        FirebaseDb.READ_OR_WATCHED_ID = "novelNumChapters"

        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(
                    listOf(
                        "06CDF88040C0DBAE35E0A89453F636C6",
                        "0F7FE45111A61A20D1EA4B517FA51744"
                    )
                )
                .build()
        )

    }
}