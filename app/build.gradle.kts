import com.android.build.api.dsl.ApplicationProductFlavor
import plugins.ProductFlavorTypes

plugins {
    id("otaku-manager-application")
    kotlin("android")
    id("com.mikepenz.aboutlibraries.plugin")
    id("kotlinx-serialization")
    alias(libs.plugins.ksp)
}

//This is just to show what the minimum is needed to create a new app

android {
    defaultConfig {
        applicationId = "com.programmersbox.otakuworld"

        fun dualStringBuildConfig(
            key: String,
            value: String,
        ) {
            buildConfigField("String", key, "\"$value\"")
            resValue("string", key, value)
        }

        dualStringBuildConfig(
            "ACCOUNT_TYPE",
            "com.programmersbox.otaku.world"
        )

        dualStringBuildConfig(
            "AUTH_TOKEN_TYPE",
            "com.programmersbox.otaku.world.apps"
        )
    }

    productFlavors {
        ProductFlavorTypes.NoFirebase.edit(this) {
            setupUris(".noFirebase", Provider.NoFirebase)
        }
        ProductFlavorTypes.NoCloudFirebase.edit(this) {
            setupUris(".noCloudFirebase", Provider.NoCloudFirebase)
        }
        ProductFlavorTypes.Full.edit(this) {
            setupUris("", Provider.Full)
        }
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        compose = true
    }

    namespace = "com.programmersbox.otakuworld"

    configurations.all {
        resolutionStrategy {
            force(libs.preference)
        }
    }
}

dependencies {
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.androidxWebkit)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(libs.recyclerview)

    //implementation(projects.uiViews)
    implementation(projects.models)
    implementation(projects.favoritesdatabase)

    implementation(libs.bundles.roomLibs)
    ksp(libs.roomCompiler)

    implementation(libs.kotlinxSerialization)
    implementation(libs.jsoup)
    implementation(libs.preference) {
        isTransitive = true
    }
    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koinLibs)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
    val composeBom = platform(libs.composePlatform)
    implementation(composeBom)
    implementation(libs.bundles.compose)

    implementation(libs.androidxWindow)
}

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
    private val favoritesUri = "provider.favorites"
    private val listsUri = "provider.customlist"
    private val favoritePermissions = "READ_WRITE_FAVORITES"
    private val listPermissions = "READ_WRITE_LISTS"

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
}

class OtakuBuilder {
    private val mangaWorldPackageName = "com.programmersbox.mangaworld"
    private val animeWorldPackageName = "com.programmersbox.animeworld"
    private val novelWorldPackageName = "com.programmersbox.novelworld"
    private val noCloudFirebaseSuffix = ".noCloudFirebase"
    private val noFirebaseSuffix = ".noFirebase"
    private val fullSuffix = ""

    private var packageName = ""
    private var suffix = ""

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

fun ApplicationProductFlavor.setupUris(
    suffix: String,
    productFlavor: Provider,
) {
    fun dualStringBuildConfig(
        key: String,
        value: String,
    ) {
        buildConfigField("String", key, "\"$value\"")
        resValue("string", key, value)
    }

    val otakuProvider = OtakuProvider()

    App.entries.forEach { app ->
        manifestPlaceholders.putAll(
            mapOf(
                "mangaworldPackage" to "com.programmersbox.${app.name.lowercase()}$suffix",
                "animeworldPackage" to "com.programmersbox.${app.name.lowercase()}$suffix",
                "novelworldPackage" to "com.programmersbox.${app.name.lowercase()}$suffix",
            )
        )

        dualStringBuildConfig(
            "${app.name.uppercase()}_PACKAGE",
            "com.programmersbox.${app.name.lowercase()}$suffix"
        )

        dualStringBuildConfig(
            "${app.name}_FAVORITES_URI",
            otakuProvider.favoritesUri {
                appType = app
                provider = productFlavor
            }
        )

        dualStringBuildConfig(
            "${app.name}_LISTS_URI",
            otakuProvider.listsUri {
                appType = app
                provider = productFlavor
            }
        )

        dualStringBuildConfig(
            "${app.name}_FAVORITES_PERMISSION",
            otakuProvider.favoritesPermissions {
                appType = app
                provider = productFlavor
            }
        )

        dualStringBuildConfig(
            "${app.name}_LISTS_PERMISSION",
            otakuProvider.listPermissions {
                appType = app
                provider = productFlavor
            }
        )
    }
}