plugins {
    `otaku-multiplatform`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.extensioninterfaces"
}

kotlin {
    android {
        namespace = "com.programmersbox.extensioninterfaces"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(commonLibs.kotlinxSerialization)
            }
        }

        commonTest {
            dependencies {
                implementation(commonLibs.kotlin.test)
            }
        }
    }
}
