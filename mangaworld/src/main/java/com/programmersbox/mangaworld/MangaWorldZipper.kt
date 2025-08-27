package com.programmersbox.mangaworld

import android.content.Context
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.mangasettings.MangaSettings
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.HistoryDao
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.utils.Zipper
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import kotlinx.coroutines.flow.firstOrNull

class MangaWorldZipper(
    context: Context,
    favoritesRepository: FavoritesRepository,
    listDao: ListDao,
    itemDao: ItemDao,
    heatMapDao: HeatMapDao,
    historyDao: HistoryDao,
    newSettingsHandling: NewSettingsHandling,
    private val mangaNewSettingsHandling: MangaNewSettingsHandling, exceptionDao: ExceptionDao,
) : Zipper(context, favoritesRepository, listDao, itemDao, heatMapDao, historyDao, newSettingsHandling, exceptionDao) {
    override fun additionalHandlers(): Map<String, ZipHandler> = mapOf(
        "manga_settings" to ZipHandler(
            input = { inputStream ->
                mangaNewSettingsHandling
                    .preferences
                    .updateData { MangaSettings.ADAPTER.decode(inputStream) }
            },
            output = { outputStream ->
                mangaNewSettingsHandling
                    .preferences
                    .data
                    .firstOrNull()
                    ?.encode(outputStream)
            }
        )
    )
}