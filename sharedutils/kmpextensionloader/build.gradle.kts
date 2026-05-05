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
                implementation(libs.coroutinesCore)
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
                implementation("com.github.ThexXTURBOXx.dex2jar:dex-tools:v76")
                implementation("com.github.ThexXTURBOXx.dex2jar:d2j-base-cmd:76")
                implementation(libs.kotlin.multiplatform.appdirs)
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