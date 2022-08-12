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
        kotlinCompilerExtensionVersion = Deps.jetpackCompiler
    }
}

dependencies {
    implementation(Deps.kotlinStLib)
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    implementation(Deps.material)
    implementation(Deps.constraintlayout)
    implementation(Deps.preference)
    implementation(Deps.firebaseCrashLibs)
    implementation(Deps.recyclerview)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(Deps.lottie)
    implementation(Deps.fileChooser)
    implementation(AnimeWorldDeps.slideToAct)

    implementation(AnimeWorldDeps.mediaRouter)
    implementation(AnimeWorldDeps.fetch)
    implementation(AnimeWorldDeps.fetchOkHttp)

    implementation(AnimeWorldDeps.torrentStream)

    implementation(Deps.gson)

    implementation(Deps.iconicsCore)
    implementation(Deps.materialTypeface)
    implementation(Deps.fontawesomeTypeface)

    implementation(project(":UIViews"))
    implementation(project(":Models"))
    implementation(project(":favoritesdatabase"))
    implementation(project(":anime_sources"))
    implementation(project(":sharedutils"))

    implementation(Deps.roomLibs)
    // For Kotlin use kapt instead of annotationProcessor
    kapt(Deps.roomCompiler)

    implementation(AnimeWorldDeps.autoBindings)
    kapt(AnimeWorldDeps.autoBindingsCompiler)

    implementation(AnimeWorldDeps.castFramework)
    implementation(AnimeWorldDeps.localCast)

    implementation(Deps.glide)
    kapt(Deps.glideCompiler)

    // Excludes the support library because it"s already included by Glide.
    implementation(Deps.glideRecyclerview) { isTransitive = false }

    implementation(AnimeWorldDeps.superForwardView)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
    implementation(Deps.koinLibs)
    implementation(Deps.composeLibs)
    implementation(AnimeWorldDeps.composeViewBinding)
    implementation(Deps.datastoreLibs)

    implementation(Media3Deps.exoplayerLibs)
}

apply(from = "$rootDir/buildtypes.gradle")