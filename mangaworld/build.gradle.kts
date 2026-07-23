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
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
        getByName("beta") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }
}

dependencies {
    implementation(androidLibs.material)
    implementation(androidx.constraintlayout.constraintlayout)
    implementation(androidx.swiperefreshlayout.swiperefreshlayout)
    implementation(androidx.recyclerview.recyclerview)
    implementation(androidx.preference.preferenceKtx)
    implementation(androidx.profileinstaller.profileinstaller)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    implementation(platform(androidLibs.firebasePlatform))
    implementation(androidLibs.bundles.firebaseCrashLibs)

    implementation(androidLibs.fileChooser)

    implementation(projects.uiViews)
    implementation(projects.kmpmodels)
    implementation(projects.favoritesdatabase)
    implementation(projects.favoritesdatabase.supabaseIntegration)
    implementation(projects.sharedutils)
    implementation(projects.sourceUtilities)
    implementation(projects.datastore.mangasettings)
    implementation(projects.mangaworld.shared)

    implementation(commonLibs.kamel.image)
    implementation(androidLibs.bundles.ziplineLibs)
    implementation(commonLibs.ktorAndroid)

    implementation(androidLibs.glide)
    baselineProfile(projects.mangaWorldbaselineprofile)
    ksp(androidLibs.glideCompiler)
    // Excludes the support library because it"s already included by Glide.
    implementation(androidLibs.glideRecyclerview) { isTransitive = false }

    implementation(androidLibs.bundles.piasyLibs)

    implementation(androidLibs.iconicsCore)
    implementation(Deps.materialTypeface)

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

    implementation(commonLibs.sonner)

    implementation(commonLibs.lifecycle.viewmodel.compose)

    //debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}
