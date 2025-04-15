package com.programmersbox.mangaworld.reader.compose

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.programmersbox.sharedutils.TranslateItems
import kotlinx.coroutines.tasks.await

class TranslatorStuff {
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    val translateItems = TranslateItems()

    suspend fun getStuff(
        inputImage: InputImage,
    ): List<TranslationItem>? {
        return runCatching { recognizer.process(inputImage).await() }
            .mapCatching {
                it.textBlocks.map {
                    TranslationItem(
                        text = "",//translateItems.translate(it.text),
                        box = it
                            .boundingBox
                            ?.toComposeRect()
                            ?: Rect.Zero,
                    )
                    /*it.lines.flatMap {
                        TranslationItem(
                            text = translateItems.translate(it.text),
                            box = it
                                .boundingBox
                                ?.toComposeRect()
                                ?: Rect.Zero,
                        )
                        it.elements.mapIndexed { index, it ->
                            TranslationItem(
                                text = translateItems.translate(it.text),
                                box = it
                                    .boundingBox
                                    ?.toComposeRect()
                                    ?: Rect.Zero,
                            )
                        }
                    }*/
                }
            }
            .mapCatching {
                it
            }
            .onFailure { it.printStackTrace() }
            .getOrNull()
    }
}

data class TranslationItem(
    val text: String,
    val box: Rect,
)
