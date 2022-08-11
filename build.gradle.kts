// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Deps.kotlinVersion}")
        classpath("com.google.gms:google-services:4.3.13")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.1")
        classpath("com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:10.4.0")
        classpath("org.jetbrains.kotlin:kotlin-serialization:${Deps.kotlinVersion}")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${Deps.navVersion}")
    }
}

tasks.register("clean").configure {
    delete("build")
}