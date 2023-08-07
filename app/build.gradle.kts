plugins {
    id("otaku-application")
    kotlin("android")
    id("com.mikepenz.aboutlibraries.plugin")
    id("kotlinx-serialization")
    alias(libs.plugins.ksp)
}

//This is just to show what the minimum is needed to create a new app

android {
    defaultConfig {
        applicationId = "com.programmersbox.otakuworld"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        compose = true
    }

    namespace = "com.programmersbox.otakuworld"

    configurations.all {
        resolutionStrategy {
            force(libs.preference)
        }
    }
}

dependencies {
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.androidxWebkit)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(libs.recyclerview)

    implementation(projects.uiViews)
    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(projects.novelSources)
    implementation(projects.animeSources)
    implementation(projects.mangaSources)

    implementation(libs.bundles.roomLibs)
    ksp(libs.roomCompiler)

    implementation(libs.kotlinxSerialization)
    implementation(libs.jsoup)
    implementation(libs.preference) {
        isTransitive = true
    }
    implementation(libs.bundles.koinLibs)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
    val composeBom = platform(libs.composePlatform)
    implementation(composeBom)
    implementation(libs.bundles.compose)

    implementation(libs.androidxWindow)
}