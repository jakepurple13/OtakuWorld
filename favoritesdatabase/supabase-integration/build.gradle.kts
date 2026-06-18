plugins {
    `otaku-multiplatform`
    id("kotlinx-serialization")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

otakuDependencies {
    androidPackageName = "com.programmersbox.supabaseintegration"
}

kotlin {
    android {
        namespace = "com.programmersbox.supabaseintegration"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform(commonLibs.supabase.bom))
            implementation(commonLibs.compose.material3)
            implementation(commonLibs.supabase.postgrest)
            implementation(commonLibs.supabase.auth)
            implementation(commonLibs.supabase.realtime)
            implementation(commonLibs.supabase.storage)
            implementation(commonLibs.supabase.composeAuth)
            implementation(project.dependencies.platform(commonLibs.koin.bom))
            implementation(commonLibs.koinCores)
            implementation(commonLibs.koinComposeKmp)
            implementation(commonLibs.koinViewModel)
            implementation(commonLibs.coroutinesCore)
            implementation(commonLibs.kotlinxSerialization)
            implementation(commonLibs.kotlinx.datetime)
            implementation(commonLibs.connectivity.core)
            implementation(commonLibs.cmp.navigation3.ui)
            implementation(commonLibs.lifecycle.viewmodel.compose)
            implementation(commonLibs.multiplatform.lifecycle.runtime.compose)
            implementation(projects.favoritesdatabase)
            implementation(projects.kmpmodels)
        }
        androidMain.dependencies {
            implementation(commonLibs.connectivity.device)
            implementation(commonLibs.ktorOkHttp)
            implementation(androidLibs.workRuntimeKtx)
            implementation(androidLibs.koin.workmanager)
            implementation("androidx.security:security-crypto:1.1.0")
        }
        jvmMain.dependencies {
            implementation(commonLibs.ktorCio)
            implementation(desktopLibs.connectivity.http)
        }
        iosMain.dependencies {
            implementation(iosLibs.ktorDarwin)
            implementation(commonLibs.connectivity.device)
        }
    }
}
