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
        applicationId = "com.programmersbox.novelworld"
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
    implementation(Deps.recyclerview)
    implementation(Deps.preference)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(Deps.iconicsCore)
    implementation(Deps.materialTypeface)
    implementation(Deps.googlePlayAds)

    implementation(project(":UIViews"))
    implementation(project(":Models"))
    implementation(project(":favoritesdatabase"))
    implementation(project(":novel_sources"))
    implementation(project(":sharedutils"))

    implementation(Deps.roomLibs)
    // For Kotlin use kapt instead of annotationProcessor
    kapt(Deps.roomCompiler)

    implementation(Deps.firebaseCrashLibs)
    implementation(Deps.composeLibs)
    implementation(Deps.swipeRefresh)

    implementation(Deps.datastoreLibs)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(Deps.koinLibs)
}

apply(from = "$rootDir/buildtypes.gradle")