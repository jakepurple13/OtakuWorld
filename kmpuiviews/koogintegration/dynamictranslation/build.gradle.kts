plugins {
    `otaku-multiplatform-no-ios`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.koogintegration.dynamictranslation"
}

kotlin {
    android {
        namespace = "com.programmersbox.koogintegration.dynamictranslation"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinStLib)
            implementation(libs.koog.agents)
            implementation(libs.kotlinxSerialization)
            implementation(project(":kmpuiviews:koogintegration"))
        }
        jvmMain.dependencies {
            implementation("net.sourceforge.tess4j:tess4j:5.13.0")
            implementation("org.openpnp:opencv:4.9.0-0")

            val rapidOcrVersion = "0.0.7" // Or check Maven Central for the latest
            val modelVersion = "1.2.2"

            // 1. Core RapidOCR Library
            implementation("io.github.mymonstercat:rapidocr:$rapidOcrVersion")
            implementation("io.github.mymonstercat:rapidocr-common:$rapidOcrVersion")

            // 2. The pre-trained ONNX OCR Models
            implementation("io.github.mymonstercat:rapidocr-onnx-models:$modelVersion")

            // 3. The magic bullet: Native ONNX runtime for Apple Silicon (Mac ARM64)
            implementation("io.github.mymonstercat:rapidocr-onnx-macosx-arm64:$modelVersion")
        }
        androidMain.dependencies {
            implementation("com.github.adaptech-cz:Tesseract4Android:4.8.0")
            implementation("org.tensorflow:tensorflow-lite:2.14.0")
            implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
            implementation("org.opencv:opencv:4.9.0")
            implementation("ai.koog:prompt-executor-litert-client:1.0.0-beta-preview7")
            // SentencePiece via TFLite Support — SentencePieceTokenizer is bundled in tensorflow-lite-support
        }
    }
}
