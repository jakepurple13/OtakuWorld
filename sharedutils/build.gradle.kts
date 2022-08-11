plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
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

    setFlavorDimensions(listOf("version"))
    productFlavors {
        create("noFirebase") {
            dimension = "version"
        }
        create("full") {
            dimension = "version"
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
        }
        getByName("full") {
            java.srcDirs("src/full/java")
        }
        getByName("noFirebase") {
            java.srcDirs("src/noFirebase/java")
        }
    }
}

dependencies {
    implementation(Deps.kotlinStLib)
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    implementation(Deps.material)
    testImplementation(Deps.junit)
    testImplementation("com.jakewharton.picnic:picnic:0.6.0")
    testImplementation("com.lordcodes.turtle:turtle:0.7.0")
    androidTestImplementation(Deps.androidJunit)
    androidTestImplementation(Deps.androidEspresso)

    add("fullImplementation", "com.google.firebase:firebase-auth:21.0.7")
    add("fullImplementation", Deps.playServices)
    add("fullImplementation", "com.firebaseui:firebase-ui-auth:8.0.1")
    add("fullImplementation", "com.google.firebase:firebase-firestore-ktx:24.2.2")
    add("fullImplementation", "com.google.firebase:firebase-database-ktx:20.0.5")

    implementation(Deps.coroutinesCore)
    implementation(Deps.coroutinesAndroid)
    add("fullImplementation", "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:${Deps.coroutinesVersion}")

    add("fullImplementation", "com.google.mlkit:translate:17.0.0")
    add("fullImplementation", "com.google.mlkit:language-id:17.0.4")

    implementation(project(":Models"))
    implementation(project(":favoritesdatabase"))
    implementation(Deps.koinLibs)
    implementation(Deps.jakepurple13Libs)
    implementation(Deps.uiUtil)
}