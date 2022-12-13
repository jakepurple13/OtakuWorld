package com.programmersbox.sharedutils

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.*
import kotlinx.coroutines.tasks.await

class TranslateItems {
    private var englishTranslator: Translator? = null

    fun translateDescription(textToTranslate: String, progress: (Boolean) -> Unit, translatedText: (String) -> Unit) {
        progress(true)
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(textToTranslate)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    println("Can't identify language.")
                } else if (languageCode != "en") {
                    println("Language: $languageCode")

                    try {
                        if (englishTranslator == null) {
                            val options = TranslatorOptions.Builder()
                                .setSourceLanguage(TranslateLanguage.fromLanguageTag(languageCode)!!)
                                .setTargetLanguage(TranslateLanguage.ENGLISH)
                                .build()
                            englishTranslator = Translation.getClient(options)

                            val conditions = DownloadConditions.Builder()
                                .requireWifi()
                                .build()

                            englishTranslator!!.downloadModelIfNeeded(conditions)
                                .addOnSuccessListener { _ ->
                                    // Model downloaded successfully. Okay to start translating.
                                    // (Set a flag, unhide the translation UI, etc.)
                                    englishTranslator!!.translate(textToTranslate)
                                        .addOnSuccessListener { translated ->
                                            // Model downloaded successfully. Okay to start translating.
                                            // (Set a flag, unhide the translation UI, etc.)

                                            translatedText(translated)
                                            progress(false)
                                        }
                                }
                                .addOnFailureListener { exception ->
                                    // Model couldn’t be downloaded or other internal error.
                                    // ...
                                    progress(false)
                                }
                        } else {
                            englishTranslator!!.translate(textToTranslate)
                                .addOnSuccessListener { translated ->
                                    // Model downloaded successfully. Okay to start translating.
                                    // (Set a flag, unhide the translation UI, etc.)

                                    translatedText(translated)
                                    progress(false)
                                }
                                .addOnFailureListener { progress(false) }
                        }
                    } catch (e: Exception) {
                        progress(false)
                    }

                } else {
                    progress(false)
                }
            }
            .addOnFailureListener {
                // Model couldn’t be loaded or other internal error.
                // ...
                progress(false)
            }
    }

    fun clear() {
        englishTranslator?.close()
    }
}

object TranslatorUtils {
    private val modelManager by lazy { RemoteModelManager.getInstance() }

    fun getModels(onSuccess: (List<CustomRemoteModel>) -> Unit) {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                onSuccess(
                    models.mapNotNull {
                        try {
                            CustomRemoteModel(it.modelHash, it.language)
                        } catch (e: Exception) {
                            null
                        }
                    }
                )
            }
            .addOnFailureListener { }
    }

    suspend fun deleteModel(model: CustomRemoteModel) {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await().find { it.modelHash == model.hash }?.let {
            modelManager
                .deleteDownloadedModel(it)
                .addOnSuccessListener {}
                .addOnFailureListener {}
        }
    }
}

data class CustomRemoteModel(val hash: String?, val language: String)