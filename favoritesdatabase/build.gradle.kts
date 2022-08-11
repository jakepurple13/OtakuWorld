plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    //TODO: In order to use ksp, EVERYTHING needs to use it. So the removal of kapt would be needed
    //id "com.google.devtools.ksp" version "1.6.0-1.0.1"
}

android {
    compileSdk = AppInfo.compileVersion
    buildToolsVersion = AppInfo.buildVersion

    defaultConfig {
        minSdk = AppInfo.minimumSdk
        targetSdk = AppInfo.targetSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-proguard-rules.pro")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
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

    implementation(Deps.gsonutils)

    implementation(project(":Models"))

    implementation(Deps.roomLibs)
    // For Kotlin use kapt instead of annotationProcessor
    kapt("androidx.room:room-compiler:${Deps.roomVersion}")

    implementation("androidx.paging:paging-runtime-ktx:${Deps.pagingVersion}")
    // alternatively - without Android dependencies for tests
    testImplementation("androidx.paging:paging-common-ktx:${Deps.pagingVersion}")
    implementation("androidx.room:room-paging:2.4.3")
}