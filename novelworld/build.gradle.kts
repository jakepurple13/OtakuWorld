plugins {
    `otaku-application`
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.mikepenz.aboutlibraries.plugin.android")
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.firebase.performance)
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
    implementation(androidLibs.material)
    implementation(androidx.preference.preference)
    implementation(androidx.recyclerview.recyclerview)
    implementation(androidx.constraintlayout.constraintlayout)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(androidLibs.iconicsCore)
    implementation(Deps.materialTypeface)

    implementation(projects.uiViews)
    implementation(projects.kmpmodels)
    implementation(projects.favoritesdatabase)
    implementation(projects.sharedutils)

    implementation(commonLibs.bundles.roomLibs)
    ksp(commonLibs.roomCompiler)

    implementation(platform(androidLibs.firebasePlatform))
    implementation(androidLibs.bundles.firebaseCrashLibs)
    val composeBom = platform(androidLibs.composePlatform)
    implementation(composeBom)
    implementation(androidLibs.bundles.compose)

    implementation(androidx.datastore.datastore)
    implementation(androidx.datastore.datastorePreferences)

    //Custom Libraries
    implementation(Deps.jakepurple13Libs)

    implementation(commonLibs.ktorAndroid)

    implementation(projects.novelworld.shared)
}
