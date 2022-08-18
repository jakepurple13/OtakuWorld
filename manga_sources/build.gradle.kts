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
}

dependencies {
    implementation(libs.kotlinStLib)
    implementation(libs.androidCore)
    implementation(libs.appCompat)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    testImplementation(TestDeps.mockitoCore)
    // required if you want to use Mockito for Android tests
    androidTestImplementation(TestDeps.mockitoAndroid)

    implementation(libs.bundles.okHttpLibs)
    implementation(libs.coroutinesCore)
    implementation(libs.jsoup)
    implementation(libs.duktape)
    implementation(libs.bundles.ziplineLibs)
    implementation(libs.gson)
    implementation(libs.kotson)
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)
    implementation(libs.kotlinxSerialization)
    implementation(libs.androidxWebkit)

    implementation(libs.uiUtil)

    implementation(projects.models)
    implementation("com.github.KotatsuApp:kotatsu-parsers:8709c3dd0c") {
        exclude("org.json", "json")
    }

    implementation(libs.bundles.koinLibs)
    implementation(libs.bundles.ktorLibs)
}