package com.programmersbox.uiviews.details

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.TranslateItems
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.ApiServiceDeserializer
import com.programmersbox.uiviews.utils.Cached
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.showErrorToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DetailsViewModel(
    handle: SavedStateHandle,
    genericInfo: GenericInfo,
    context: Context,
    private val dao: ItemDao,
    val itemModel: ItemModel? = handle.get<String>("model")
        ?.fromJson<ItemModel>(ApiService::class.java to ApiServiceDeserializer(genericInfo))
) : ViewModel() {

    var info: InfoModel? by mutableStateOf(null)

    private var addRemoveFavoriteJob: Job? = null

    private val itemListener = FirebaseDb.FirebaseListener()
    private val dbModelListener = FirebaseDb.FirebaseListener()
    private val chapterListener = FirebaseDb.FirebaseListener()

    var favoriteListener by mutableStateOf(false)
    var chapters: List<ChapterWatched> by mutableStateOf(emptyList())

    var description: String by mutableStateOf("")

    var dbModel by mutableStateOf<DbModel?>(null)

    init {
        itemModel?.url?.let { url ->
            Cached.cache[url]?.let { flow { emit(Result.success(it)) } } ?: itemModel.toInfoModel()
        }
            ?.dispatchIo()
            ?.catch {
                it.printStackTrace()
                context.showErrorToast()
            }
            ?.filter { it.isSuccess }
            ?.map { it.getOrThrow() }
            ?.onEach { item ->
                info = item
                description = item.description.ifEmpty { "No Description Found" }
                setup(item)
                Cached.cache[item.url] = item
            }
            ?.launchIn(viewModelScope)
    }

    suspend fun toggleNotify() {
        dbModel
            ?.let { it.copy(shouldCheckForUpdate = !it.shouldCheckForUpdate) }
            ?.let {
                dao.updateFavoriteItem(it)
                FirebaseDb.toggleUpdateCheckShowFlow(it).collect()
            }
    }

    private val englishTranslator = TranslateItems()

    fun translateDescription(progress: MutableState<Boolean>) {
        englishTranslator.translateDescription(
            info!!.description,
            { progress.value = it },
            { description = it }
        )
    }

    private fun setup(info: InfoModel) {
        combine(
            itemListener.findItemByUrlFlow(info.url),
            dao.containsItem(info.url)
        ) { f, d -> f || d }
            .dispatchIo()
            .onEach { favoriteListener = it }
            .launchIn(viewModelScope)

        combine(
            chapterListener.getAllEpisodesByShowFlow(info.url),
            dao.getAllChapters(info.url)
        ) { f, d -> (f + d).distinctBy { it.url } }
            .onEach { chapters = it }
            .launchIn(viewModelScope)

        combine(
            dao.getDbModel(info.url),
            dbModelListener.getShowFlow(info.url)
        ) { d, f -> f ?: d }
            .onEach { dbModel = it }
            .launchIn(viewModelScope)
    }

    fun markAs(c: ChapterModel, b: Boolean) {
        ChapterWatched(url = c.url, name = c.name, favoriteUrl = info!!.url).let {
            viewModelScope.launch {
                if (b) dao.insertChapter(it) else dao.deleteChapter(it)
                (if (b) FirebaseDb.insertEpisodeWatchedFlow(it) else FirebaseDb.removeEpisodeWatchedFlow(it)).collect()
            }
        }
    }

    fun addItem() {
        val db = info!!.toDbModel(info!!.chapters.size)
        addRemoveFavoriteJob?.cancel()
        addRemoveFavoriteJob = viewModelScope.launch {
            dao.insertFavorite(db)
            FirebaseDb.insertShowFlow(db).collect()
        }
    }

    fun removeItem() {
        val db = info!!.toDbModel(info!!.chapters.size)
        addRemoveFavoriteJob?.cancel()
        addRemoveFavoriteJob = viewModelScope.launch {
            dao.deleteFavorite(db)
            FirebaseDb.removeShowFlow(db).collect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        itemListener.unregister()
        chapterListener.unregister()
        dbModelListener.unregister()
        englishTranslator.clear()
    }
}