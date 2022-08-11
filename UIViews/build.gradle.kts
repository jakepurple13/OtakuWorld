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
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation(Deps.preference)
    implementation("androidx.window:window:1.1.0-alpha03")
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation("com.google.firebase:firebase-auth:21.0.7")
    implementation(Deps.playServices)

    implementation("com.google.androidbrowserhelper:androidbrowserhelper:2.4.0")
    implementation("androidx.browser:browser:1.4.0")

    implementation("me.zhanghai.android.fastscroll:library:1.1.8")

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

    implementation("com.mikepenz:iconics-core:5.3.4")
    implementation("com.mikepenz:google-material-typeface:4.0.0.2-kotlin@aar")
    //Google Material Icons
    implementation("com.mikepenz:fontawesome-typeface:5.13.3.0-kotlin@aar")

    implementation(Deps.gson)

    implementation(Deps.recyclerview)
    implementation(Deps.palette)
    implementation("com.airbnb.android:lottie:${Deps.lottieVersion}")
    implementation(Deps.roomLibs)
    implementation("com.github.anzaizai:EasySwipeMenuLayout:1.1.4")

    implementation("com.github.noowenz:ShowMoreLess:1.0.3")
    implementation("com.mikepenz:aboutlibraries-core:${Deps.latestAboutLibsRelease}")
    implementation("com.mikepenz:aboutlibraries-compose:${Deps.latestAboutLibsRelease}")

    implementation(Deps.glide)
    kapt(Deps.glideCompiler)
    implementation("com.github.bumptech.glide:recyclerview-integration:${Deps.glideVersion}") {
        // Excludes the support library because it"s already included by Glide.
        isTransitive = false
    }

    implementation(Deps.stetho)

    implementation(Deps.workRuntime)

    // Kotlin
    api("androidx.navigation:navigation-fragment-ktx:${Deps.navVersion}")
    api("androidx.navigation:navigation-ui-ktx:${Deps.navVersion}")

    // Testing Navigation
    androidTestImplementation("androidx.navigation:navigation-testing:${Deps.navVersion}")

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(Deps.composeLibs)
    implementation("com.airbnb.android:lottie-compose:${Deps.lottieVersion}")
    implementation("me.onebone:toolbar-compose:2.3.4")
    implementation("com.github.nanihadesuka:LazyColumnScrollbar:1.5.1")
    implementation("androidx.paging:paging-compose:1.0.0-alpha16")

    implementation("androidx.paging:paging-runtime-ktx:${Deps.pagingVersion}")
    // alternatively - without Android dependencies for tests
    testImplementation("androidx.paging:paging-common-ktx:${Deps.pagingVersion}")

    implementation(Deps.datastoreLibs)
}