plugins {
    `otaku-application`
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.mikepenz.aboutlibraries.plugin.android")
    alias(libs.plugins.ksp)
    id("kotlinx-serialization")
    id("androidx.baselineprofile")
    alias(libs.plugins.google.firebase.performance)
    //alias(libs.plugins.hotswan.compiler)
}

android {
    namespace = "com.programmersbox.mangaworld"

    defaultConfig {
        applicationId = "com.programmersbox.mangaworld"
        // Published APKs pass -PreleaseAbis to drop emulator ABIs; local builds keep them all.
        providers.gradleProperty("releaseAbis").orNull?.let { abis ->
            ndk { abiFilters += abis.split(",") }
        }
    }

    buildTypes {
        getByName("beta") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
        // Windows console DLLs from jansi (pulled in by koog's ktor-server-sse); never loaded on Android.
        resources.excludes += "org/fusesource/jansi/**"
    }
}

dependencies {
    implementation(androidx.preference.preferenceKtx)
    implementation(androidx.profileinstaller.profileinstaller)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    implementation(platform(androidLibs.firebasePlatform))
    implementation(androidLibs.bundles.firebaseCrashLibs)

    implementation(projects.uiViews)
    implementation(projects.kmpmodels)
    implementation(projects.mihonsupport)
    implementation(androidLibs.fragmentCompose)
    implementation(projects.favoritesdatabase)
    implementation(projects.favoritesdatabase.supabaseIntegration)
    implementation(projects.sharedutils)
    implementation(projects.sourceUtilities)
    implementation(projects.datastore.mangasettings)
    implementation(projects.mangaworld.shared)

    implementation(commonLibs.kamel.image)
    implementation(androidLibs.duktape)
    implementation(androidLibs.bundles.ziplineLibs)
    implementation(commonLibs.ktorAndroid)

    implementation(androidLibs.glide)
    baselineProfile(projects.mangaWorldbaselineprofile)
    ksp(androidLibs.glideCompiler)

    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.coroutinesAndroid)

    implementation(commonLibs.bundles.roomLibs)
    ksp(commonLibs.roomCompiler)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(platform(androidLibs.composePlatform))
    implementation(androidLibs.bundles.compose)

    implementation(androidx.datastore.datastore)
    implementation(androidx.datastore.datastorePreferences)

    implementation(androidLibs.glideCompose)

    implementation(commonLibs.zoomableModifier)

    implementation(androidLibs.pagecurl)

    implementation(androidLibs.panpf.zoomimage.compose.glide)

    implementation(androidLibs.telephoto.zoomable.image.glide)

    implementation(commonLibs.sonner)

    implementation(commonLibs.lifecycle.viewmodel.compose)
    implementation(androidLibs.brotli)
    implementation(androidLibs.zstd)

    //debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}
