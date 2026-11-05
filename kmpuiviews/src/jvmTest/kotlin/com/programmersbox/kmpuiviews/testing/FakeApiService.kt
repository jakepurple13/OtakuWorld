package com.programmersbox.kmpuiviews.testing

import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/**
 * A [KmpApiService] whose recent list and item info are scripted per test.
 *
 * [recentUrls] is returned from [recent] unless [recentError] is set, in which case it is thrown.
 * [onItemInfo] produces the info for a fetched item (or throws / suspends).
 */
class FakeApiService(
    override val serviceName: String = "FakeService",
    private val recentUrls: List<String> = emptyList(),
    private val recentError: Throwable? = null,
    private val onItemInfo: suspend FakeApiService.(KmpItemModel) -> KmpInfoModel = { infoModel(it.url, chapters = 0) },
) : KmpApiService {
    override val baseUrl: String = "https://$serviceName.example"
    override val displayName: String = "$serviceName Display"

    /** Number of times [recent] was called. */
    val recentCalls = AtomicInteger(0)

    /** Urls passed to [itemInfo], in call order. */
    val fetchedUrls: MutableList<String> = Collections.synchronizedList(mutableListOf())

    override suspend fun recent(page: Int): List<KmpItemModel> {
        recentCalls.incrementAndGet()
        recentError?.let { throw it }
        return recentUrls.map { KmpItemModel(title = it, description = "", url = it, imageUrl = "", source = this) }
    }

    override suspend fun itemInfo(model: KmpItemModel): KmpInfoModel {
        fetchedUrls += model.url
        return onItemInfo(model)
    }

    fun infoModel(url: String, chapters: Int) = KmpInfoModel(
        title = url,
        description = "",
        url = url,
        imageUrl = "",
        chapters = List(chapters) { index ->
            KmpChapterModel(
                name = "Chapter ${chapters - index}",
                url = "$url/${chapters - index}",
                uploaded = "",
                sourceUrl = url,
                source = this,
            )
        },
        genres = emptyList(),
        alternativeNames = emptyList(),
        source = this,
    )
}

fun SourceRepository.register(vararg services: KmpApiService) {
    addSources(
        services.map {
            KmpSourceInformation(apiService = it, name = it.serviceName, icon = null, packageName = it.serviceName)
        }
    )
}
