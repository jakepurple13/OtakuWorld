plugins {
    `otaku-application`
    id("androidx.navigation.safeargs.kotlin")
    id("com.mikepenz.aboutlibraries.plugin")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.programmersbox.novelworld"

    defaultConfig {
        applicationId = "com.programmersbox.novelworld"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.material)
    implementation(androidx.preference.preference)
    implementation(androidx.recyclerview.recyclerview)
    implementation(androidx.constraintlayout.constraintlayout)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(libs.iconicsCore)
    implementation(Deps.materialTypeface)

    implementation(projects.uiViews)
    implementation(projects.models)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)

    implementation(libs.bundles.roomLibs)
    ksp(libs.roomCompiler)

    implementation(platform(libs.firebasePlatform))
    implementation(libs.bundles.firebaseCrashLibs)
    val composeBom = platform(libs.composePlatform)
    implementation(composeBom)
    implementation(libs.bundles.compose)

    implementation(androidx.datastore.datastore)
    implementation(androidx.datastore.datastorePreferences)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(libs.bundles.koinLibs)

    implementation(libs.ktorAndroid)
}
