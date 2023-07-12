package com.programmersbox.uiviews.utils.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import com.programmersbox.models.ApiService
import com.programmersbox.uiviews.GenericInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

private const val METADATA_NAME = "programmersbox.otaku.name"
private const val METADATA_CLASS = "programmersbox.otaku.class"
private const val EXTENSION_FEATURE = "programmersbox.otaku.extension"

class SourceLoader(
    private val context: Context,
    genericInfo: GenericInfo,
    private val sourceRepository: SourceRepository
) {
    private val extensionLoader = ExtensionLoader<ApiService, SourceInformation>(
        context,
        EXTENSION_FEATURE + "." + genericInfo.sourceType,
        METADATA_CLASS,
    ) { t, a, p ->
        SourceInformation(
            apiService = t,
            name = a.metaData.getString(METADATA_NAME) ?: "Nothing",
            icon = context.packageManager.getApplicationIcon(p.packageName),
            packageName = p.packageName
        )
    }

    suspend fun load() {
        sourceRepository.setSources(extensionLoader.loadExtensions())
    }
}

data class SourceInformation(
    val apiService: ApiService,
    val name: String,
    val icon: Drawable?,
    val packageName: String
)

class SourceRepository {
    private val sourcesList = MutableStateFlow<List<SourceInformation>>(emptyList())
    val sources = sourcesList.asSharedFlow()
    val list get() = sourcesList.value

    fun setSources(sourceList: List<SourceInformation>) {
        sourcesList.value = sourceList
    }
}