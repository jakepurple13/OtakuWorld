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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Deps.kotlinVersion}")
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    implementation(Deps.material)
    implementation(Deps.constraintlayout)
    implementation(Deps.preference)
    implementation(Deps.firebaseCrashLibs)
    implementation(Deps.recyclerview)
    implementation(Deps.swiperefresh)
    testImplementation(Deps.junit)
    androidTestImplementation(Deps.androidJunit)
    androidTestImplementation(Deps.androidEspresso)

    implementation("com.airbnb.android:lottie:${Deps.lottieVersion}")
    implementation("com.github.hedzr:android-file-chooser:1.2.0")

    implementation("com.ncorti:slidetoact:0.9.0")

    implementation("androidx.mediarouter:mediarouter:1.3.1")
    implementation("androidx.tonyodev.fetch2:xfetch2:3.1.6")
    implementation("androidx.tonyodev.fetch2okhttp:xfetch2okhttp:3.1.6")

    implementation("com.github.TorrentStream:TorrentStream-Android:2.8.0")

    implementation(Deps.gson)

    implementation("com.mikepenz:iconics-core:5.3.4")
    implementation("com.mikepenz:google-material-typeface:4.0.0.2-kotlin@aar")
    //Google Material Icons
    implementation("com.mikepenz:fontawesome-typeface:5.13.3.0-kotlin@aar")

    implementation(project(":UIViews"))
    implementation(project(":Models"))
    implementation(project(":favoritesdatabase"))
    implementation(project(":anime_sources"))
    implementation(project(":sharedutils"))

    implementation(Deps.roomLibs)
    // For Kotlin use kapt instead of annotationProcessor
    kapt("androidx.room:room-compiler:${Deps.roomVersion}")

    val autoBinding = "1.1-beta04"
    implementation("io.github.kaustubhpatange:autobindings:$autoBinding")
    kapt("io.github.kaustubhpatange:autobindings-compiler:$autoBinding")

    implementation("com.google.android.gms:play-services-cast-framework:21.1.0")

    implementation("com.github.KaustubhPatange:Android-Cast-Local-Sample:0.01")

    implementation(Deps.glide)
    kapt(Deps.glideCompiler)
    implementation("com.github.bumptech.glide:recyclerview-integration:${Deps.glideVersion}") {
        // Excludes the support library because it"s already included by Glide.
        isTransitive = false
    }

    implementation("com.github.ertugrulkaragoz:SuperForwardView:0.2")

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(Deps.koinLibs)

    implementation(Deps.composeLibs)

    implementation("androidx.compose.ui:ui-viewbinding:${Deps.jetpack}")

    implementation(Deps.datastoreLibs)

    //debugImplementation("com.squareup.leakcanary:leakcanary-android:2.8")

    implementation(Media3Deps.exoplayerLibs)
}

apply(from = "$rootDir/buildtypes.gradle")