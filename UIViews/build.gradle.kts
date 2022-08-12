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
        kotlinCompilerExtensionVersion = Deps.jetpackCompiler
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
    implementation(Deps.kotlinStLib)
    implementation(Deps.androidCore)
    implementation(Deps.appCompat)
    implementation(Deps.material)
    implementation(Deps.androidxLegacySupport)
    implementation(Deps.preference)
    implementation(Deps.androidxWindow)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(Deps.firebaseAuth)
    implementation(Deps.playServices)

    implementation(Deps.androidBrowserHelper)
    implementation(Deps.androidxBrowser)

    implementation(Deps.fastScroll)

    implementation(Deps.reactiveNetwork)

    implementation(Deps.koinLibs)

    implementation(project(":Models"))
    implementation(project(":favoritesdatabase"))
    implementation(project(":sharedutils"))

    implementation(Deps.constraintlayout)
    implementation(Deps.coroutinesCore)
    implementation(Deps.coroutinesAndroid)
    implementation(Deps.fragmentKtx)
    implementation(Deps.lifecycleExtensions)
    implementation(Deps.lifecycleRuntime)
    implementation(Deps.lifecycleLivedata)
    implementation(Deps.lifecycleViewModel)

    implementation(Deps.iconicsCore)
    implementation(Deps.materialTypeface)
    implementation(Deps.fontawesomeTypeface)

    implementation(Deps.gson)

    implementation(Deps.recyclerview)
    implementation(Deps.palette)
    implementation(Deps.lottie)
    implementation(Deps.lottieCompose)
    implementation(Deps.roomLibs)

    implementation(Deps.showMoreLess)
    implementation(Deps.aboutLibrariesCore)
    implementation(Deps.aboutLibrariesCompose)

    implementation(Deps.glide)
    kapt(Deps.glideCompiler)

    // Excludes the support library because it"s already included by Glide.
    implementation(Deps.glideRecyclerview) { isTransitive = false }

    implementation(Deps.stetho)

    implementation(Deps.workRuntime)

    // Kotlin
    api(Deps.navFragment)
    api(Deps.navUiKtx)

    // Testing Navigation
    androidTestImplementation(Deps.navTesting)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(Deps.composeLibs)
    implementation(Deps.toolbarCompose)
    implementation(Deps.lazyColumnScrollbar)
    implementation(Deps.pagingCompose)
    implementation(Deps.pagingLibs)
    implementation(Deps.datastoreLibs)
}