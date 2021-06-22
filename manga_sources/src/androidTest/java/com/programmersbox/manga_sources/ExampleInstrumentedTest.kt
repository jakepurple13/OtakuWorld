package com.programmersbox.manga_sources

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.programmersbox.manga_sources.manga.MangaPark
import com.programmersbox.manga_sources.utilities.NetworkHelper
import com.programmersbox.manga_sources.utilities.asJsoup
import com.programmersbox.manga_sources.utilities.cloudflare
import com.programmersbox.models.*
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.programmersbox.manga_sources.test", appContext.packageName)
    }

    @Test
    fun mangaparkv3Test() {

        val recent = MangaParkV3.getRecent(1).blockingGet()

        println(recent)

    }

    object MangaParkV3 : ApiService, KoinComponent {

        override val baseUrl = "https://www.mangapark.net"

        override fun getRecent(page: Int): Single<List<ItemModel>> = Single.create { emitter ->
            cloudflare(helper, "${MangaPark.baseUrl}/browse?sort=update&page=$page").execute().asJsoup()
                .also { println(it) }
                /*.select("div.ls1").select("div.d-flex, div.flex-row, div.item")
                .map {
                    ItemModel(
                        title = it.select("a.cover").attr("title"),
                        description = "",
                        url = "${MangaPark.baseUrl}${it.select("a.cover").attr("href")}",
                        imageUrl = it.select("a.cover").select("img").attr("abs:src"),
                        source = Sources.MANGA_PARK
                    )
                }
                .let { emitter.onSuccess(it) }*/
                .let { emitter.onSuccess(emptyList()) }

        }

        override fun getList(page: Int): Single<List<ItemModel>> {
            TODO("Not yet implemented")
        }

        override fun getItemInfo(model: ItemModel): Single<InfoModel> {
            TODO("Not yet implemented")
        }

        override fun getChapterInfo(chapterModel: ChapterModel): Single<List<Storage>> {
            TODO("Not yet implemented")
        }

        override val serviceName: String get() = "MANGA_PARK"

        private val helper: NetworkHelper by inject()

    }
}