plugins {
    id("otaku-library")
    id("kotlinx-serialization")
}

android {
    namespace = "com.programmersbox.models"
}

dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    //Coroutines
    implementation(libs.coroutinesCore)
    implementation(libs.coroutinesAndroid)

    implementation(libs.bundles.ktorLibs)
    implementation(libs.kotlinxSerialization)
}