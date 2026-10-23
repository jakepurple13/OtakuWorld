plugins {
    `otaku-multiplatform`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.mihon"
}

kotlin {
    android {
        namespace = "com.programmersbox.mihon"
        withHostTestBuilder {}.configure {
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(commonLibs.coroutinesCore)
                implementation(commonLibs.kotlinxSerialization)
                implementation(commonLibs.kotlinxSerializationProtobuf)
                implementation(commonLibs.ktorCore)
                implementation(projects.kmpmodels)
            }
        }

        commonTest {
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
                implementation(commonLibs.ktorMock)
            }
        }

        androidMain {
            kotlin.srcDir("src/jvmAndroidShared/kotlin")
            dependencies {
                implementation(commonLibs.ktorOkHttp)
                implementation(projects.datastore)
                implementation(project.dependencies.platform(commonLibs.koin.bom))
                implementation(commonLibs.koinCores)
                implementation(androidLibs.okhttpLib)
                implementation(commonLibs.kotlinxSerializationOkio)
                implementation(androidLibs.okhttpDns)
                implementation(androidLibs.okhttpLogging)
                implementation(androidLibs.brotli)
                implementation(androidLibs.zstd)
                implementation(androidLibs.jsoup)
                implementation(androidLibs.injekt)
                implementation(androidLibs.rxjava1)
                implementation(commonLibs.zipline)
                implementation(androidLibs.squareLogcat)
                implementation(androidLibs.preference)
                implementation(androidLibs.androidxWebkit)
            }
        }

        getByName("androidHostTest") {
            kotlin.srcDir("src/jvmAndroidSharedTest/kotlin")
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
                implementation(androidLibs.okhttpMockWebServer)
            }
        }

        jvmMain {
            kotlin.srcDir("src/jvmAndroidShared/kotlin")
            dependencies {
                implementation(commonLibs.ktorOkHttp)
                implementation(projects.datastore)
                implementation(project.dependencies.platform(commonLibs.koin.bom))
                implementation(commonLibs.koinCores)
                implementation(androidLibs.okhttpLib)
                implementation(commonLibs.kotlinxSerializationOkio)
                implementation(androidLibs.okhttpDns)
                implementation(androidLibs.okhttpLogging)
                implementation(androidLibs.brotli)
                implementation(androidLibs.zstd)
                implementation(androidLibs.jsoup)
                implementation(androidLibs.injekt)
                implementation(androidLibs.rxjava1)
                implementation(commonLibs.zipline)
                implementation(projects.sharedutils.kmpextensionloader)
            }
        }

        jvmTest {
            kotlin.srcDir("src/jvmAndroidSharedTest/kotlin")
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
                implementation(androidLibs.okhttpMockWebServer)
            }
        }
    }
}
