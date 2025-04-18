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
    androidPackageName = "com.programmersbox.mangasettings"
}

kotlin {
    androidLibrary {
        namespace = "com.programmersbox.mangasettings"
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
            implementation(projects.datastore)
        }
    }
}

wire {
    kotlin {}
    sourcePath {
        srcDir("src/commonMain/proto")
    }
}