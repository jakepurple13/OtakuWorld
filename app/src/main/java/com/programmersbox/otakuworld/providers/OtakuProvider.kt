package com.programmersbox.otakuworld.providers

import kotlinx.serialization.Serializable
import kotlin.properties.Delegates

private const val favoritesUri = "provider.favorites"
private const val listsUri = "provider.customlist"

private const val mangaWorldPackageName = "com.programmersbox.mangaworld"
private const val animeWorldPackageName = "com.programmersbox.animeworld"
private const val novelWorldPackageName = "com.programmersbox.novelworld"

private const val noCloudFirebaseSuffix = ".noCloudFirebase"
private const val noFirebaseSuffix = ".noFirebase"
private const val fullSuffix = ""

private const val favoritePermissions = "READ_WRITE_FAVORITES"
private const val listPermissions = "READ_WRITE_LISTS"

@Serializable
enum class App {
    MangaWorld,
    AnimeWorld,
    NovelWorld
}

enum class Provider {
    NoCloudFirebase,
    NoFirebase,
    Full
}

class OtakuProvider {
    fun favoritesBuilder(
        builder: OtakuBuilder.() -> Unit,
    ) = OtakuFavoritesContentProviderHelper(
        OtakuBuilder()
            .apply(builder)
            .build() + ".$favoritesUri"
    )

    fun favoritesUri(
        builder: OtakuBuilder.() -> Unit,
    ) = OtakuBuilder()
        .apply(builder)
        .build() + ".$favoritesUri"

    fun favoritesPermissions(
        builder: OtakuBuilder.() -> Unit,
    ) = OtakuBuilder()
        .apply(builder)
        .build() + ".$favoritePermissions"

    fun listsBuilder(
        builder: OtakuBuilder.() -> Unit,
    ) = OtakuCustomListContentProviderHelper(
        OtakuBuilder()
            .apply(builder)
            .build() + ".$listsUri"
    )

    fun listsUri(
        builder: OtakuBuilder.() -> Unit,
    ) = OtakuBuilder()
        .apply(builder)
        .build() + ".$listsUri"

    fun listPermissions(
        builder: OtakuBuilder.() -> Unit,
    ) = OtakuBuilder()
        .apply(builder)
        .build() + ".$listPermissions"

    class OtakuBuilder {
        private var packageName by Delegates.notNull<String>()
        private var suffix by Delegates.notNull<String>()

        var appType: App
            get() = error("App type not set")
            set(value) {
                setPackage(value)
            }

        var provider: Provider
            get() = error("Provider not set")
            set(value) {
                setProvider(value)
            }

        fun setPackage(app: App) = apply {
            packageName = when (app) {
                App.MangaWorld -> mangaWorldPackageName
                App.AnimeWorld -> animeWorldPackageName
                App.NovelWorld -> novelWorldPackageName
            }
        }

        fun setProvider(provider: Provider) = apply {
            suffix = when (provider) {
                Provider.NoCloudFirebase -> noCloudFirebaseSuffix
                Provider.NoFirebase -> noFirebaseSuffix
                Provider.Full -> fullSuffix
            }
        }

        fun build() = "$packageName$suffix"
    }
}