plugins {
    `otaku-application`
    kotlin("kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.mikepenz.aboutlibraries.plugin")
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.firebase.performance)
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
    implementation(androidx.constraintlayout.constraintlayout)
    implementation(androidx.preference.preferenceKtx)
    implementation(platform(libs.firebasePlatform))
    implementation(libs.bundles.firebaseCrashLibs)
    implementation(androidx.recyclerview.recyclerview)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(libs.fileChooser)
    implementation(libs.slideToAct)

    implementation(androidx.mediarouter.mediarouter)

    //Commenting out since it's no longer being worked on
    //implementation(libs.torrentStream)

    implementation(libs.gson)

    implementation(libs.iconicsCore)
    implementation(Deps.materialTypeface)
    implementation(Deps.fontawesomeTypeface)

    implementation(projects.uiViews)
    implementation(projects.models)
    implementation(projects.kmpmodels)
    implementation(projects.favoritesdatabase)
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

    //implementation(libs.superForwardView)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
    val composeBom = platform(libs.composePlatform)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    implementation(libs.coilVideo)
    implementation(libs.composeViewBinding)
    implementation(libs.bundles.datastoreLibs)

    implementation(libs.bundles.media3)

    implementation(libs.ktorAndroid)
}
