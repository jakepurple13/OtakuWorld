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
    implementation(libs.googlePlayAds)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    implementation(libs.preference)
    implementation(libs.bundles.firebaseCrashLibs)

    implementation(libs.fileChooser)
    implementation(libs.storage)

    implementation(projects.uiViews)
    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(projects.mangaSources)
    implementation(projects.sharedutils)

    implementation(libs.glide)
    ksp(libs.glideCompiler)
    // Excludes the support library because it"s already included by Glide.
    implementation(libs.glideRecyclerview) { isTransitive = false }

    implementation(libs.bundles.piasyLibs)
    implementation(libs.subsamplingImageView)
    implementation(libs.subsamplingCompose)

    implementation(libs.iconicsCore)
    implementation(Deps.materialTypeface)

    implementation(libs.coroutinesCore)
    implementation(libs.coroutinesAndroid)

    implementation(libs.bundles.roomLibs)
    ksp(libs.roomCompiler)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(libs.bundles.koinLibs)
    val composeBom = platform(libs.composePlatform)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    implementation(libs.coilGif)
    implementation(libs.bundles.datastoreLibs)

    implementation(libs.glideCompose)
}
