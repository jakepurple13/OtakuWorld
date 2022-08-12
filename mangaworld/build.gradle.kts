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
        kotlinCompilerExtensionVersion = Deps.jetpackCompiler
    }
}

dependencies {
    implementation(Deps.kotlinStLib)
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    implementation(Deps.material)
    implementation(Deps.constraintlayout)
    implementation(Deps.swiperefresh)
    implementation(Deps.recyclerview)
    implementation(Deps.googlePlayAds)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    implementation(Deps.preference)
    implementation(Deps.firebaseCrashLibs)

    implementation(Deps.fileChooser)
    implementation(Deps.storage)

    implementation(project(":UIViews"))
    implementation(project(":Models"))
    implementation(project(":favoritesdatabase"))
    implementation(project(":manga_sources"))
    implementation(project(":sharedutils"))

    implementation(Deps.glide)
    kapt(Deps.glideCompiler)
    // Excludes the support library because it"s already included by Glide.
    implementation(Deps.glideRecyclerview) { isTransitive = false }

    implementation(MangaWorldDeps.piasyLibs)
    implementation(MangaWorldDeps.subsamplingImageView)
    implementation(MangaWorldDeps.subsamplingCompose)

    implementation(Deps.iconicsCore)
    implementation(Deps.materialTypeface)

    implementation(Deps.coroutinesCore)
    implementation(Deps.coroutinesAndroid)

    implementation(Deps.roomLibs)
    // For Kotlin use kapt instead of annotationProcessor
    kapt(Deps.roomCompiler)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(Deps.koinLibs)
    implementation(Deps.composeLibs)
    implementation(Deps.composePager)
    implementation(MangaWorldDeps.swiperefresh)
    implementation(MangaWorldDeps.coilGif)
    implementation(Deps.datastoreLibs)
}

apply(from = "$rootDir/buildtypes.gradle")