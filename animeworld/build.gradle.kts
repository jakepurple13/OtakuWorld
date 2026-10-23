plugins {
    `otaku-application`
    kotlin("kapt")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.mikepenz.aboutlibraries.plugin.android")
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.firebase.performance)
    id("androidx.baselineprofile")
}

android {
    namespace = "com.programmersbox.animeworld"

    defaultConfig {
        applicationId = "com.programmersbox.animeworld"

        providers.gradleProperty("releaseAbis").orNull?.let { abis ->
            ndk { abiFilters += abis.split(",") }
        }
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            //For testing
            if (System.getenv("CI") == null)
                signingConfig = signingConfigs.getByName("debug")
        }
    }

    // The APK is sideloaded from GitHub, so compress dex and native libs to shrink the download.
    // Costs extra install-time extraction and on-disk space.
    packaging {
        dex.useLegacyPackaging = true
        jniLibs.useLegacyPackaging = true
    }
}

dependencies {
    implementation(androidLibs.material)
    implementation(androidx.constraintlayout.constraintlayout)
    implementation(androidx.preference.preferenceKtx)
    implementation(platform(androidLibs.firebasePlatform))
    implementation(androidLibs.bundles.firebaseCrashLibs)
    implementation(androidx.recyclerview.recyclerview)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(androidLibs.fileChooser)
    implementation(androidLibs.slideToAct)

    implementation(androidx.mediarouter.mediarouter)

    //Commenting out since it's no longer being worked on
    //implementation(libs.torrentStream)

    implementation(androidLibs.gson)

    implementation(androidLibs.iconicsCore)
    implementation(Deps.materialTypeface)
    implementation(Deps.fontawesomeTypeface)

    implementation(projects.uiViews)
    implementation(projects.kmpmodels)
    implementation(projects.sourceUtilities.aniyomi)
    implementation(projects.sourceUtilities.cloudstream)
    implementation(androidLibs.fragmentCompose)
    implementation(projects.favoritesdatabase)
    implementation(projects.favoritesdatabase.supabaseIntegration)
    implementation(projects.sharedutils)

    implementation(commonLibs.bundles.roomLibs)
    ksp(commonLibs.roomCompiler)

    implementation(androidLibs.autoBindings)
    kapt(androidLibs.autoBindingsCompiler)

    implementation(androidLibs.castFramework)

    implementation(androidLibs.glide)
    ksp(androidLibs.glideCompiler)

    // Excludes the support library because it"s already included by Glide.
    implementation(androidLibs.glideRecyclerview) { isTransitive = false }

    //implementation(libs.superForwardView)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
    val composeBom = platform(androidLibs.composePlatform)
    implementation(composeBom)
    implementation(androidLibs.bundles.compose)
    implementation(androidLibs.composeViewBinding)
    implementation(commonLibs.bundles.datastoreLibs)

    implementation(androidLibs.bundles.media3)

    implementation(commonLibs.ktorAndroid)

    implementation(projects.animeworld.shared)

    implementation(androidx.profileinstaller.profileinstaller)
    baselineProfile(projects.animeWorldbaselineprofile)
}
