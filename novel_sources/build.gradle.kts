plugins {
    id("otaku-library")
}

android {
    namespace = "com.programmersbox.novel_sources"
}

dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)
    implementation(androidLibs.bundles.okHttpLibs)

    implementation(commonLibs.coroutinesCore)

    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)
    implementation(androidLibs.gson)

    implementation(androidLibs.jsoup)

    implementation(androidLibs.uiUtil)

    implementation(projects.models)
    api(projects.sourceUtilities)
    implementation(commonLibs.bundles.ktorLibs)

    implementation(androidLibs.bundles.koinLibs)
}
