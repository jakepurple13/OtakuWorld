plugins {
    `otaku-multiplatform-no-ios`
    id("kotlinx-serialization")
}

otakuDependencies {
    androidPackageName = "com.programmersbox.koogintegration.dynamictranslation"
}

kotlin {
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
        }
        androidMain.dependencies {
            implementation("com.github.adaptech-cz:tesseract4android:4.8.0")
            implementation("org.tensorflow:tensorflow-lite:2.14.0")
            implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
            implementation("org.opencv:opencv:4.9.0")
            // SentencePiece for NLLB tokenization — provides SentencePieceProcessor
            implementation("com.github.google.sentencepiece:libsentencepiece-android:0.2.0")
        }
    }
}
