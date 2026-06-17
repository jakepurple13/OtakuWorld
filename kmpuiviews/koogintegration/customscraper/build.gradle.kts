plugins {
    `otaku-multiplatform-no-ios`
    id("kotlinx-serialization")
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
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
                implementation(libs.roomRuntime)
                implementation(libs.roomPaging)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.bundles.koinKmp)
                implementation(libs.compose.material3)
                implementation(libs.material.icons.extended)
                implementation(libs.runtime)
                implementation(libs.ui)
                implementation(libs.cmp.ui.util)
                implementation(libs.foundation)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.ktorMock)
                implementation(libs.coroutinesTest)
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
                //implementation("com.microsoft.playwright:playwright:1.60.0")
            }
        }
    }
}

dependencies {
    add("ksp", libs.roomCompiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}