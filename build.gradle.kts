import com.android.build.gradle.internal.dsl.BaseAppModuleExtension

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
        configureKotlinCompile()
        when {
            plugins.hasPlugin("otaku-library") -> configureAndroidLibraryPlugin()
            plugins.hasPlugin("otaku-application") -> configureAndroidBasePlugin()
        }

        afterEvaluate {
            useGoogleType()
        }
    }
}

fun Project.useGoogleType() {
    extensions.findByType<BaseAppModuleExtension>()?.apply {
        applicationVariants.forEach { variant ->
            println(variant.name)
            val googleTask = tasks.findByName("process${variant.name.capitalize()}GoogleServices")
            googleTask?.enabled = "noFirebase" != variant.flavorName
        }
    }
}

fun Project.configureAndroidBasePlugin() {
    extensions.findByType<com.android.build.gradle.BaseExtension>()?.apply {
        compileSdkVersion(AppInfo.compileVersion)
        buildToolsVersion(AppInfo.buildVersion)

        defaultConfig {
            minSdk = AppInfo.minimumSdk
            targetSdk = AppInfo.targetSdk
            versionCode = 1
            versionName = AppInfo.otakuVersionName

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            isCoreLibraryDesugaringEnabled = true
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        buildFeatures.compose = true

        composeOptions {
            useLiveLiterals = true
            kotlinCompilerExtensionVersion = libs.versions.jetpackCompiler.get()
        }

        packagingOptions {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}:"
            }
        }

        buildTypes {
            getByName("release") {
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
            }
            getByName("debug") {
                extra["enableCrashlytics"] = false
            }
            create("beta") {
                initWith(getByName("debug"))
                matchingFallbacks.addAll(listOf("debug", "release"))
                isDebuggable = false
            }
        }

        flavorDimensions("version")
        productFlavors {
            create("noFirebase") {
                dimension("version")
                versionNameSuffix("-noFirebase")
                applicationIdSuffix(".noFirebase")
            }
            create("full") {
                dimension("version")
            }
        }

        dependencies {
            val coreLibraryDesugaring by configurations
            coreLibraryDesugaring(libs.coreLibraryDesugaring)
        }
    }
}

fun Project.configureKotlinCompile() {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

fun Project.configureAndroidLibraryPlugin() {
    extensions.findByType<com.android.build.gradle.BaseExtension>()?.apply {
        compileSdkVersion(AppInfo.compileVersion)
        buildToolsVersion(AppInfo.buildVersion)

        defaultConfig {
            minSdk = AppInfo.minimumSdk
            targetSdk = AppInfo.targetSdk
            versionCode = 1
            versionName = AppInfo.otakuVersionName

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        packagingOptions {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}:"
            }
        }

        dependencies {

        }
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