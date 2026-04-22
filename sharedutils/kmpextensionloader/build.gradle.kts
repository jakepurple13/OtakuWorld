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
    }
}