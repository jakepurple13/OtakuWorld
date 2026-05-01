plugins {
    `otaku-multiplatform`
    alias(libs.plugins.ksp)
    id("kotlinx-serialization")
    alias(libs.plugins.room)
}

otakuDependencies {
    androidPackageName = "com.programmersbox.favoritesdatabase"
}

kotlin {
    android {
        namespace = "com.programmersbox.favoritesdatabase"
    }

    sourceSets {
        commonMain.dependencies {
            //implementation(projects.models)
            implementation(libs.kotlinxSerialization)
            implementation(libs.roomRuntime)
            implementation(libs.roomPaging)
            implementation(projects.kmpmodels)
            implementation(libs.kotlinx.datetime)
        }

        jvmMain.dependencies {
            implementation(libs.androidx.room.sqlite)
            implementation(libs.kotlin.multiplatform.appdirs)
        }

        androidMain.dependencies {
            implementation(projects.models)
        }
    }
}

dependencies {
    add("ksp", libs.roomCompiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}