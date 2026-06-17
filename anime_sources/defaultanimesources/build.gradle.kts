import plugins.SourceType

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-source-application")
}

android {
    namespace = "com.programmersbox.defaultanimesources"

    defaultConfig {
        applicationId = "com.programmersbox.defaultanimesources"
    }
}

otakuSourceInformation {
    name = "Default Anime Sources"
    classInfo = ".AnimeSources"
    sourceType = SourceType.Anime
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
    implementation(projects.animeSources)
    api(projects.sourceUtilities)
    implementation(commonLibs.bundles.ktorLibs)

    implementation(androidLibs.bundles.koinLibs)
}
