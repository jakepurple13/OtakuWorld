import plugins.ProductFlavorTypes

plugins {
    `otaku-library`
    `otaku-protobuf`
    id("androidx.navigation.safeargs.kotlin")
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

    setFlavorDimensions(listOf(ProductFlavorTypes.dimension))
    productFlavors {
        ProductFlavorTypes.NoFirebase(this)
        ProductFlavorTypes.Full(this)
    }
    namespace = "com.programmersbox.uiviews"
}

dependencies {
    implementation(libs.material)
    implementation(libs.androidxLegacySupport)
    implementation(libs.preference)
    implementation(libs.androidxWindow)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(libs.firebaseAuth)
    implementation(libs.playServices)

    implementation(libs.androidBrowserHelper)
    implementation(libs.androidxBrowser)

    implementation(libs.reactiveNetwork)

    implementation(libs.bundles.koinLibs)

    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)

    implementation(libs.constraintlayout)
    implementation(libs.coroutinesCore)
    implementation(libs.coroutinesAndroid)
    implementation(libs.fragmentKtx)
    implementation(libs.lifecycleExtensions)
    implementation(libs.lifecycleRuntime)
    implementation(libs.lifecycleLivedata)
    implementation(libs.lifecycleViewModel)

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
    api(libs.navFragment)
    api(libs.navUiKtx)

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

    implementation(libs.androidx.activity.ktx)

    //Multiplatform
    //implementation(projects.imageloader)
    api(libs.kamel.image)

    //Extension Loader
    api(projects.sharedutils.extensionloader)

    api(libs.haze)

    implementation(libs.compose.collapsable)

    //implementation(libs.material.adaptive.navigation.suite)
    implementation(libs.materialAdaptive)
    implementation(libs.adaptive.layout.android)
    implementation(libs.adaptive.navigation.android)

    implementation(libs.dragselect)

    implementation(libs.bundles.firebaseCrashLibs)

    implementation(libs.sonner)
    implementation(libs.glideCompose)
}
