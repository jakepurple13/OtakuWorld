import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

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
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                        project.layout.buildDirectory.get().asFile.absolutePath + "/compose_metrics"
            )
        }
    }
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
    composeFeatureFlags()
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

fun Project.composeFeatureFlags() {
    extensions.findByType(ComposeCompilerGradlePluginExtension::class.java)?.apply {
        featureFlags.add(ComposeFeatureFlag.StrongSkipping)
    }
}

tasks.register("clean").configure {
    delete("build")
}

plugins {
    id("io.github.jakepurple13.ProjectInfo") version "1.1.1"
    //id("org.jetbrains.compose") version libs.versions.jetbrainsCompiler apply false
    alias(libs.plugins.compose.compiler) apply false
    //alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin) apply false
    alias(libs.plugins.google.firebase.performance) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    id("com.squareup.wire") version "5.3.6" apply false
    id("org.jetbrains.compose.hot-reload") version "1.0.0-beta04" apply false
    alias(libs.plugins.buildKonfig) apply false
}

projectInfo {
    filter {
        exclude("otakumanager/**")
        excludeFileTypes("png", "webp", "ttf", "json")
    }
    showTopCount = 3
}