// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    dependencies {
        classpath(libs.google.services)
        classpath(libs.firebase.crashlytics.gradle)
        classpath("com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:${libs.versions.latestAboutLibsRelease.get()}")
        classpath("org.jetbrains.kotlin:kotlin-serialization:${libs.versions.kotlin.get()}")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
        classpath(libs.navigation.safe.args.gradle.plugin)
    }
}

subprojects {
    afterEvaluate {
        when {
            plugins.hasPlugin("otaku-library") -> {
                println("Otaku Library")
            }

            plugins.hasPlugin("otaku-application") -> configureAndroidBasePlugin()

            plugins.hasPlugin("otaku-multiplatform") -> {
                println("Otaku Multiplatform Library")
            }
        }
    }
}

fun Project.configureAndroidBasePlugin() {
    extensions.findByType<com.android.build.gradle.BaseExtension>()?.apply {
        compileOptions {
            isCoreLibraryDesugaringEnabled = true
        }

        dependencies {
            val coreLibraryDesugaring by configurations
            coreLibraryDesugaring(libs.coreLibraryDesugaring)
        }
    }
}

tasks.register("clean").configure {
    delete("build")
}

plugins {
    id("io.github.jakepurple13.ProjectInfo") version "1.1.1"
    id("org.jetbrains.compose") version libs.versions.jetbrainsCompiler apply false
}

projectInfo {
    filter {
        exclude("otakumanager/**")
        excludeFileTypes("png", "webp", "ttf", "json")
    }
    showTopCount = 3
}