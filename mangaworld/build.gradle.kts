plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.starter.easylauncher") version "5.1.2"
}

android {
    compileSdk = AppInfo.compileVersion
    buildToolsVersion = AppInfo.buildVersion

    defaultConfig {
        applicationId = "com.programmersbox.mangaworld"
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
    kapt(libs.glideCompiler)
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
    // For Kotlin use kapt instead of annotationProcessor
    kapt(libs.roomCompiler)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(libs.bundles.koinLibs)
    implementation(libs.bundles.compose)
    implementation(libs.composePager)
    implementation(libs.swiperefresh)
    implementation(libs.coilGif)
    implementation(libs.bundles.datastoreLibs)

}

apply(from = "$rootDir/buildtypes.gradle")