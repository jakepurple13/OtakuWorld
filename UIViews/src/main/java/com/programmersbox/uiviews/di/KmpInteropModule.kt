package com.programmersbox.uiviews.di

import android.content.Context
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.domain.TranslationHandler
import com.programmersbox.kmpuiviews.domain.TranslationModel
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.sharedutils.TranslateItems
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.R
import kotlinx.coroutines.flow.MutableStateFlow
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
    override val isAvailable: Boolean = false
    override val models = MutableStateFlow<List<TranslationModel>>(emptyList())
    override suspend fun refresh() {}
    override fun download(code: String) {}
    override fun cancelDownload(code: String) {}
    override suspend fun delete(codes: Set<String>) {}
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