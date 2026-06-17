plugins {
    id("otaku-multiplatform")
}

otakuDependencies {
    commonDependencies {
        implementation(compose.dependencies.runtime)
        api(commonLibs.kamel.image)
    }

    androidDependencies {
        api(commonLibs.ktorAndroid)
    }
}

compose {
    //kotlinCompilerPlugin.set("org.jetbrains.kotlin.plugin.compose:1.6.10-beta03"/*"androidx.compose.compiler:compiler:${libs.versions.jetpackCompiler.get()}"*/)
    kotlinCompilerPlugin = "org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:${libs.versions.kotlin.get()}"
}

android {
    namespace = "com.programmersbox.imageloader"
}