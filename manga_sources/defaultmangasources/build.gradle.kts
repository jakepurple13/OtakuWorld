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
    implementation(androidLibs.bundles.okHttpLibs)

    implementation(commonLibs.coroutinesCore)

    implementation(Deps.gsonutils)
    implementation(Deps.helpfulutils)
    debugImplementation(Deps.loggingutils)
    implementation(androidLibs.gson)

    implementation(androidLibs.jsoup)

    implementation(projects.models)
    implementation(projects.mangaSources)
    api(projects.sourceUtilities)
    implementation(commonLibs.bundles.ktorLibs)

    implementation(androidLibs.bundles.koinLibs)
}
