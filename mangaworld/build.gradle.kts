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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Deps.kotlinVersion}")
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    implementation(Deps.material)
    implementation(Deps.constraintlayout)
    implementation(Deps.swiperefresh)
    implementation(Deps.recyclerview)
    implementation("com.google.android.gms:play-services-ads:21.1.0")
    testImplementation(Deps.junit)
    androidTestImplementation(Deps.androidJunit)
    androidTestImplementation(Deps.androidEspresso)
    implementation(Deps.preference)
    implementation(Deps.firebaseCrashLibs)

    implementation("com.github.hedzr:android-file-chooser:1.2.0")
    implementation("com.anggrayudi:storage:1.4.1")

    implementation(project(":UIViews"))
    implementation(project(":Models"))
    implementation(project(":favoritesdatabase"))
    implementation(project(":manga_sources"))
    implementation(project(":sharedutils"))

    implementation(Deps.glide)
    kapt(Deps.glideCompiler)
    implementation("com.github.bumptech.glide:recyclerview-integration:${Deps.glideVersion}") {
        // Excludes the support library because it"s already included by Glide.
        isTransitive = false
    }

    val piasy = "1.8.1"
    implementation("com.github.piasy:BigImageViewer:$piasy")
    implementation("com.github.piasy:GlideImageLoader:$piasy")
    implementation("com.github.piasy:ProgressPieIndicator:$piasy")

    implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")

    implementation("xyz.quaver:subsampledimage:0.0.1-alpha22-SNAPSHOT")

    implementation("com.mikepenz:iconics-core:5.3.4")
    implementation("com.mikepenz:google-material-typeface:4.0.0.2-kotlin@aar")

    implementation(Deps.coroutinesCore)
    implementation(Deps.coroutinesAndroid)

    implementation(Deps.roomLibs)
    // For Kotlin use kapt instead of annotationProcessor
    kapt("androidx.room:room-compiler:${Deps.roomVersion}")

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(Deps.koinLibs)

    implementation(Deps.composeLibs)
    implementation("com.google.accompanist:accompanist-pager:${Deps.accompanist}")
    implementation("com.google.accompanist:accompanist-swiperefresh:${Deps.accompanist}")
    implementation("io.coil-kt:coil-gif:${Deps.coil}")

    implementation(Deps.datastoreLibs)

}

apply(from = "$rootDir/buildtypes.gradle")