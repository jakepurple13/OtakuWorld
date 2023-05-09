plugins {
    id("otaku-library")
    id("kotlinx-serialization")
}

android {
    namespace = "com.programmersbox.anime_sources"
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
    implementation(libs.jsoup)
    implementation(libs.duktape)
    implementation(libs.bundles.ziplineLibs)
    implementation(libs.rhino)
    implementation(libs.gson)
    implementation(libs.kotson)
    implementation(libs.karnKhttp) //okhttp instead
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)

    implementation(libs.uiUtil)

    implementation(libs.retrofit)
    implementation(libs.retrofitGson)

    implementation(projects.models)
    api(projects.sourceUtilities)

    implementation(libs.bundles.koinLibs)

    implementation(libs.bundles.ktorLibs)
    implementation(libs.kotlinxSerialization)
}