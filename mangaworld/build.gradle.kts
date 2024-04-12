plugins {
    `otaku-application`
    id("androidx.navigation.safeargs.kotlin")
    id("com.mikepenz.aboutlibraries.plugin")
    `otaku-protobuf`
    alias(libs.plugins.ksp)
    //id("androidx.baselineprofile")
}

android {
    namespace = "com.programmersbox.mangaworld"

    defaultConfig {
        applicationId = "com.programmersbox.mangaworld"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefresh)
    implementation(libs.recyclerview)
    implementation(libs.profileinstaller)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    implementation(libs.preference)
    implementation(libs.bundles.firebaseCrashLibs)

    implementation(libs.fileChooser)

    implementation(projects.uiViews)
    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)
    implementation(projects.sourceUtilities)
    implementation(projects.imageloader)
    implementation(libs.duktape)
    implementation(libs.bundles.ziplineLibs)

    implementation(libs.glide)
    //baselineProfile(projects.mangaWorldbaselineprofile)
    ksp(libs.glideCompiler)
    // Excludes the support library because it"s already included by Glide.
    implementation(libs.glideRecyclerview) { isTransitive = false }

    implementation(libs.bundles.piasyLibs)
    implementation(libs.subsamplingImageView)

    implementation(libs.iconicsCore)
    implementation(Deps.materialTypeface)

    implementation(libs.coroutinesCore)
    implementation(libs.coroutinesAndroid)

    implementation(libs.bundles.roomLibs)
    ksp(libs.roomCompiler)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(libs.bundles.koinLibs)
    implementation(platform(libs.composePlatform))
    implementation(libs.bundles.compose)
    implementation(libs.coilGif)
    implementation(libs.bundles.datastoreLibs)

    implementation(libs.glideCompose)

    implementation(libs.zoomableModifier)
}
