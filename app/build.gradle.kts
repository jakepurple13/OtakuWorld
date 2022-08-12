plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.mikepenz.aboutlibraries.plugin")
}

//This is just to show what the minimum is needed to create a new app

android {
    compileSdk = AppInfo.compileVersion
    buildToolsVersion = AppInfo.buildVersion

    defaultConfig {
        applicationId = "com.programmersbox.otakuworld"
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

    setFlavorDimensions(listOf("version"))
    productFlavors {
        create("noFirebase") {
            dimension = "version"
        }
        create("full") {
            dimension = "version"
        }
    }
}

dependencies {
    implementation(Deps.kotlinStLib)
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    implementation(Deps.material)
    implementation(Deps.constraintlayout)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(Deps.recyclerview)

    implementation(project(":UIViews"))
    implementation(project(":Models"))
    implementation(project(":favoritesdatabase"))
    implementation(project(":novel_sources"))
    implementation(project(":anime_sources"))
    implementation(project(":manga_sources"))

    implementation(Deps.roomLibs)
    // For Kotlin use kapt instead of annotationProcessor
    kapt(Deps.roomCompiler)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(Deps.composeLibs)
    implementation(Deps.composePager)
    implementation("com.google.accompanist:accompanist-insets:${Deps.accompanist}")
    // If using insets-ui
    implementation("com.google.accompanist:accompanist-insets-ui:${Deps.accompanist}")

    implementation("androidx.window:window:1.0.0")

}