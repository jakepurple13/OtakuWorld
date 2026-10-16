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
            }
        }

        androidMain {
            dependencies {
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
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
            }
        }
    }
}
