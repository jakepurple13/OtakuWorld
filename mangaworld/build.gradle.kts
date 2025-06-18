plugins {
    `otaku-application`
    id("androidx.navigation.safeargs.kotlin")
    id("com.mikepenz.aboutlibraries.plugin")
    `otaku-protobuf`
    alias(libs.plugins.ksp)
    id("kotlinx-serialization")
    //id("androidx.baselineprofile")
    alias(libs.plugins.google.firebase.performance)
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
    implementation(androidx.constraintlayout.constraintlayout)
    implementation(androidx.swiperefreshlayout.swiperefreshlayout)
    implementation(androidx.recyclerview.recyclerview)
    implementation(androidx.preference.preferenceKtx)
    implementation(androidx.profileinstaller.profileinstaller)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    implementation(platform(libs.firebasePlatform))
    implementation(libs.bundles.firebaseCrashLibs)

    implementation(libs.fileChooser)

    implementation(projects.uiViews)
    implementation(projects.kmpmodels)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)
    implementation(projects.sourceUtilities)
    implementation(projects.datastore.mangasettings)
    implementation(projects.mangaworld.shared)

    implementation(libs.kamel.image)
    implementation(libs.duktape)
    implementation(libs.bundles.ziplineLibs)
    implementation(libs.ktorAndroid)

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

    implementation(platform(libs.composePlatform))
    implementation(libs.bundles.compose)
    implementation(libs.coilGif)

    implementation(androidx.datastore.datastore)
    implementation(androidx.datastore.datastorePreferences)

    implementation(libs.glideCompose)

    implementation(libs.zoomableModifier)

    implementation(libs.pagecurl)

    implementation(libs.panpf.zoomimage.compose.glide)

    implementation(libs.telephoto.zoomable.image.glide)

    implementation(libs.sonner)

    implementation(libs.lifecycle.viewmodel.compose)

    //debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}
