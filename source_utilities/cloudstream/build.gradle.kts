// GPL-3.0: this module vendors recloudstream/cloudstream's `library` (see LICENSE, VENDORED.md).
plugins {
    `otaku-multiplatform`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.cloudstream"
}

kotlin {
    android {
        namespace = "com.programmersbox.cloudstream"
        androidResources {
            enable = true
        }
        withHostTestBuilder {}.configure {
            isReturnDefaultValues = true
        }
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.files.add(project.file("consumer-rules.pro"))
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("com.lagradost.cloudstream3.InternalAPI")
                optIn("com.lagradost.cloudstream3.Prerelease")
            }
        }

        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(commonLibs.coroutinesCore)
                implementation(commonLibs.kotlinxSerialization)
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
                // CloudStream plugins compile against these; the host must provide them.
                api(androidLibs.cloudstreamNiceHttp)
                api(androidLibs.cloudstreamJackson)
                api(androidLibs.cloudstreamRhino)
                api(androidLibs.cloudstreamKsoup)
                api(androidLibs.cloudstreamCryptographyCore)
                api(androidLibs.cloudstreamCryptographyProvider)
                api(androidLibs.cloudstreamAtomicfu)
                api(androidLibs.cloudstreamKotlinxIo)
                api(androidLibs.cloudstreamAnnotation)
                api(androidLibs.jsoup)
                api(androidLibs.okhttpLib)
                api(commonLibs.ktorCore)
                api(commonLibs.kotlinx.datetime)
                api(commonLibs.kotlinxSerialization)
                api(commonLibs.coroutinesCore)
                api(kotlin("reflect"))
                // Plugin settings UIs and some parsers use these (see shims.md).
                api(androidLibs.appCompat)
                api(androidLibs.fragmentKtx)
                api(androidLibs.material)
                api(androidLibs.gson)
            }
        }

        getByName("androidHostTest") {
            kotlin.srcDir("src/jvmAndroidSharedTest/kotlin")
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
                implementation(commonLibs.ktorMock)
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
                // android.* / androidx.* stubs the vendored library compiles against.
                implementation(projects.sharedutils.kmpextensionloader)
                implementation(desktopLibs.kotlin.multiplatform.appdirs)
                // CloudStream plugins compile against these; the host must provide them.
                api(androidLibs.cloudstreamNiceHttp)
                api(androidLibs.cloudstreamJackson)
                api(androidLibs.cloudstreamRhino)
                api(androidLibs.cloudstreamKsoup)
                api(androidLibs.cloudstreamCryptographyCore)
                api(androidLibs.cloudstreamCryptographyProvider)
                api(androidLibs.cloudstreamAtomicfu)
                api(androidLibs.cloudstreamKotlinxIo)
                api(androidLibs.cloudstreamAnnotation) // resolves to annotation-jvm
                api(androidLibs.jsoup)
                api(androidLibs.okhttpLib)
                api(commonLibs.ktorCore)
                api(commonLibs.kotlinx.datetime)
                api(commonLibs.kotlinxSerialization)
                api(commonLibs.coroutinesCore)
                api(kotlin("reflect"))
                api(androidLibs.gson)
            }
        }

        jvmTest {
            kotlin.srcDir("src/jvmAndroidSharedTest/kotlin")
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
                implementation(commonLibs.ktorMock)
                implementation(androidLibs.okhttpMockWebServer)
            }
        }
    }
}
