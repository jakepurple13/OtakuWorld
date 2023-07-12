import plugins.SourceType

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-source-application")
}

android {
    namespace = "com.programmersbox.novelupdates"

    defaultConfig {
        applicationId = "com.programmersbox.novelupdates"
    }
}

otakuSourceInformation {
    name = "NovelUpdates"
    classInfo = ".NovelUpdates1"
    sourceType = SourceType.Novel
}

dependencies {
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

    implementation(projects.models)
    api(projects.sourceUtilities)
    implementation(libs.bundles.ktorLibs)

    implementation(libs.bundles.koinLibs)
}