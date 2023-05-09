plugins {
    id("otaku-library")
}

android {
    namespace = "com.programmersbox.novel_sources"
}

dependencies {
    implementation(libs.kotlinStLib)
    implementation(libs.androidCore)
    implementation(libs.appCompat)
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    implementation(libs.bundles.okHttpLibs)

    implementation(libs.coroutinesCore)

    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)
    implementation(libs.gson)

    implementation(libs.jsoup)

    implementation(libs.uiUtil)

    implementation(projects.models)
    api(projects.sourceUtilities)
    implementation(libs.bundles.ktorLibs)

    implementation(libs.bundles.koinLibs)
}