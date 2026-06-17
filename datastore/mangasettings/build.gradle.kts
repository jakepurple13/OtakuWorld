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
    android {
        namespace = "com.programmersbox.mangasettings"
    }

    sourceSets {
        commonMain.dependencies {
            //implementation(projects.models)
            implementation(commonLibs.kotlinxSerialization)
            implementation(commonLibs.bundles.datastoreLibs)
            //implementation(libs.composeRuntimeLivedata)
            implementation(compose.runtime)
            implementation(commonLibs.multiplatform.lifecycle.runtime.compose)
            implementation(commonLibs.datastoreOkio)
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