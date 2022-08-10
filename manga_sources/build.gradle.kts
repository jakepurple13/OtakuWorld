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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Deps.kotlinVersion}")
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    testImplementation(Deps.junit)
    androidTestImplementation(Deps.androidJunit)
    androidTestImplementation(Deps.androidEspresso)

    testImplementation("org.mockito:mockito-core:4.2.0")
    // required if you want to use Mockito for Android tests
    androidTestImplementation("org.mockito:mockito-android:4.2.0")

    implementation(Deps.okHttpLibs)
    implementation(Deps.coroutinesCore)
    implementation(Deps.jsoup)
    implementation("com.squareup.duktape:duktape-android:1.4.0")
    implementation(Deps.ziplineLibs)
    implementation(Deps.gson)
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    implementation("androidx.webkit:webkit:1.4.0")

    implementation(Deps.uiUtil)

    implementation(project(":Models"))
    implementation("com.github.KotatsuApp:kotatsu-parsers:8709c3dd0c") {
        exclude("org.json", "json")
    }

    implementation(Deps.koinLibs)

    implementation(Deps.ktorLibs)
}