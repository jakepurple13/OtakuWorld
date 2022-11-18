plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.starter.easylauncher") version "6.1.0"
}

android {
    compileSdk = AppInfo.compileVersion
    buildToolsVersion = AppInfo.buildVersion

    defaultConfig {
        applicationId = "com.programmersbox.animeworldtv"
        minSdk = AppInfo.minimumSdk
        targetSdk = AppInfo.targetSdk
        versionCode = 1
        versionName = AppInfo.otakuVersionName
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

    setFlavorDimensions(listOf("version"))
    productFlavors {
        create("noFirebase") {
            dimension = "version"
        }
        create("full") {
            dimension = "version"
        }
    }
    namespace = "com.programmersbox.animeworldtv"

    configurations.all {
        resolutionStrategy.force("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    }
}

dependencies {
    implementation(libs.kotlinStLib)
    implementation(libs.androidCore)
    implementation(libs.appCompat)
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
}

apply(from = "$rootDir/buildtypes.gradle")