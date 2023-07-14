@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("otaku-library")
}

android {
    namespace = "com.programmersbox.extensionloader"
}

dependencies {
    implementation(projects.models)
    //Custom Libraries
    implementation(Deps.jakepurple13Libs)
    implementation("io.reactivex:rxjava:1.3.8")
}