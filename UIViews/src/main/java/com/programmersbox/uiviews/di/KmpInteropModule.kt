package com.programmersbox.uiviews.di

import android.content.Context
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.domain.KmpCustomRemoteModel
import com.programmersbox.kmpuiviews.domain.TranslationHandler
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.sharedutils.CustomRemoteModel
import com.programmersbox.sharedutils.TranslateItems
import com.programmersbox.sharedutils.TranslatorUtils
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.R
import org.koin.dsl.module

val kmpInterop = module {
    single {
        AppConfig(
            appName = get<Context>().getString(R.string.app_name),
            isDebug = BuildConfig.DEBUG,
            buildType = when (BuildConfig.FLAVOR) {
                "noFirebase" -> BuildType.NoFirebase
                else -> BuildType.Full
            }
        )
    }

    factory<TranslationHandler> { TranslationItemHandler() }
    factory<TranslationModelHandler> { TranslationModelHandlerImpl() }
}

class TranslationModelHandlerImpl : TranslationModelHandler {
    override fun getModels(onSuccess: (List<KmpCustomRemoteModel>) -> Unit) = TranslatorUtils.getModels {
        onSuccess(it.map(::toRemoteModel))
    }

    override suspend fun deleteModel(model: KmpCustomRemoteModel) = TranslatorUtils
        .deleteModel(toRemoteModel(model))

    override suspend fun modelList(): List<KmpCustomRemoteModel> = TranslatorUtils
        .modelList()
        .map(::toRemoteModel)

    override suspend fun delete(model: KmpCustomRemoteModel) = TranslatorUtils
        .delete(toRemoteModel(model))

    private fun toRemoteModel(kmpCustomRemoteModel: KmpCustomRemoteModel) = CustomRemoteModel(
        hash = kmpCustomRemoteModel.hash,
        language = kmpCustomRemoteModel.language
    )

    private fun toRemoteModel(customRemoteModel: CustomRemoteModel) = KmpCustomRemoteModel(
        hash = customRemoteModel.hash.orEmpty(),
        language = customRemoteModel.language
    )
}


class TranslationItemHandler(
    private val translateItems: TranslateItems = TranslateItems(),
) : TranslationHandler {
    override fun translateDescription(
        textToTranslate: String,
        progress: (Boolean) -> Unit,
        translatedText: (String) -> Unit,
    ) = translateItems.translateDescription(textToTranslate, progress, translatedText)

    override suspend fun translate(textToTranslate: String): String = translateItems.translate(textToTranslate)
    override fun clear() = translateItems.clear()
}