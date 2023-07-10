plugins {
    id("otaku-application")
    kotlin("kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.mikepenz.aboutlibraries.plugin")
    id("otaku-easylauncher")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.programmersbox.animeworld"

    defaultConfig {
        applicationId = "com.programmersbox.animeworld"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.preference)
    implementation(libs.bundles.firebaseCrashLibs)
    implementation(libs.recyclerview)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(libs.lottie)
    implementation(libs.fileChooser)
    implementation(libs.slideToAct)

    implementation(libs.mediaRouter)

    implementation(libs.torrentStream)

    implementation(libs.gson)

    implementation(libs.iconicsCore)
    implementation(Deps.materialTypeface)
    implementation(Deps.fontawesomeTypeface)

    implementation(projects.uiViews)
    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(projects.animeSources)
    implementation(projects.sharedutils)

    implementation(libs.bundles.roomLibs)
    ksp(libs.roomCompiler)

    implementation(libs.autoBindings)
    kapt(libs.autoBindingsCompiler)

    implementation(libs.castFramework)
    implementation(libs.localCast)

    implementation(libs.glide)
    ksp(libs.glideCompiler)

    // Excludes the support library because it"s already included by Glide.
    implementation(libs.glideRecyclerview) { isTransitive = false }

    implementation(libs.superForwardView)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
    implementation(libs.bundles.koinLibs)
    val composeBom = platform(libs.composePlatform)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    implementation(libs.coilVideo)
    implementation(libs.composeViewBinding)
    implementation(libs.bundles.datastoreLibs)

    implementation(libs.bundles.media3)
}
