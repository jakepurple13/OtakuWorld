plugins {
    id("otaku-library")
    id("kotlinx-serialization")
}

android {
    namespace = "com.programmersbox.anime_sources"
}

dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(androidLibs.bundles.okHttpLibs)
    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.jsoup)
    implementation(androidLibs.duktape)
    implementation(androidLibs.bundles.ziplineLibs)
    implementation(androidLibs.rhino)
    implementation(androidLibs.gson)
    implementation(androidLibs.kotson)
    implementation(androidLibs.karnKhttp) //okhttp instead
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)

    implementation(androidLibs.uiUtil)

    implementation(androidLibs.retrofit)
    implementation(androidLibs.retrofitGson)

    implementation(projects.models)
    api(projects.sourceUtilities)

    implementation(platform(commonLibs.koin.bom))
    implementation(androidLibs.bundles.koinLibs)

    implementation(commonLibs.bundles.ktorLibs)
    implementation(commonLibs.kotlinxSerialization)
}
