import com.android.build.api.dsl.ApplicationProductFlavor
import plugins.ProductFlavorTypes

plugins {
    id("otaku-manager-application")
    kotlin("android")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.mikepenz.aboutlibraries.plugin.android")
    id("kotlinx-serialization")
    `otaku-easylauncher`
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

        //TODO: Put these in the setup uris
        /*dualStringBuildConfig(
            "ACCOUNT_TYPE",
            "com.programmersbox.otaku.world"
        )*/

        dualStringBuildConfig(
            "AUTH_TOKEN_TYPE",
            "com.programmersbox.otaku.world.apps"
        )
    }

    productFlavors {
        ProductFlavorTypes.NoFirebase.edit(this) {
            setupUris(".noFirebase", Provider.NoFirebase)
            isDefault = true
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
            force(androidLibs.preference)
        }
    }
}

dependencies {
    implementation(androidLibs.material)
    implementation(androidLibs.constraintlayout)
    implementation(androidLibs.androidxWebkit)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(androidLibs.recyclerview)

    //implementation(projects.uiViews)

    implementation(commonLibs.bundles.roomLibs)
    ksp(commonLibs.roomCompiler)

    implementation(commonLibs.kotlinxSerialization)
    implementation(androidLibs.jsoup)
    implementation(androidLibs.preference) {
        isTransitive = true
    }
    implementation(platform(commonLibs.koin.bom))
    implementation(androidLibs.bundles.koinLibs)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
    val composeBom = platform(androidLibs.composePlatform)
    implementation(composeBom)
    implementation(androidLibs.bundles.compose)

    implementation(androidLibs.androidxWindow)

    implementation(commonLibs.androidx.navigation3.runtime)
    implementation(commonLibs.androidx.navigation3.ui)
    implementation(commonLibs.androidx.material3.navigation3)
    implementation(commonLibs.androidx.lifecycle.viewmodel.navigation3)

    implementation(commonLibs.qrose)

    implementation(commonLibs.ktorCore)
    implementation(commonLibs.ktorAndroid)
    implementation(commonLibs.ktorAuth)
    implementation(commonLibs.ktorLogging)
    implementation(commonLibs.ktorSerialization)
    implementation(commonLibs.ktorJson)
    implementation(commonLibs.ktorContentNegotiation)

    implementation(commonLibs.bundles.datastoreLibs)
    implementation(androidLibs.biometric)
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
    private val incognitoUri = "provider.incognito"
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

    fun incognitoUri(
        builder: OtakuBuilder.() -> Unit,
    ) = OtakuBuilder()
        .apply(builder)
        .build() + ".$incognitoUri"
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

    dualStringBuildConfig(
        "ACCOUNT_TYPE",
        "com.programmersbox.otaku.world$suffix"
    )

    App.entries.forEach { app ->
        println("Setting up $app")
        println("Putting ${"${app.name.lowercase()}Package"} into manifest as ${"com.programmersbox.${app.name.lowercase()}$suffix"}")
        manifestPlaceholders["${app.name.lowercase()}Package"] = "com.programmersbox.${app.name.lowercase()}$suffix"

        println(
            "Putting ${"${app.name.lowercase()}_FAVORITES_PERMISSION"} into manifest as ${
                otakuProvider.favoritesPermissions {
                    appType = app
                    provider = productFlavor
                }
            }"
        )

        manifestPlaceholders["${app.name.lowercase()}_FAVORITES_PERMISSION"] = otakuProvider.favoritesPermissions {
            appType = app
            provider = productFlavor
        }

        println(
            "Putting ${"${app.name.lowercase()}_LISTS_PERMISSION"} into manifest as ${
                otakuProvider.listPermissions {
                    appType = app
                    provider = productFlavor
                }
            }"
        )

        manifestPlaceholders["${app.name.lowercase()}_LISTS_PERMISSION"] = otakuProvider.listPermissions {
            appType = app
            provider = productFlavor
        }

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

        dualStringBuildConfig(
            "${app.name}_INCOGNITO_URI",
            otakuProvider.incognitoUri {
                appType = app
                provider = productFlavor
            }
        )
    }
}