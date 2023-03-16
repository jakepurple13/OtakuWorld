plugins {
    id("com.android.application")
    kotlin("android")
    id("com.mikepenz.aboutlibraries.plugin")
    alias(libs.plugins.ksp)
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
        kotlinCompilerExtensionVersion = libs.versions.jetpackCompiler.get()
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
    namespace = "com.programmersbox.otakuworld"
}

dependencies {
    implementation(libs.kotlinStLib)
    implementation(libs.androidCore)
    implementation(libs.appCompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(libs.recyclerview)

    implementation(projects.uiViews)
    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(projects.novelSources)
    implementation(projects.animeSources)
    implementation(projects.mangaSources)

    implementation(libs.bundles.roomLibs)
    ksp(libs.roomCompiler)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(libs.bundles.compose)

    implementation("androidx.window:window:1.0.0")

}