package com.programmersbox.sharedutils

class TranslateItems {
    fun translateDescription(textToTranslate: String, progress: (Boolean) -> Unit, translatedText: (String) -> Unit) =
        translatedText(textToTranslate)

    suspend fun translate(textToTranslate: String): String = textToTranslate
    fun clear() = Unit
}

object TranslatorUtils {
    fun getModels(onSuccess: (List<CustomRemoteModel>) -> Unit) = Unit
    suspend fun deleteModel(model: CustomRemoteModel) = Unit

    suspend fun modelList() = emptyList<CustomRemoteModel>()

    suspend fun delete(model: CustomRemoteModel) = Unit
}

data class CustomRemoteModel(val hash: String, val language: String)