plugins {
    `otaku-multiplatform`
    id("kotlinx-serialization")
    alias(libs.plugins.ksp)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

otakuDependencies {
    androidPackageName = "com.programmersbox.sharedcomponents"
}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    android {
        namespace = "com.programmersbox.sharedcomponents"
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(commonLibs.compose.material3)
                implementation(commonLibs.material.icons.extended)
                implementation(commonLibs.runtime)
                implementation(commonLibs.ui)
                implementation(commonLibs.cmp.ui.util)
                implementation(commonLibs.foundation)
                implementation(commonLibs.kotlinxSerialization)
                implementation(commonLibs.kotlinx.datetime)
                implementation(commonLibs.filekit.core)
                implementation(commonLibs.filekit.dialogs.compose)
                implementation(project.dependencies.platform(commonLibs.koin.bom))
                implementation(commonLibs.bundles.koinKmp)
                implementation(commonLibs.androidx.navigation3.runtime)
                implementation(commonLibs.androidx.navigationevent)
                implementation(commonLibs.cmp.navigation3.ui)
                implementation(commonLibs.qrose)
                implementation(commonLibs.scanner)
                implementation(commonLibs.multiplatform.lifecycle.runtime.compose)
                implementation(commonLibs.lifecycle.viewmodel.compose)
                implementation(projects.showcase.annotations)
            }
        }

        commonTest {
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
                implementation(androidLibs.barcode.scanning)
                api(androidLibs.coroutinesPlayServices)
            }
        }

        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP’s default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }

        jvmMain {
            dependencies {
                implementation(desktopLibs.core)
                implementation(desktopLibs.javase)
                api(desktopLibs.kotlin.multiplatform.appdirs)
                implementation(desktopLibs.camerak)
                implementation(desktopLibs.camerak.image.saver)
                implementation(desktopLibs.camerak.qr.scanner)
                implementation(desktopLibs.camerak.ocr)
                implementation(desktopLibs.camerak.analyzer)
            }
        }
    }
}

dependencies {
    if (System.getenv("CI") == null) {
        add("kspJvm", projects.showcase.processor)
    }
}

ksp {
    arg("showcaseModuleId", "sharedcomponents")
}