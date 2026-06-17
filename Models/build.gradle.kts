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
    implementation(commonLibs.coroutinesCore)
    implementation(androidLibs.coroutinesAndroid)

    implementation(commonLibs.bundles.ktorLibs)
    implementation(commonLibs.kotlinxSerialization)
}