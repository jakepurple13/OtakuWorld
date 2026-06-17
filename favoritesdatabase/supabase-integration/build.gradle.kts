plugins {
    `otaku-multiplatform`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.supabaseintegration"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform(commonLibs.supabase.bom))
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
            implementation(projects.favoritesdatabase)
            implementation(projects.kmpmodels)
        }
        androidMain.dependencies {
            // Re-declare BOM here so androidCompileClasspath resolves Supabase versions
            implementation(project.dependencies.platform(commonLibs.supabase.bom))
            implementation(commonLibs.connectivity.device)
            implementation(commonLibs.ktorOkHttp)
            implementation(androidLibs.workRuntimeKtx)
            implementation(androidLibs.koin.workmanager)
            implementation("androidx.security:security-crypto:1.1.0-alpha06")
        }
        jvmMain.dependencies {
            implementation(commonLibs.ktorCio)
        }
        iosMain.dependencies {
            implementation(iosLibs.ktorDarwin)
        }
    }
}
