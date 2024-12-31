@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-library")
    id("kotlinx-serialization")
}

android {
    namespace = "com.programmersbox.source_utilities"
}

dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(libs.bundles.okHttpLibs)
    implementation(libs.coroutinesCore)
    implementation(libs.jsoup)
    implementation(libs.gson)
    implementation(libs.kotson)
    implementation(libs.karnKhttp) //okhttp instead
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)

    implementation(libs.androidxWebkit)

    implementation(projects.models)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koinLibs)

    implementation(libs.bundles.ktorLibs)
    implementation(libs.kotlinxSerialization)
}