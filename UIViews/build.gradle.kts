import plugins.ProductFlavorTypes

plugins {
    `otaku-library`
    `otaku-protobuf`
    id("androidx.navigation.safeargs.kotlin")
    id("kotlinx-serialization")
    `kotlin-parcelize`
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
        ProductFlavorTypes.NoCloudFirebase(this)
        ProductFlavorTypes.Full(this)
    }
    namespace = "com.programmersbox.uiviews"
}

dependencies {
    implementation(libs.material)
    implementation(androidx.legacy.legacySupportV4)
    implementation(androidx.preference.preferenceKtx)
    implementation(androidx.window.window)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(platform(libs.firebasePlatform))
    implementation(libs.firebaseAuth)
    implementation(libs.playServices)
    implementation(libs.bundles.firebaseCrashLibs)

    api(platform(libs.koin.bom))
    api(libs.bundles.koinLibs)

    implementation(projects.kmpmodels)
    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)
    api(projects.datastore)
    api(projects.kmpuiviews)

    //Extension Loader
    api(projects.sharedutils.kmpextensionloader)

    implementation(androidx.constraintlayout.constraintlayout)
    implementation(libs.coroutinesCore)
    implementation(libs.coroutinesAndroid)
    implementation(androidx.fragment.fragmentKtx)
    implementation(androidx.lifecycle.lifecycleExtensions)
    implementation(androidx.lifecycle.lifecycleRuntimeKtx)
    implementation(androidx.lifecycle.lifecycleLivedataKtx)
    implementation(androidx.lifecycle.lifecycleViewmodelKtx)

    implementation(libs.iconicsCore)
    implementation(Deps.materialTypeface)
    implementation(Deps.fontawesomeTypeface)

    implementation(libs.gson)

    implementation(libs.recyclerview)
    //implementation(libs.palette)
    implementation(libs.bundles.roomLibs)

    implementation(libs.glide)
    ksp(libs.glideCompiler)

    // Excludes the support library because it"s already included by Glide.
    implementation(libs.glideRecyclerview) { isTransitive = false }

    api(libs.workRuntime)

    implementation(libs.kotlinxSerialization)

    // Testing Navigation
    androidTestImplementation(libs.navTesting)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    val composeBom = platform(libs.composePlatform)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    implementation(libs.adaptive)
    implementation(libs.bundles.datastoreLibs)

    implementation(libs.bundles.ktorLibs)

    implementation(androidx.activity.activityKtx)

    //Multiplatform
    //implementation(projects.imageloader)
    api(libs.bundles.kamel)

    api(libs.haze)
    api(libs.hazeMaterials)

    implementation(libs.compose.collapsable)

    //implementation(libs.material.adaptive.navigation.suite)
    implementation(libs.materialAdaptive)
    implementation(libs.adaptive.layout.android)
    implementation(libs.adaptive.navigation.android)

    implementation(libs.glideCompose)

    implementation(libs.material.kolor)

    implementation(libs.blurhash)
    ksp(libs.roomCompiler)

    //implementation(projects.gemini)

    debugImplementation(libs.workinspector)

    //implementation(libs.bundles.xr)

    //TODO: Use this to check recomposition count on every screen
    //implementation("io.github.theapache64:rebugger:1.0.0-rc03")

    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.core.performance)

    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs.compose)

    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)
    api(libs.androidx.material3.navigation3)
    api(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.composeActivity)
    implementation(libs.androidx.activity)
}
