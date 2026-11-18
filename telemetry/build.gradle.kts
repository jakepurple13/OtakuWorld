plugins {
    `otaku-multiplatform`
}

otakuDependencies {
    androidPackageName = "com.programmersbox.telemetry"
}

kotlin {
    android {
        namespace = "com.programmersbox.telemetry"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project.dependencies.platform(commonLibs.koin.bom))
                api(commonLibs.koinCores)
            }
        }

        commonTest {
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.koin.test)
            }
        }
    }
}
