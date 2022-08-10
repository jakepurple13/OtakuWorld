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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Deps.kotlinVersion}")
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    testImplementation(Deps.junit)
    androidTestImplementation(Deps.androidJunit)
    androidTestImplementation(Deps.androidEspresso)

    implementation(Deps.okHttpLibs)
    implementation(Deps.coroutinesCore)
    implementation(Deps.jsoup)
    implementation("com.squareup.duktape:duktape-android:1.4.0")
    implementation(Deps.ziplineLibs)
    implementation("org.mozilla:rhino:1.7.14")
    implementation(Deps.gson)
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")
    implementation("io.karn:khttp-android:0.1.2") //okhttp instead
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)

    implementation(Deps.uiUtil)

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation(project(":Models"))

    implementation(Deps.koinLibs)

    implementation(Deps.ktorLibs)
    implementation(Deps.kotlinxSerialization)
}