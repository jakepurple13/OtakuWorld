import plugins.ApplicationBuildTypes
import plugins.ProductFlavorTypes

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("otaku-easylauncher")
}

android {
    compileSdk = AppInfo.compileVersion

    defaultConfig {
        applicationId = "com.programmersbox.animeworldtv"
        minSdk = AppInfo.minimumSdk
        targetSdk = AppInfo.targetSdk
        versionCode = 1
        versionName = AppInfo.otakuVersionName
    }

    buildTypes {
        ApplicationBuildTypes.Release.setup(this) {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        ApplicationBuildTypes.Debug.setup(this)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    setFlavorDimensions(listOf(ProductFlavorTypes.dimension))
    productFlavors {
        ProductFlavorTypes.NoFirebase(this)
        ProductFlavorTypes.Full(this)
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
}
