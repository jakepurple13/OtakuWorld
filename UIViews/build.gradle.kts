import plugins.ProductFlavorTypes

plugins {
    `otaku-library`
    id("kotlinx-serialization")
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        buildConfigField(
            type = "Boolean",
            name = "IS_PRERELEASE",
            value = runCatching { System.getenv("IS_PRERELEASE") }
                .onFailure { it.printStackTrace() }
                .mapCatching { it.toBoolean() }
                .getOrDefault(false)
                .toString()
                .also { println("IS_PRERELEASE: $it") }
        )
    }

    setFlavorDimensions(listOf(ProductFlavorTypes.dimension))
    productFlavors {
        ProductFlavorTypes.NoFirebase(this)
        ProductFlavorTypes.Full(this)
    }
    namespace = "com.programmersbox.uiviews"
}

dependencies {
    implementation(androidLibs.material)
    implementation(androidx.legacy.legacySupportV4)
    implementation(androidx.preference.preferenceKtx)
    implementation(androidx.window.window)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(platform(androidLibs.firebasePlatform))
    implementation(androidLibs.bundles.firebaseCrashLibs)

    api(platform(commonLibs.koin.bom))
    api(androidLibs.bundles.koinLibs)

    implementation(projects.kmpmodels)
    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)
    api(projects.datastore)
    api(projects.kmpuiviews)

    //Extension Loader
    api(projects.sharedutils.kmpextensionloader)

    implementation(androidx.constraintlayout.constraintlayout)
    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.coroutinesAndroid)
    implementation(androidx.fragment.fragmentKtx)
    implementation(androidx.lifecycle.lifecycleExtensions)
    implementation(androidx.lifecycle.lifecycleRuntimeKtx)
    implementation(androidx.lifecycle.lifecycleLivedataKtx)
    implementation(androidx.lifecycle.lifecycleViewmodelKtx)

    implementation(androidLibs.iconicsCore)
    implementation(Deps.materialTypeface)
    implementation(Deps.fontawesomeTypeface)

    implementation(androidLibs.gson)

    implementation(androidLibs.recyclerview)
    //implementation(libs.palette)
    implementation(commonLibs.bundles.roomLibs)

    implementation(androidLibs.glide)
    ksp(androidLibs.glideCompiler)

    // Excludes the support library because it"s already included by Glide.
    implementation(androidLibs.glideRecyclerview) { isTransitive = false }

    api(androidLibs.workRuntime)

    implementation(commonLibs.kotlinxSerialization)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    val composeBom = platform(androidLibs.composePlatform)
    implementation(composeBom)
    implementation(androidLibs.bundles.compose)
    implementation(androidLibs.adaptive)
    implementation(commonLibs.bundles.datastoreLibs)

    implementation(commonLibs.bundles.ktorLibs)

    implementation(androidx.activity.activityKtx)

    //Multiplatform
    //implementation(projects.imageloader)
    api(commonLibs.bundles.kamel)

    api(commonLibs.haze)
    api(commonLibs.haze.blur)
    api(commonLibs.haze.materials)

    implementation(androidLibs.composeCollapsable)

    //implementation(libs.material.adaptive.navigation.suite)
    implementation(androidLibs.materialAdaptive)
    implementation(androidLibs.adaptive.layout.android)
    implementation(androidLibs.adaptive.navigation.android)

    implementation(androidLibs.glideCompose)

    implementation(commonLibs.material.kolor)

    //implementation(libs.blurhash)

    //implementation(projects.gemini)

    debugImplementation(androidLibs.workinspector)

    //implementation(libs.bundles.xr)

    //TODO: Use this to check recomposition count on every screen
    //implementation("io.github.theapache64:rebugger:1.0.0-rc03")

    implementation(commonLibs.kotlinx.datetime)

    implementation(androidLibs.androidx.core.performance)

    implementation(commonLibs.filekit.core)
    implementation(commonLibs.filekit.dialogs.compose)

    api(commonLibs.androidx.navigation3.runtime)
    api(commonLibs.androidx.navigation3.ui)
    api(commonLibs.androidx.material3.navigation3)
    api(commonLibs.androidx.lifecycle.viewmodel.navigation3)
    implementation(androidLibs.androidx.activity.ktx)
    implementation(androidLibs.composeActivity)
    implementation(androidLibs.androidx.activity)
}
