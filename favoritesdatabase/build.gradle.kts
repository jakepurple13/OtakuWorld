plugins {
    id("otaku-library")
    alias(libs.plugins.ksp)
    id("kotlinx-serialization")
}

android {
    defaultConfig {
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    namespace = "com.programmersbox.favoritesdatabase"
}

dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(Deps.gsonutils)

    implementation(projects.models)
    implementation(libs.kotlinxSerialization)

    implementation(libs.bundles.roomLibs)
    ksp(libs.roomCompiler)
    implementation(libs.bundles.pagingLibs)
}