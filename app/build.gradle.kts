plugins {
    id("otaku-application")
    kotlin("android")
    id("com.mikepenz.aboutlibraries.plugin")
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
}

dependencies {
    implementation(libs.kotlinStLib)
    implementation(libs.androidCore)
    implementation(libs.appCompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
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

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
    val composeBom = platform(libs.composePlatform)
    implementation(composeBom)
    implementation(libs.bundles.compose)

    implementation("androidx.window:window:1.0.0")

}