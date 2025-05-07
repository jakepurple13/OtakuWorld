package com.programmersbox.kmpuiviews.domain

interface TranslationModelHandler {
    fun getModels(onSuccess: (List<KmpCustomRemoteModel>) -> Unit)
    suspend fun deleteModel(model: KmpCustomRemoteModel)

    suspend fun modelList(): List<KmpCustomRemoteModel>

    suspend fun delete(model: KmpCustomRemoteModel)
}

data class KmpCustomRemoteModel(val hash: String, val language: String)