package com.programmersbox.kmpuiviews.presentation.settings.accountinfo.accountstats

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.programmersbox.favoritesdatabase.BlurHashDao
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.sharedcomponents.stats.StatData
import com.programmersbox.sharedcomponents.stats.StatisticsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

class SystemStatisticsProvider(
    private val exceptionDao: ExceptionDao,
    private val blurHashDao: BlurHashDao,
    private val translationModelHandler: TranslationModelHandler,
    private val appConfig: AppConfig,
) : StatisticsProvider() {
    override val header: String = "⚙️ System"
    override val key: String = "system"
    override val contentType: String = "system"
    override val priority: Int = 100

    override fun observeStats(): Flow<List<StatData>> = combine(
        exceptionDao.getExceptionCount(),
        blurHashDao.getAllHashesCount(),
        flow { emit(translationModelHandler.modelList().size) }
    ) { exceptions, blurHashes, translationModels ->
        listOfNotNull(
            StatData(
                id = "blurHashes",
                label = "Blur Hash Cache",
                description = "Speeds up image loading",
                value = blurHashes.toString()
            ),
            if (appConfig.buildType != BuildType.NoFirebase)
                StatData(
                    id = "translationModels",
                    label = "Translation Models",
                    description = "Downloaded language models",
                    value = translationModels.toString()
                )
            else null,
            StatData(
                id = "exceptions",
                label = "Logged Exceptions",
                description = "Errors captured by the app",
                value = exceptions.toString(),
                valueColor = {
                    if (exceptions > 0)
                        MaterialTheme.colorScheme.error
                    else
                        Color.Unspecified
                }
            )
        )
    }.onStart {
        emit(
            listOfNotNull(
                StatData(
                    id = "blurHashes",
                    label = "Blur Hash Cache",
                    description = "Speeds up image loading",
                    value = "0"
                ),
                if (appConfig.buildType != BuildType.NoFirebase)
                    StatData(
                        id = "translationModels",
                        label = "Translation Models",
                        description = "Downloaded language models",
                        value = "0"
                    )
                else null,
                StatData(
                    id = "exceptions",
                    label = "Logged Exceptions",
                    description = "Errors captured by the app",
                    value = "0"
                )
            )
        )
    }
}