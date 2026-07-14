import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.compose.hot-reload")
    id("kotlinx-serialization")
}

configurations.all {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
}

kotlin {
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xwhen-guards")
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinStLib)
            implementation(projects.kmpuiviews)
            implementation(commonLibs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.uiUtil)
            implementation(compose.foundation)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(compose.components.resources)
            implementation(commonLibs.material.kolor)

            implementation(project.dependencies.platform(commonLibs.koin.bom))
            implementation(commonLibs.bundles.koinKmp)

            implementation(projects.favoritesdatabase)
            implementation(projects.datastore)
            implementation(projects.kmpmodels)
            implementation(projects.novelworld.shared)
            implementation(commonLibs.bundles.datastoreLibs)
            implementation(commonLibs.coroutinesCore)
            implementation(desktopLibs.kotlinx.coroutines.swing)
            api(commonLibs.androidx.navigation3.runtime)
            api(commonLibs.filekit.core)
            api(commonLibs.filekit.dialogs.compose)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(desktopLibs.kotlinx.coroutines.swing)
        }
    }
}

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

compose.desktop {
    application {
        mainClass = "com.programmersbox.desktop.MainKt"

        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.programmersbox.desktop"
            packageVersion = "1.0.0"
        }
    }
}
