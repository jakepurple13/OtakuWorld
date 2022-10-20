plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlinx-serialization")
}

android {
    compileSdk = AppInfo.compileVersion
    buildToolsVersion = AppInfo.buildVersion

    defaultConfig {
        minSdk = AppInfo.minimumSdk
        targetSdk = AppInfo.targetSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-proguard-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    namespace = "com.programmersbox.anime_sources"
}

dependencies {
    implementation(libs.kotlinStLib)
    implementation(libs.androidCore)
    implementation(libs.appCompat)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(libs.bundles.okHttpLibs)
    implementation(libs.coroutinesCore)
    implementation(libs.jsoup)
    implementation(libs.duktape)
    implementation(libs.bundles.ziplineLibs)
    implementation(libs.rhino)
    implementation(libs.gson)
    implementation(libs.kotson)
    implementation(libs.karnKhttp) //okhttp instead
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)

    implementation(libs.uiUtil)

    implementation(libs.retrofit)
    implementation(libs.retrofitGson)

    implementation(projects.models)

    implementation(libs.bundles.koinLibs)

    implementation(libs.bundles.ktorLibs)
    implementation(libs.kotlinxSerialization)
}