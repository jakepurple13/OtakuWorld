// bare id(...) — buildSrc convention plugins already apply this plugin unversioned; alias(libs.plugins.*) conflicts with that.
plugins {
    id("kotlin-multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.programmersbox.showcase.annotations"
        compileSdk = AppInfo.compileVersion
        minSdk = AppInfo.minimumSdk
    }

    val xcfName = "sharedKit"

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = xcfName
            isStatic = true
        }
    }

    jvm()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(commonLibs.runtime)
        }
    }
}
