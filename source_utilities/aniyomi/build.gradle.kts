plugins {
    `otaku-multiplatform`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.aniyomi"
}

kotlin {
    android {
        namespace = "com.programmersbox.aniyomi"
        withHostTestBuilder {}.configure {
            isReturnDefaultValues = true
        }
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.files.add(project.file("consumer-rules.pro"))
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
                // Extension APKs compile against these; the host must provide them.
                api(androidLibs.nanohttpd)
                api(androidLibs.preference)
                api(androidLibs.injekt)
                api(androidLibs.rxjava1)
                api(androidLibs.jsoup)
                api(androidLibs.okhttpLib)
                api(androidLibs.okhttpDns)
                api(androidLibs.okhttpLogging)
                api(androidLibs.brotli)
                api(androidLibs.zstd)
                api(commonLibs.kotlinxSerialization)
                api(commonLibs.kotlinxSerializationProtobuf)
                api(commonLibs.kotlinxSerializationOkio)
                api(commonLibs.zipline)
                api(androidLibs.squareLogcat)
                api(androidLibs.androidxWebkit)
            }
        }

        getByName("androidHostTest") {
            kotlin.srcDir("src/jvmAndroidSharedTest/kotlin")
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
                implementation(commonLibs.ktorMock)
            }
        }

        jvmMain {
            kotlin.srcDir("src/jvmAndroidShared/kotlin")
            dependencies {
                implementation(commonLibs.ktorOkHttp)
                implementation(projects.datastore)
                implementation(project.dependencies.platform(commonLibs.koin.bom))
                implementation(commonLibs.koinCores)
                implementation(androidLibs.nanohttpd)
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
                // android.* and androidx.preference stubs the vendored runtime compiles against.
                implementation(projects.sharedutils.kmpextensionloader)
                implementation(androidLibs.apksig)
                implementation("net.dongliu:apk-parser:2.6.10")
                implementation(desktopLibs.kotlin.multiplatform.appdirs)
            }
        }

        jvmTest {
            kotlin.srcDir("src/jvmAndroidSharedTest/kotlin")
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
                implementation(androidLibs.okhttpMockWebServer)
                implementation("org.ow2.asm:asm:9.10.1") // AniyomiDexConversionTest
            }
        }
    }
}
