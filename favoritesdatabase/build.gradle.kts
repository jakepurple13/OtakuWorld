import com.android.build.api.dsl.androidLibrary

plugins {
    `otaku-multiplatform`
    alias(libs.plugins.ksp)
    id("kotlinx-serialization")
    alias(libs.plugins.room)
}

/*android {
    room {
        schemaDirectory("$projectDir/schemas")
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    namespace = "com.programmersbox.favoritesdatabase"
}*/

/*dependencies {
    testImplementation(TestDeps.junit)
    androidTestImplementation(TestDeps.androidJunit)
    androidTestImplementation(TestDeps.androidEspresso)

    implementation(Deps.gsonutils)

    implementation(projects.models)
    implementation(libs.kotlinxSerialization)

    implementation(libs.bundles.roomLibs)
    ksp(libs.roomCompiler)
    implementation(libs.bundles.pagingLibs)
}*/

otakuDependencies {
    androidPackageName = "com.programmersbox.favoritesdatabase"
}

kotlin {
    androidLibrary {
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
        }

        androidMain.dependencies {
            implementation(projects.models)
        }
    }
}

dependencies {
    add("ksp", libs.roomCompiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}