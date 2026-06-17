plugins {
    `otaku-application`
    kotlin("kapt")
}

android {
    defaultConfig {
        applicationId = "com.programmersbox.animeworldtv"
    }

    namespace = "com.programmersbox.animeworldtv"

    configurations.all {
        resolutionStrategy.force(androidLibs.lifecycleViewModel)
    }
}

dependencies {
    implementation(androidLibs.bundles.leanbackLibs)
    implementation(androidLibs.glide)
    kapt(androidLibs.glideCompiler)
    implementation(libs.androidxLegacySupport)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(platform(androidLibs.firebasePlatform))
    implementation(androidLibs.bundles.firebaseCrashLibs)
    implementation(androidLibs.firebaseAuth)
    implementation(androidLibs.playServices)
    //implementation(libs.palette)
    implementation(androidLibs.bundles.media3)
    // For building media playback UIs for Android TV using the Jetpack Leanback library
    implementation(androidLibs.exoplayerleanback)

    implementation(projects.models)
    implementation(projects.animeSources)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
    implementation(platform(commonLibs.koin.bom))
    implementation(androidLibs.koin.android)
    implementation(commonLibs.bundles.roomLibs)
    implementation(androidLibs.gson)

    implementation(platform(androidLibs.composePlatform))
    implementation(androidLibs.bundles.composeTv)
    implementation(androidLibs.coilGif)
    implementation(androidLibs.tv.foundation)
    implementation(androidLibs.tv.material)
}
