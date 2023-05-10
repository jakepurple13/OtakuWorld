import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import plugins.ProductFlavorTypes

plugins {
    id("otaku-library")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlinx-serialization")
    id("com.google.protobuf") version "0.9.3"
    alias(libs.plugins.ksp)
}

android {
    buildFeatures {
        dataBinding = true
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.jetpackCompiler.get()
    }

    setFlavorDimensions(listOf(ProductFlavorTypes.dimension))
    productFlavors {
        ProductFlavorTypes.NoFirebase(this)
        ProductFlavorTypes.Full(this)
    }
    namespace = "com.programmersbox.uiviews"
}

dependencies {
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
    ksp(libs.glideCompiler)

    // Excludes the support library because it"s already included by Glide.
    implementation(libs.glideRecyclerview) { isTransitive = false }

    implementation(libs.stetho)

    implementation(libs.workRuntime)

    implementation(libs.kotlinxSerialization)

    // Kotlin
    api(libs.navFragment)
    api(libs.navUiKtx)

    // Testing Navigation
    androidTestImplementation(libs.navTesting)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    val composeBom = platform(libs.composePlatform)
    implementation(composeBom)
    implementation(libs.bundles.compose)
    implementation(libs.toolbarCompose)
    implementation(libs.lazyColumnScrollbar)
    implementation(libs.pagingCompose)
    implementation(libs.bundles.pagingLibs)
    implementation(libs.bundles.datastoreLibs)

    implementation(libs.bundles.protobuf)
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.21.12" }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("java") { option("lite") }
                id("kotlin") { option("lite") }
            }
        }
    }
}
