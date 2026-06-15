plugins {
    `otaku-multiplatform-no-ios`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.koogintegration.customscraper"
}

kotlin {
    android {
        namespace = "com.programmersbox.koogintegration.customscraper"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(libs.kotlinxSerialization)
                implementation(libs.ktorCore)
                implementation(libs.koog.agents)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.ktorMock)
            }
        }

        androidMain {
            dependencies {
                // Android Ktor engine (OkHttp-based, optimized for Android)
                implementation(libs.ktorAndroid)
            }
        }

        jvmMain {
            dependencies {
                // OkHttp engine — works cross-platform on JVM; no CIO in catalog
                implementation(libs.ktorOkHttp)
            }
        }
    }
}
