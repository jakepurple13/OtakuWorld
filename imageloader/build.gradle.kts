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
    kotlinCompilerPlugin.set("androidx.compose.compiler:compiler:${libs.versions.jetpackCompiler.get()}")
}

android {
    namespace = "com.programmersbox.imageloader"
}