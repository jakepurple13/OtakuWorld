import com.android.build.gradle.api.AndroidBasePlugin

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
        classpath(libs.google.services)
        classpath(libs.firebase.crashlytics.gradle)
        classpath("com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:${libs.versions.latestAboutLibsRelease.get()}")
        classpath("org.jetbrains.kotlin:kotlin-serialization:${libs.versions.kotlin.get()}")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.6.0-alpha09")
    }
}

allprojects {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
        //maven { url "https://dl.bintray.com/piasy/maven" }
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

subprojects {
    afterEvaluate {
        if (plugins.findPlugin(AndroidBasePlugin::class) != null) {
            configureAndroidBasePlugin()
        }
    }
}

fun Project.configureAndroidBasePlugin() {
    extensions.findByType<com.android.build.gradle.BaseExtension>()?.apply {

    }
}

tasks.register("clean").configure {
    delete("build")
}

plugins {
    id("io.github.jakepurple13.ProjectInfo") version "1.1.1"
}

projectInfo {
    filter {
        exclude("otakumanager/**")
        excludeFileTypes("png", "webp", "ttf", "json")
    }
    showTopCount = 3
}