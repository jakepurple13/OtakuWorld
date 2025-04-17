plugins {
    `otaku-multiplatform`
    alias(libs.plugins.ksp)
    id("kotlinx-serialization")
    //`otaku-protobuf`
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    id("com.squareup.wire")
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
            implementation("androidx.datastore:datastore-core-okio:${libs.versions.datastore.get()}")
        }
    }
}

wire {
    kotlin {}
    sourcePath {
        srcDir("src/commonMain/proto")
    }
}