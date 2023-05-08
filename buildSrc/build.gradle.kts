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
    }
}


dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib"))
    implementation(libs.gradle)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
}