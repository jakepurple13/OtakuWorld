import dev.nucleusframework.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.compose.hot-reload")
    id("kotlinx-serialization")
    id("dev.nucleusframework")
}

apply(from = "vlc.gradle.kts")

compose.resources {
    packageOfResClass = "com.programmersbox.desktop"
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
            implementation(projects.animeworld.shared)
            implementation(projects.sourceUtilities.aniyomi)
            implementation(projects.sourceUtilities.cloudstream)
            implementation(commonLibs.bundles.datastoreLibs)
            implementation(commonLibs.coroutinesCore)
            implementation(desktopLibs.kotlinx.coroutines.swing)
            api(commonLibs.androidx.navigation3.runtime)
            api(commonLibs.filekit.core)
            api(commonLibs.filekit.dialogs.compose)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            // CMP 1.13 ships Skiko natives only in skiko-awt-runtime-all, but Nucleus packaging only
            // extracts libskiko from the per-platform runtime jar (while still pointing skiko.library.path
            // at $APPDIR). Pull in the current platform jar so the packaged app can load Skiko.
            // Version comes from skiko-awt's dependency constraints.
            runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-${compose.desktop.currentOs.substringAfter("desktop-jvm-").substringBefore(':')}")
            implementation(desktopLibs.kotlinx.coroutines.swing)
        }

        jvmTest.dependencies {
            implementation(commonLibs.kotlin.test)
            implementation(androidLibs.okhttpMockWebServer)
            // AnimeWorldEndToEndNetworkTest (opt-in) loads the AnimeOnsen fixture extension.
            implementation(commonLibs.ktorMock)
            // CloudstreamEndToEndNetworkTest (opt-in) talks to a real repo like the app does.
            implementation(commonLibs.ktorOkHttp)
            implementation(androidLibs.okhttpLib)
            implementation(projects.sharedutils.kmpextensionloader)
        }
        // DashDownloadPlaybackTest downloads the DASH fixture that :animeworld:shared's tests use.
        jvmTest {
            resources.srcDir("../shared/src/jvmTest/resources")
        }
    }
}

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

val desktopVersion = System.getenv("DESKTOP_VERSION") ?: AppInfo.otakuVersionName
val numericVersion = desktopVersion.substringBefore('-')

// Hot reload's hotRunJvm defaults its main class from compose.desktop.application, which is no longer applied.
tasks.withType<org.jetbrains.compose.reload.gradle.ComposeHotRun>().configureEach {
    mainClass.set("com.programmersbox.desktop.MainKt")
}

nucleus.application {
    mainClass = "com.programmersbox.desktop.MainKt"

    jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

    if (System.getProperty("os.name").contains("Mac")) {
        jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
    }

    nativeDistributions {
        targetFormats(
            *listOfNotNull(
                TargetFormat.Dmg,
                TargetFormat.Zip.takeIf { System.getProperty("os.name").contains("Mac") },
                TargetFormat.Nsis,
                TargetFormat.Deb,
                TargetFormat.AppImage,
            ).toTypedArray()
        )
        packageName = "AnimeWorld"
        packageVersion = desktopVersion
        homepage = "https://github.com/jakepurple13/OtakuWorld"

        modules("jdk.unsupported")

        // libVLC for the video player lands in appResources/<os>-<arch>/vlc (see vlc.gradle.kts).
        appResourcesRootDir.set(layout.buildDirectory.dir("appResources"))

        windows {
            packageVersion = numericVersion
            iconFile.set(project.file("icons/icon.ico"))
        }
        macOS {
            packageVersion = numericVersion
            iconFile.set(project.file("icons/icon.icns"))
        }
        linux {
            iconFile.set(project.file("icons/icon.png"))
        }
    }
}

tasks.named { it == "prepareAppResources" || it == "run" }.configureEach {
    dependsOn("prepareVlc")
}

// CloudstreamFixture downloads its plugin once into the Gradle user home; -P overrides exist for offline checks.
tasks.named<Test>("jvmTest") {
    systemProperty(
        "cloudstream.fixture.cache",
        providers.gradleProperty("cloudstream.fixture.cache").orNull
            ?: File(gradle.gradleUserHomeDir, "caches/otaku-test-fixtures/cloudstream").absolutePath,
    )
    providers.gradleProperty("cloudstream.fixture.url").orNull?.let { systemProperty("cloudstream.fixture.url", it) }
}
