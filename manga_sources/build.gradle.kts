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
    implementation(Deps.kotlinStLib)
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    testImplementation(Deps.junit)
    androidTestImplementation(Deps.androidJunit)
    androidTestImplementation(Deps.androidEspresso)

    testImplementation("org.mockito:mockito-core:4.6.1")
    // required if you want to use Mockito for Android tests
    androidTestImplementation("org.mockito:mockito-android:4.6.1")

    implementation(Deps.okHttpLibs)
    implementation(Deps.coroutinesCore)
    implementation(Deps.jsoup)
    implementation(SourceDeps.duktape)
    implementation(SourceDeps.ziplineLibs)
    implementation(Deps.gson)
    implementation(SourceDeps.kotson)
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)

    implementation(SourceDeps.kotlinxJson)

    implementation("androidx.webkit:webkit:1.4.0")

    implementation(Deps.uiUtil)

    implementation(project(":Models"))
    implementation("com.github.KotatsuApp:kotatsu-parsers:8709c3dd0c") {
        exclude("org.json", "json")
    }

    implementation(Deps.koinLibs)

    implementation(Deps.ktorLibs)
}