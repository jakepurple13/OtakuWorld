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
            dependencies {
                implementation(commonLibs.ktorOkHttp)
                implementation(projects.datastore)
                implementation(project.dependencies.platform(commonLibs.koin.bom))
                implementation(commonLibs.koinCores)
                implementation(androidLibs.okhttpLib)
                implementation(androidLibs.okhttpDns)
                implementation(androidLibs.okhttpLogging)
                implementation(androidLibs.brotli)
                implementation(androidLibs.jsoup)
                implementation(androidLibs.injekt)
                implementation(androidLibs.rxjava1)
                implementation(androidLibs.quickjsAndroid)
                implementation(androidLibs.squareLogcat)
                implementation(androidLibs.preference)
                implementation(androidLibs.androidxWebkit)
            }
        }

        getByName("androidHostTest") {
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
                implementation(androidLibs.okhttpMockWebServer)
            }
        }
    }
}
