repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `version-catalog`
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

        register("otaku-benchmark") {
            id = "otaku-benchmark"
            implementationClass = "plugins.BenchmarkPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(libs.kotlinStLib)
    implementation(libs.gradle)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation(libs.easylauncher)
    implementation(libs.androidx.baselineprofile.gradle.plugin)
}