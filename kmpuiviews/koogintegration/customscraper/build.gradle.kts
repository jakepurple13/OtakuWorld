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
                implementation(commonLibs.kotlinxSerialization)
                implementation(commonLibs.ktorCore)
                implementation(commonLibs.koog.agents)
                implementation(commonLibs.roomRuntime)
                implementation(commonLibs.roomPaging)
                implementation(project.dependencies.platform(commonLibs.koin.bom))
                implementation(commonLibs.bundles.koinKmp)
                implementation(commonLibs.compose.material3)
                implementation(commonLibs.material.icons.extended)
                implementation(commonLibs.runtime)
                implementation(commonLibs.ui)
                implementation(commonLibs.cmp.ui.util)
                implementation(commonLibs.foundation)
            }
        }

        commonTest {
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.ktorMock)
                implementation(commonLibs.coroutinesTest)
            }
        }

        androidMain {
            dependencies {
                // Android Ktor engine (OkHttp-based, optimized for Android)
                implementation(commonLibs.ktorAndroid)
            }
        }

        jvmMain {
            dependencies {
                // OkHttp engine — works cross-platform on JVM; no CIO in catalog
                implementation(commonLibs.ktorOkHttp)
                //implementation("com.microsoft.playwright:playwright:1.60.0")
            }
        }
    }
}

dependencies {
    add("ksp", commonLibs.roomCompiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}