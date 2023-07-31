import plugins.SourceType

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-source-application")
}

android {
    namespace = "com.programmersbox.defaultmangasources"

    defaultConfig {
        applicationId = "com.programmersbox.defaultmangasources"
    }
}

otakuSourceInformation {
    name = "Default Manga Sources"
    classInfo = ".MangaSources"
    sourceType = SourceType.Manga
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
    implementation(projects.mangaSources)
    api(projects.sourceUtilities)
    implementation(libs.bundles.ktorLibs)

    implementation(libs.bundles.koinLibs)
}