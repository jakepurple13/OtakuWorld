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
            implementation(commonLibs.kotlinxSerialization)
            implementation(commonLibs.roomRuntime)
            implementation(commonLibs.roomPaging)
            implementation(projects.kmpmodels)
            implementation(commonLibs.kotlinx.datetime)
        }

        jvmMain.dependencies {
            implementation(commonLibs.androidx.room.sqlite)
            implementation(desktopLibs.kotlin.multiplatform.appdirs)
        }

        jvmTest.dependencies {
            implementation(commonLibs.kotlin.test)
            implementation(commonLibs.coroutinesTest)
        }

        androidMain.dependencies {
            implementation(projects.models)
        }
    }
}

dependencies {
    add("ksp", commonLibs.roomCompiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}