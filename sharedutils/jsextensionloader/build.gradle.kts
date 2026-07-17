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
                implementation(project.dependencies.platform(commonLibs.koin.bom))
                implementation(commonLibs.koinCores)
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
                implementation(desktopLibs.kotlin.multiplatform.appdirs)
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
                implementation(commonLibs.datastorePref)
            }
        }
    }
}
