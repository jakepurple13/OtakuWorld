plugins {
    `otaku-multiplatform`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.jsextensionloader"
}

kotlin {
    android {
        namespace = "com.programmersbox.jsextensionloader"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(commonLibs.coroutinesCore)
                implementation(commonLibs.kotlinxSerialization)
                implementation(commonLibs.zipline)
                implementation(commonLibs.ktorCore)
                implementation(commonLibs.ktorContentNegotiation)
                implementation(commonLibs.ktorJson)
                implementation(projects.kmpmodels.extensioninterfaces)
                implementation(projects.datastore)
            }
        }

        androidMain {
            dependencies {
                implementation(commonLibs.ktorAndroid)
                implementation(androidLibs.workRuntimeKtx)
            }
        }

        jvmMain {
            dependencies {
                implementation(commonLibs.ktorOkHttp)
            }
        }

        iosMain {
            dependencies {
                implementation(iosLibs.ktorDarwin)
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation(commonLibs.coroutinesTest)
                implementation(commonLibs.ktorMock)
            }
        }
    }
}
