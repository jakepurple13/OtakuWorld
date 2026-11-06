package com.programmersbox.sharedutils

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class TranslateItems {
    private var translator: Translator? = null
    private var translatorKey: Pair<String, String>? = null
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val languageIdentifier by lazy { LanguageIdentification.getClient() }

    /** Returns [text] unchanged when the language is unknown, already [target], or unsupported. */
    suspend fun translate(text: String, target: String, onTranslated: suspend (source: String) -> Unit = {}): String {
        val source = languageIdentifier.identifyLanguage(text).await()
        if (!shouldTranslate(source, target)) return text
        val sourceTag = TranslateLanguage.fromLanguageTag(source) ?: return text
        val targetTag = TranslateLanguage.fromLanguageTag(target) ?: return text

        return mutex.withLock {
            val key = sourceTag to targetTag
            if (translatorKey != key) {
                translator?.close()
                translator = Translation.getClient(
                    TranslatorOptions.Builder()
                        .setSourceLanguage(sourceTag)
                        .setTargetLanguage(targetTag)
                        .build()
                )
                translatorKey = key
            }
            val current = translator!!
            current.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build()).await()
            current.translate(text).await().also { onTranslated(source) }
        }
    }

    /** Closes the cached translator once any in-flight translation releases the lock. */
    fun clear() {
        scope.launch {
            mutex.withLock {
                translator?.close()
                translator = null
                translatorKey = null
            }
        }
    }
}

object TranslatorUtils {
    private val modelManager by lazy { RemoteModelManager.getInstance() }

    val isAvailable: Boolean = true

    /** Language tags of the translation models currently on the device. */
    suspend fun downloadedCodes(): Set<String> = modelManager
        .getDownloadedModels(TranslateRemoteModel::class.java)
        .await()
        .map { it.language }
        .toSet()

    /** Downloads one language model (Wi-Fi only). */
    suspend fun download(code: String) {
        modelManager
            .download(TranslateRemoteModel.Builder(code).build(), DownloadConditions.Builder().requireWifi().build())
            .await()
    }

    suspend fun delete(code: String) {
        modelManager.deleteDownloadedModel(TranslateRemoteModel.Builder(code).build()).await()
    }

    /**
     * On-disk size of one language model, or null so callers use an estimate.
     *
     * Layout verified on a device (ML Kit translate, 2026-11): each downloaded language lives in
     * `<noBackupFilesDir>/com.google.mlkit.translate.models/en_<code>/`, holding both directions
     * (`translate_en<code>/`, `translate_<code>en/`) plus `merged_dict_en_<code>_*.bin` files.
     * In-progress downloads sit under `temp/` and are not counted. English is the pivot and has no
     * directory of its own, so it takes no space.
     */
    fun modelSizeBytes(context: Context, code: String): Long? {
        if (code == "en") return 0L
        val dir = java.io.File(context.noBackupFilesDir, "com.google.mlkit.translate.models/en_$code")
        if (!dir.isDirectory) return null
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
