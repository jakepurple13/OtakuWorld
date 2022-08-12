plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
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

    configurations.all {
        resolutionStrategy.force("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    }
}

dependencies {
    implementation(Deps.kotlinStLib)
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    implementation(AnimeWorldDeps.leanbackLibs)
    implementation(Deps.glide)
    kapt(Deps.glideCompiler)
    implementation(Deps.androidxLegacySupport)
    implementation(Deps.material)
    implementation(Deps.constraintlayout)
    implementation(Deps.firebaseCrashLibs)
    implementation(Deps.firebaseAuth)
    implementation(Deps.playServices)
    implementation(Deps.palette)
    implementation(Media3Deps.exoplayerLibs)
    // For building media playback UIs for Android TV using the Jetpack Leanback library
    implementation(Media3Deps.leanback)

    implementation(project(":Models"))
    implementation(project(":anime_sources"))
    implementation(project(":favoritesdatabase"))
    implementation(project(":sharedutils"))

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
    implementation(Deps.koinAndroid)
    implementation(Deps.roomLibs)
}
