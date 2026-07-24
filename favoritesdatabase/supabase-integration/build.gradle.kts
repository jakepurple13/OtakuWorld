plugins {
    `otaku-multiplatform`
    id("kotlinx-serialization")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

otakuDependencies {
    androidPackageName = "com.programmersbox.supabaseintegration"
}

compose.resources {
    // Exposes Res to other modules
    publicResClass = true
    packageOfResClass = "com.programmersbox.supabaseintegration" // Defines a strict, predictable package name
    generateResClass = always
}

kotlin {
    android {
        namespace = "com.programmersbox.supabaseintegration"
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
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
            implementation(commonLibs.koinNavigation3)
            implementation(commonLibs.coroutinesCore)
            implementation(commonLibs.kotlinxSerialization)
            implementation(commonLibs.kotlinx.datetime)
            implementation(commonLibs.connectivity.core)
            implementation(commonLibs.material.icons.extended)
            implementation(commonLibs.cmp.navigation3.ui)
            implementation(commonLibs.lifecycle.viewmodel.compose)
            implementation(commonLibs.multiplatform.lifecycle.runtime.compose)
            implementation(projects.favoritesdatabase)
            implementation(projects.kmpmodels)
            implementation(projects.sharedtools)
            implementation(projects.sharedcomponents)
            implementation(projects.datastore)
            implementation(commonLibs.bundles.datastoreLibs)
            implementation(commonLibs.qrose)
            implementation(commonLibs.scanner)
            implementation(compose.components.resources)
            implementation(commonLibs.roomRuntime)
        }
        androidMain.dependencies {
            implementation(commonLibs.connectivity.device)
            implementation(commonLibs.ktorOkHttp)
            implementation(androidLibs.workRuntimeKtx)
            implementation(androidLibs.koin.workmanager)
            implementation(androidLibs.androidx.security.crypto)
            implementation(androidLibs.androidx.credentials)
            implementation(androidLibs.androidx.credentials.play.services.auth)
        }
        jvmMain.dependencies {
            implementation(commonLibs.ktorCio)
            implementation(desktopLibs.connectivity.http)
        }
        jvmTest.dependencies {
            implementation(commonLibs.kotlin.test)
            implementation(commonLibs.coroutinesTest)
            implementation(commonLibs.roomRuntime)
            implementation(commonLibs.androidx.room.sqlite)
        }
        iosMain.dependencies {
            implementation(iosLibs.ktorDarwin)
            implementation(commonLibs.connectivity.device)
        }
    }
}
