plugins {
    id("com.android.library")
    kotlin("android")
    id("androidx.navigation.safeargs.kotlin")
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
    buildFeatures {
        dataBinding = true
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.jetpackCompiler.get()
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
}

dependencies {
    implementation(libs.kotlinStLib)
    implementation(libs.androidCore)
    implementation(libs.appCompat)
    implementation(libs.material)
    implementation(libs.androidxLegacySupport)
    implementation(libs.preference)
    implementation(libs.androidxWindow)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(libs.firebaseAuth)
    implementation(libs.playServices)

    implementation(libs.androidBrowserHelper)
    implementation(libs.androidxBrowser)

    implementation(libs.fastScroll)

    implementation(libs.reactiveNetwork)

    implementation(libs.bundles.koinLibs)

    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)

    implementation(libs.constraintlayout)
    implementation(libs.coroutinesCore)
    implementation(libs.coroutinesAndroid)
    implementation(libs.fragmentKtx)
    implementation(libs.lifecycleExtensions)
    implementation(libs.lifecycleRuntime)
    implementation(libs.lifecycleLivedata)
    implementation(libs.lifecycleViewModel)

    implementation(libs.iconicsCore)
    implementation(Deps.materialTypeface)
    implementation(Deps.fontawesomeTypeface)

    implementation(libs.gson)

    implementation(libs.recyclerview)
    implementation(libs.palette)
    implementation(libs.lottie)
    implementation(libs.lottieCompose)
    implementation(libs.bundles.roomLibs)

    implementation(libs.showMoreLess)
    implementation(libs.aboutLibrariesCore)
    implementation(libs.aboutLibrariesCompose)

    implementation(libs.glide)
    kapt(libs.glideCompiler)

    // Excludes the support library because it"s already included by Glide.
    implementation(libs.glideRecyclerview) { isTransitive = false }

    implementation(libs.stetho)

    implementation(libs.workRuntime)

    // Kotlin
    api(libs.navFragment)
    api(libs.navUiKtx)

    // Testing Navigation
    androidTestImplementation(libs.navTesting)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(libs.bundles.compose)
    implementation(libs.toolbarCompose)
    implementation(libs.lazyColumnScrollbar)
    implementation(libs.pagingCompose)
    implementation(libs.bundles.pagingLibs)
    implementation(libs.bundles.datastoreLibs)
}