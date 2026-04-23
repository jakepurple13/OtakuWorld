package com.programmersbox.kmpmodels

class ExampleService : KmpApiService {
    override val baseUrl: String
        get() = "https://example.com/"

    override suspend fun recent(page: Int): List<KmpItemModel> = listOf(
        KmpItemModel(
            title = "Example",
            description = "Example",
            url = "https://example.com/",
            imageUrl = "https://picsum.photos/200/300",
            source = this
        )
    )

    override suspend fun itemInfo(model: KmpItemModel): KmpInfoModel {
        return KmpInfoModel(
            title = "Example",
            description = "Example",
            url = "https://example.com/",
            imageUrl = "https://picsum.photos/200/300",
            chapters = listOf(
                KmpChapterModel(
                    name = "Example",
                    url = "https://example.com/",
                    uploaded = "Example",
                    sourceUrl = "https://example.com/",
                    source = this
                )
            ),
            source = this,
            genres = listOf("Example"),
            alternativeNames = listOf("Example"),
        )
    }

    override suspend fun chapterInfo(chapterModel: KmpChapterModel): List<KmpStorage> {
        return listOf(
            KmpStorage(
                source = chapterModel.url,
                link = "https://picsum.photos/200/300",
                filename = chapterModel.name,
                sub = "Example",
                quality = "Example",
            )
        )
    }

    companion object {
        fun getSourceInformation() = KmpSourceInformation(
            name = "Example",
            packageName = "com.example",
            apiService = ExampleService(),
            icon = null,
            catalog = null
        )
    }
}