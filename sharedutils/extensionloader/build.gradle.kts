@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-library")
}

android {
    namespace = "com.programmersbox.extensionloader"
}

dependencies {
    implementation(projects.models)
    implementation(projects.kmpmodels)
    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
}