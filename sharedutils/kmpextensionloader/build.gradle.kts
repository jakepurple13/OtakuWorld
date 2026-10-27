plugins {
    `otaku-multiplatform`
}

otakuDependencies {
    androidPackageName = "com.programmersbox.kmpextensionloader"
}

kotlin {
    android {
        namespace = "com.programmersbox.kmpextensionloader"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(commonLibs.coroutinesCore)
                implementation(projects.kmpmodels)
            }
        }

        androidMain {
            dependencies {
                implementation(projects.models)
            }
        }

        jvmMain {
            dependencies {
                implementation("net.dongliu:apk-parser:2.6.10")
                // Maven Central release of the ThexXTURBOXx fork; v76 (JitPack) lacks dontSanitizeNames.
                implementation("de.femtopedia.dex2jar:dex-translator:2.4.38")
                implementation("org.ow2.asm:asm:9.11.1")
                // Android ships org.json in the framework; extensions call it directly.
                implementation("org.json:json:20251224")
                implementation(desktopLibs.kotlin.multiplatform.appdirs)
            }
        }

        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
    }
}