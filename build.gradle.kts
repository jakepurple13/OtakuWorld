import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

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
    }
}

subprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-coroutines")) {
                useVersion("1.10.2")
            }
        }
    }
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
            coreLibraryDesugaring(androidLibs.coreLibraryDesugaring)
        }
    }
}

fun Project.composeFeatureFlags() {
    extensions.findByType(ComposeCompilerGradlePluginExtension::class.java)?.apply {

    }
}

tasks.register("clean").configure {
    delete("build")
}

plugins {
    //id("io.github.jakepurple13.ProjectInfo") version "1.1.1"
    //id("org.jetbrains.compose") version libs.versions.jetbrainsCompiler apply false
    alias(libs.plugins.compose.compiler) apply false
    //alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin) apply false
    alias(libs.plugins.google.firebase.performance) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    id("com.squareup.wire") version "6.4.0" apply false
    id("org.jetbrains.compose.hot-reload") version "1.1.1" apply false
    alias(libs.plugins.buildKonfig) apply false
    alias(libs.plugins.koin.compiler) apply false
    //alias(libs.plugins.hotswan.compiler) apply false
    alias(libs.plugins.kotzilla) apply false
}

//To run:
// ./gradlew createModule --no-daemon --console=plain
tasks.register("createModule") {
    group = "scaffolding"
    description = "Interactively creates a new Compose Multiplatform module."

    doLast {
        fun readInput(prompt: String): String {
            print(prompt)
            System.out.flush() // Force the prompt to display before waiting
            return readlnOrNull()?.trim() ?: ""
        }

        println("\n--- Compose Multiplatform Module Scaffolder ---")

        // 1. Module Location Selection
        val parentOptions = mutableListOf("root")
        // Get all existing subprojects (modules)
        parentOptions.addAll(rootProject.subprojects.map { it.path }.sorted())

        println("Select the parent location for the new module:")
        parentOptions.forEachIndexed { index, path ->
            println("$index. $path")
        }

        var locationIndex = -1
        while (locationIndex !in parentOptions.indices) {
            val inputStr = readInput("Enter location number [0-${parentOptions.size - 1}]: ")
            locationIndex = inputStr.toIntOrNull() ?: -1
        }
        val selectedParentPath = parentOptions[locationIndex]
        val isRoot = selectedParentPath == "root"

        var packageName = ""
        while (packageName.isEmpty()) {
            packageName = readInput("Enter new package name (e.g., com.example): ")
        }

        // 2. Module Naming
        var moduleName = ""
        while (moduleName.isEmpty()) {
            moduleName = readInput("Enter new module name (e.g., my-feature): ")
        }

        // 3. Platform Selection
        println("\nSelect platforms to support (comma-separated, e.g., 1,2,3):")
        println("1. Android")
        println("2. iOS")
        println("3. Desktop (JVM)")
        println("4. Web (Wasm/JS)")

        var platformsInput = ""
        while (platformsInput.isEmpty()) {
            platformsInput = readInput("Enter platforms: ")
        }

        val selectedPlatforms = platformsInput.split(",").map { it.trim() }
        val hasAndroid = selectedPlatforms.contains("1")
        val hasIos = selectedPlatforms.contains("2")
        val hasDesktop = selectedPlatforms.contains("3")
        val hasWeb = selectedPlatforms.contains("4")

        // 4. Resolve Paths and Validate
        val logicalPath = if (isRoot) ":$moduleName" else "$selectedParentPath:$moduleName"
        val parentDir = if (isRoot) {
            rootProject.projectDir
        } else {
            rootProject.project(selectedParentPath).projectDir
        }
        val moduleDir = File(parentDir, moduleName)

        if (moduleDir.exists()) {
            error("ABORTING: A directory already exists at ${moduleDir.absolutePath}")
        }

        println("\nScaffolding module '$logicalPath'...")

        // 5. Source Set Folder Structure
        val srcDir = File(moduleDir, "src")
        File(srcDir, "commonMain/kotlin").mkdirs()
        if (hasAndroid) File(srcDir, "androidMain/kotlin").mkdirs()
        if (hasIos) File(srcDir, "iosMain/kotlin").mkdirs()
        if (hasDesktop) File(srcDir, "desktopMain/kotlin").mkdirs()
        if (hasWeb) File(srcDir, "wasmJsMain/kotlin").mkdirs()

        // 6. Generate build.gradle.kts
        val buildGradleFile = File(moduleDir, "build.gradle.kts")
        val buildGradleContent = buildString {
            appendLine("plugins {")
            if (hasIos) {
                appendLine("    `otaku-multiplatform`")
            } else {
                appendLine("    `otaku-multiplatform-no-ios`")
            }
            appendLine("}")
            appendLine()

            val safeNamespace = moduleName.replace("[^a-zA-Z0-9]".toRegex(), "")

            appendLine("otakuDependencies {")
            if (hasAndroid) {
                appendLine("    androidPackageName = \"${packageName}.${safeNamespace}\"")
            }
            appendLine("}")

            appendLine("kotlin {")

            if (hasAndroid) {
                appendLine()
                appendLine("    android {")
                // Generate a safe namespace removing hyphens/special chars
                appendLine("        namespace = \"${packageName}.${safeNamespace}\"")
                appendLine("    }")
            }

            appendLine()
            appendLine("    sourceSets {")
            appendLine("        val commonMain by getting {")
            appendLine("            dependencies {")
            appendLine("                //implementation(compose.runtime)")
            appendLine("                //implementation(compose.foundation)")
            appendLine("                // implementation(compose.material3)")
            appendLine("                // implementation(compose.components.resources)")
            appendLine("            }")
            appendLine("        }")
            if (hasAndroid) {
                appendLine("        val androidMain by getting")
            }
            if (hasDesktop) {
                appendLine("        val desktopMain by getting")
            }
            if (hasWeb) {
                appendLine("        val wasmJsMain by getting")
            }
            if (hasIos) {
                appendLine("        val iosMain by creating {")
                appendLine("            dependsOn(commonMain)")
                appendLine("        }")
            }
            appendLine("    }")
            appendLine("}")
        }
        buildGradleFile.writeText(buildGradleContent)

        // 7. Automatic Registration in settings.gradle.kts
        val settingsFile = File(rootProject.projectDir, "settings.gradle.kts")
        if (settingsFile.exists()) {
            settingsFile.appendText("\ninclude(\"$logicalPath\")\n")
            println("Registered '$logicalPath' in settings.gradle.kts")
        } else {
            println("WARNING: settings.gradle.kts not found in root. You must manually include this module.")
        }

        println("SUCCESS: Module '$logicalPath' created at ${moduleDir.absolutePath}!")
        println("Sync your Gradle project to complete setup.")
    }
}

/*
projectInfo {
    filter {
        exclude("otakumanager/**")
        excludeFileTypes("png", "webp", "ttf", "json")
    }
    showTopCount = 3
}*/
