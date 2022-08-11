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
}

dependencies {
    implementation(Deps.kotlinStLib)
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(Deps.okHttpLibs)
    implementation(Deps.coroutinesCore)
    implementation(Deps.jsoup)
    implementation(SourceDeps.duktape)
    implementation(SourceDeps.ziplineLibs)
    implementation(SourceDeps.rhino)
    implementation(Deps.gson)
    implementation(SourceDeps.kotson)
    implementation(SourceDeps.karnKhttp) //okhttp instead
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)

    implementation(Deps.uiUtil)

    implementation(SourceDeps.retrofit)
    implementation(SourceDeps.retrofitGson)

    implementation(project(":Models"))

    implementation(Deps.koinLibs)

    implementation(Deps.ktorLibs)
    implementation(Deps.kotlinxSerialization)
}