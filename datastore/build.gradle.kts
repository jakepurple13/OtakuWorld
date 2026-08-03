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
    android {
        namespace = "com.programmersbox.datastore"
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
            implementation(commonLibs.material.kolor)
            implementation(commonLibs.kmpalette.core)
        }

        androidMain.dependencies {
            implementation(commonLibs.datastoreTink)
        }

        jvmMain.dependencies {
            implementation(commonLibs.datastoreTink)
        }
    }
}

wire {
    kotlin {}
    sourcePath {
        srcDir("src/commonMain/proto")
    }
}