@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `version-catalog`
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.kotlinGp)
    }
}

gradlePlugin {
    plugins {
        register("otaku-application") {
            id = "otaku-application"
            implementationClass = "plugins.AndroidApplicationPlugin"
        }

        register("otaku-library") {
            id = "otaku-library"
            implementationClass = "plugins.AndroidLibraryPlugin"
        }

        register("otaku-protobuf") {
            id = "otaku-protobuf"
            implementationClass = "plugins.OtakuProtobufPlugin"
        }

        register("otaku-source-application") {
            id = "otaku-source-application"
            implementationClass = "plugins.AndroidSourcePlugin"
        }

        register("otaku-easylauncher") {
            id = "otaku-easylauncher"
            implementationClass = "plugins.EasyLauncherSetup"
        }

        register("otaku-multiplatform") {
            id = "otaku-multiplatform"
            implementationClass = "plugins.MultiplatformLibraryPlugin"
        }

        register("otaku-multiplatform-application") {
            id = "otaku-multiplatform-application"
            implementationClass = "plugins.MultiplatformApplicationPlugin"
        }

        register("otaku-benchmark") {
            id = "otaku-benchmark"
            implementationClass = "plugins.BenchmarkPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(libs.kotlinStLib)
    implementation(libs.gradle)
    implementation(libs.kotlinGp)
    implementation(libs.easylauncher)
    implementation(libs.androidx.baselineprofile.gradle.plugin)
    implementation(libs.protobuf.gradle.plugin)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

kotlin {
    abiValidation {
        enabled.set(true)
    }
}