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

    setFlavorDimensions(listOf(ProductFlavorTypes.dimension))
    productFlavors {
        ProductFlavorTypes.NoFirebase(this)
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

    implementation(libs.firebaseAuth)
    implementation(libs.playServices)

    implementation(androidx.browser.browser)
    implementation(libs.androidBrowserHelper)

    implementation(libs.reactiveNetwork)

    implementation(libs.bundles.koinLibs)

    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)

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

    implementation(libs.showMoreLess)
    implementation(libs.aboutLibrariesCore)
    implementation(libs.aboutLibrariesCompose)

    implementation(libs.glide)
    ksp(libs.glideCompiler)

    // Excludes the support library because it"s already included by Glide.
    implementation(libs.glideRecyclerview) { isTransitive = false }

    implementation(libs.workRuntime)

    implementation(libs.kotlinxSerialization)

    // Kotlin
    api(androidx.navigation.navigationUiKtx)
    api(androidx.navigation.navigationFragmentKtx)

    // Testing Navigation
    androidTestImplementation(libs.navTesting)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    val composeBom = platform(libs.composePlatform)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    implementation(libs.toolbarCompose)
    implementation(libs.lazyColumnScrollbar)
    implementation(libs.adaptive)
    implementation(libs.pagingCompose)
    implementation(libs.bundles.pagingLibs)
    implementation(libs.bundles.datastoreLibs)

    implementation(libs.bundles.ktorLibs)

    implementation(androidx.activity.activityKtx)

    //Multiplatform
    //implementation(projects.imageloader)
    api(libs.bundles.kamel)

    //Extension Loader
    api(projects.sharedutils.extensionloader)

    api(libs.haze)
    api(libs.hazeMaterials)

    implementation(libs.compose.collapsable)

    //implementation(libs.material.adaptive.navigation.suite)
    implementation(libs.materialAdaptive)
    implementation(libs.adaptive.layout.android)
    implementation(libs.adaptive.navigation.android)

    implementation(libs.dragselect)

    implementation(libs.bundles.firebaseCrashLibs)

    implementation(libs.sonner)
    implementation(libs.glideCompose)

    implementation(libs.material.kolor)

    implementation("com.vanniktech:blurhash:0.4.0-SNAPSHOT")
    ksp(libs.roomCompiler)

    implementation(projects.gemini)

    //TODO: Use this to check recomposition count on every screen
    //implementation("io.github.theapache64:rebugger:1.0.0-rc03")
}
