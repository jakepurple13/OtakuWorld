plugins {
    `otaku-application`
    id("androidx.navigation.safeargs.kotlin")
    id("com.mikepenz.aboutlibraries.plugin")
    `otaku-protobuf`
    alias(libs.plugins.ksp)
    id("kotlinx-serialization")
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

composeCompiler {
    enableStrongSkippingMode = true
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
    implementation(libs.bundles.firebaseCrashLibs)

    implementation(libs.fileChooser)

    implementation(projects.uiViews)
    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)
    implementation(projects.sourceUtilities)
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

    implementation(libs.bundles.koinLibs)
    implementation(platform(libs.composePlatform))
    implementation(libs.bundles.compose)
    implementation(libs.coilGif)

    implementation(androidx.datastore.datastore)
    implementation(androidx.datastore.datastorePreferences)

    implementation(libs.glideCompose)

    implementation(libs.zoomableModifier)

    implementation("io.github.oleksandrbalan:pagecurl:1.5.1")

    implementation("io.github.panpf.zoomimage:zoomimage-compose-glide:1.0.2")

    implementation("me.saket.telephoto:zoomable-image-glide:0.11.2")
}
