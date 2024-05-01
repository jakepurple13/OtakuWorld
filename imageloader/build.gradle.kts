plugins {
    id("otaku-multiplatform")
}

otakuDependencies {
    commonDependencies {
        implementation(compose.dependencies.runtime)
        api(libs.kamel.image)
    }

    androidDependencies {
        api(libs.ktorAndroid)
    }
}

compose {
    //kotlinCompilerPlugin.set("org.jetbrains.kotlin.plugin.compose:1.6.10-beta03"/*"androidx.compose.compiler:compiler:${libs.versions.jetpackCompiler.get()}"*/)
    kotlinCompilerPlugin = "org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:2.0.0-RC2"
}

android {
    namespace = "com.programmersbox.imageloader"
}