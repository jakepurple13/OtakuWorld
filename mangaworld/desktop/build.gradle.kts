import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

plugins {
    `otaku-multiplatform-application`
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.compose.hot-reload")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinStLib)
            implementation(projects.kmpuiviews)
            implementation("org.jetbrains.compose.material3:material3:1.9.0-SNAPSHOT+release-1-8-data-source-prototype")
            //implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.uiUtil)
            implementation(compose.foundation)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(compose.components.resources)
            implementation(libs.material.kolor)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.bundles.koinKmp)

            implementation(projects.favoritesdatabase)
            implementation(projects.datastore)
            implementation(projects.datastore.mangasettings)
            implementation(projects.kmpmodels)
            implementation(projects.mangaworld.shared)
            implementation(libs.bundles.datastoreLibs)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
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
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED") // recommended but not necessary

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
