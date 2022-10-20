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
    namespace = "com.programmersbox.sharedutils"
}

dependencies {
    implementation(libs.kotlinStLib)
    implementation(libs.androidCore)
    implementation(libs.appCompat)
    implementation(libs.material)
    testImplementation(TestDeps.junit)
    testImplementation("com.jakewharton.picnic:picnic:0.6.0")
    testImplementation("com.lordcodes.turtle:turtle:0.8.0")
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    fullImplementation(libs.mlkitTranslate)
    fullImplementation(libs.mlkitLanguage)
    fullImplementation(libs.firebaseDatabase)
    fullImplementation(libs.firebaseFirestore)
    fullImplementation(libs.firebaseAuth)
    fullImplementation(libs.firebaseUiAuth)
    fullImplementation(libs.playServices)
    fullImplementation(libs.coroutinesPlayServices)

    implementation(libs.coroutinesCore)
    implementation(libs.coroutinesAndroid)

    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(libs.bundles.koinLibs)
    implementation(Deps.jakepurple13Libs)
    implementation(libs.uiUtil)
}

fun DependencyHandlerScope.fullImplementation(item: Provider<MinimalExternalModuleDependency>) = add("fullImplementation", item)
