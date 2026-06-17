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

    implementation(androidLibs.bundles.okHttpLibs)
    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.jsoup)
    implementation(androidLibs.duktape)
    implementation(androidLibs.bundles.ziplineLibs)
    implementation(androidLibs.gson)
    implementation(androidLibs.kotson)
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)
    implementation(commonLibs.kotlinxSerialization)
    implementation(androidLibs.androidxWebkit)

    implementation(androidLibs.uiUtil)

    implementation(projects.models)
    api(projects.sourceUtilities)

    implementation("com.github.KotatsuApp:kotatsu-parsers:8709c3dd0c") {
        exclude("org.json", "json")
    }

    implementation(androidLibs.bundles.koinLibs)
    implementation(commonLibs.bundles.ktorLibs)
}
