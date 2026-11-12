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
            // FTS5 (favorites/notes search) is not compiled into the platform SQLite.
            // api: DatabaseBuilder.build is inline, so callers reference the driver too.
            api(commonLibs.androidx.room.sqlite)
        }
    }
}

dependencies {
    add("kspJvm", commonLibs.roomCompiler)
    add("kspAndroid", commonLibs.roomCompiler)
    if (AppInfo.includeIos(project)) {
        add("kspIosSimulatorArm64", commonLibs.roomCompiler)
        add("kspIosArm64", commonLibs.roomCompiler)
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}