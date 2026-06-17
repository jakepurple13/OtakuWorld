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

    implementation(androidLibs.bundles.okHttpLibs)
    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.jsoup)
    implementation(androidLibs.gson)
    implementation(androidLibs.kotson)
    implementation(androidLibs.karnKhttp) //okhttp instead
    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)

    implementation(androidLibs.androidxWebkit)

    implementation(projects.models)

    implementation(platform(commonLibs.koin.bom))
    implementation(androidLibs.bundles.koinLibs)

    implementation(commonLibs.bundles.ktorLibs)
    implementation(commonLibs.kotlinxSerialization)
}