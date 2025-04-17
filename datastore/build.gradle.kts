plugins {
    `otaku-multiplatform`
    alias(libs.plugins.ksp)
    id("kotlinx-serialization")
    //`otaku-protobuf`
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

otakuDependencies {
    androidPackageName = "com.programmersbox.datastore"
}

kotlin {
    androidLibrary {
        namespace = "com.programmersbox.datastore"
    }

    sourceSets {
        commonMain.dependencies {
            //implementation(projects.models)
            implementation(libs.kotlinxSerialization)
            implementation(libs.bundles.datastoreLibs)
            //implementation(libs.composeRuntimeLivedata)
            implementation(compose.runtime)
            implementation(libs.multiplatform.lifecycle.runtime.compose)

        }
    }
}