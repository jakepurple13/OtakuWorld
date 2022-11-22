plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.starter.easylauncher") version "6.1.0"
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.programmersbox.animeworld"
    compileSdk = AppInfo.compileVersion
    buildToolsVersion = AppInfo.buildVersion

    defaultConfig {
        applicationId = "com.programmersbox.animeworld"
        minSdk = AppInfo.minimumSdk
        targetSdk = AppInfo.targetSdk
        versionCode = 1
        versionName = AppInfo.otakuVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        getByName("debug") {
            extra["enableCrashlytics"] = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.jetpackCompiler.get()
    }
}

dependencies {
    implementation(libs.kotlinStLib)
    implementation(libs.androidCore)
    implementation(libs.appCompat)
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
    implementation(libs.fetch)
    implementation(libs.fetchOkHttp)

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
    kapt(libs.glideCompiler)

    // Excludes the support library because it"s already included by Glide.
    implementation(libs.glideRecyclerview) { isTransitive = false }

    implementation(libs.superForwardView)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
    implementation(libs.bundles.koinLibs)
    implementation(libs.bundles.compose)
    implementation(libs.composeViewBinding)
    implementation(libs.bundles.datastoreLibs)

    implementation(libs.bundles.media3)
}

apply(from = "$rootDir/buildtypes.gradle")