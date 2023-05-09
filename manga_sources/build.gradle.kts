plugins {
    id("otaku-library")
    id("kotlinx-serialization")
}

android {
    namespace = "com.programmersbox.manga_sources"
}

dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    testImplementation(TestDeps.mockitoCore)
    // required if you want to use Mockito for Android tests
    androidTestImplementation(TestDeps.mockitoAndroid)

    implementation(libs.bundles.okHttpLibs)
    implementation(libs.coroutinesCore)
    implementation(libs.jsoup)
    implementation(libs.duktape)
    implementation(libs.bundles.ziplineLibs)
    implementation(libs.gson)
    implementation(libs.kotson)
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)
    implementation(libs.kotlinxSerialization)
    implementation(libs.androidxWebkit)

    implementation(libs.uiUtil)

    implementation(projects.models)
    api(projects.sourceUtilities)

    implementation("com.github.KotatsuApp:kotatsu-parsers:8709c3dd0c") {
        exclude("org.json", "json")
    }

    implementation(libs.bundles.koinLibs)
    implementation(libs.bundles.ktorLibs)
}