plugins {
    id("otaku-application")
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("otaku-easylauncher")
}

android {
    defaultConfig {
        applicationId = "com.programmersbox.animeworldtv"
    }

    namespace = "com.programmersbox.animeworldtv"

    configurations.all {
        resolutionStrategy.force(libs.lifecycleViewModel)
    }
}

dependencies {
    implementation(libs.bundles.leanbackLibs)
    implementation(libs.glide)
    kapt(libs.glideCompiler)
    implementation(libs.androidxLegacySupport)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.bundles.firebaseCrashLibs)
    implementation(libs.firebaseAuth)
    implementation(libs.playServices)
    implementation(libs.palette)
    implementation(libs.bundles.media3)
    // For building media playback UIs for Android TV using the Jetpack Leanback library
    implementation(libs.exoplayerleanback)

    implementation(projects.models)
    implementation(projects.animeSources)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
    implementation(libs.koinAndroid)
    implementation(libs.bundles.roomLibs)
    implementation(libs.gson)

    implementation(platform(libs.composePlatform))
    implementation(libs.bundles.composeTv)
    implementation(libs.coilGif)
    implementation("androidx.tv:tv-foundation:1.0.0-alpha07")
    implementation("androidx.tv:tv-material:1.0.0-alpha07")
}
