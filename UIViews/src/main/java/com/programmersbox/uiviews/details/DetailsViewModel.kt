package com.programmersbox.uiviews.details

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmpalette.palette.graphics.Palette
import com.programmersbox.extensionloader.SourceRepository
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
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.DefaultToastItems
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.ToastItems
import com.programmersbox.uiviews.utils.blurhash.BlurHashDao
import com.programmersbox.uiviews.utils.blurhash.BlurHashItem
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.recordFirebaseException
import com.vanniktech.blurhash.BlurHash
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DetailsViewModel(
    details: Screen.DetailsScreen.Details? = null,
    handle: SavedStateHandle,
    genericInfo: GenericInfo,
    private val dao: ItemDao,
    private val blurHashDao: BlurHashDao,
) : ViewModel(), KoinComponent, ToastItems by DefaultToastItems() {

    private val sourceRepository: SourceRepository by inject()

    val itemModel: ItemModel? = handle.get<String>("model")
        ?.fromJson<ItemModel>(ApiService::class.java to ApiServiceDeserializer(genericInfo))
        ?: details?.toItemModel(sourceRepository, genericInfo)

    var info: InfoModel? by mutableStateOf(null)

    var palette by mutableStateOf<Palette?>(null)

    private var addRemoveFavoriteJob: Job? = null

    private val itemListener = FirebaseDb.FirebaseListener()
    private val dbModelListener = FirebaseDb.FirebaseListener()
    private val chapterListener = FirebaseDb.FirebaseListener()

    var favoriteListener by mutableStateOf(false)
    var chapters: List<ChapterWatched> by mutableStateOf(emptyList())

    var description: String by mutableStateOf("")

    var dbModel by mutableStateOf<DbModel?>(null)

    var imageBitmap: Bitmap? by mutableStateOf(null)
    var blurHash by mutableStateOf<BitmapPainter?>(null)
    private var blurHashItem: BlurHashItem? = null

    init {
        itemModel?.url?.let { url ->
            Cached.cache[url]?.let { flow { emit(Result.success(it)) } } ?: itemModel.toInfoModel()
        }
            ?.dispatchIo()
            ?.catch {
                recordFirebaseException(it)
                it.printStackTrace()
                showError()
                emit(Result.failure(it))
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

        blurHashDao
            .getHash(itemModel?.imageUrl)
            .onEach { blurHashItem = it }
            .filterNotNull()
            .mapNotNull {
                BlurHash.decode(
                    it.blurHash,
                    width = ComposableUtils.IMAGE_WIDTH_PX,
                    height = ComposableUtils.IMAGE_HEIGHT_PX
                )?.asImageBitmap()
            }
            .onEach { blurHash = BitmapPainter(it) }
            .onEach { if (palette == null) palette = Palette.from(it).generate() }
            .launchIn(viewModelScope)

        combine(
            snapshotFlow { favoriteListener },
            snapshotFlow { imageBitmap },
            blurHashDao.getHash(itemModel?.imageUrl)
        ) { f, i, b ->
            BlurAdd(
                bitmap = i,
                isFavorite = f,
                blurHashItem = b
            )
        }.onEach {
            if (it.blurHashItem == null && it.bitmap != null && it.isFavorite) {
                blurHashDao.insertHash(
                    BlurHashItem(
                        info!!.imageUrl,
                        BlurHash.encode(it.bitmap, 4, 3)
                    )
                )
            }
        }
            .launchIn(viewModelScope)
    }

    class BlurAdd(
        val bitmap: Bitmap?,
        val isFavorite: Boolean,
        val blurHashItem: BlurHashItem?,
    )

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

        imageBitmap?.let {
            viewModelScope.launch {
                blurHashDao.insertHash(
                    BlurHashItem(
                        info!!.imageUrl,
                        BlurHash.encode(it, 4, 3)
                    )
                )
            }
        }
    }

    fun removeItem() {
        val db = info!!.toDbModel(info!!.chapters.size)
        addRemoveFavoriteJob?.cancel()
        addRemoveFavoriteJob = viewModelScope.launch {
            dao.deleteFavorite(db)
            FirebaseDb.removeShowFlow(db).collect()
        }

        viewModelScope.launch {
            blurHashItem?.let { blurHashDao.deleteHash(it) }
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