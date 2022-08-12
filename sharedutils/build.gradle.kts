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
    testImplementation(TestDeps.junit)
    testImplementation("com.jakewharton.picnic:picnic:0.6.0")
    testImplementation("com.lordcodes.turtle:turtle:0.7.0")
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    fullImplementation(SharedDeps.fullImplementation)
    implementation(Deps.coroutinesCore)
    implementation(Deps.coroutinesAndroid)

    implementation(project(":Models"))
    implementation(project(":favoritesdatabase"))
    implementation(Deps.koinLibs)
    implementation(Deps.jakepurple13Libs)
    implementation(Deps.uiUtil)
}

fun DependencyHandlerScope.fullImplementation(item: Array<String>) = item.forEach { fullImplementation(it) }
fun DependencyHandlerScope.fullImplementation(item: String) = add("fullImplementation", item)
