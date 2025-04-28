package com.programmersbox.uiviews.utils

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.Screen
import kotlinx.coroutines.flow.combine
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
inline fun <T> MutableList<T>.addNewList(@BuilderInference builderAction: MutableList<T>.() -> Unit): Boolean =
    addAll(buildList(builderAction))

fun recordFirebaseException(throwable: Throwable) = runCatching {
    Firebase.crashlytics.recordException(throwable)
}

fun logFirebaseMessage(message: String) = runCatching {
    println(message)
    Firebase.crashlytics.log(message)
}.onFailure { println(message) }

//TODO: This could probably go...somewhere
fun combineSources(
    sourceRepository: SourceRepository,
    dao: ItemDao,
) = combine(
    sourceRepository.sources,
    dao.getSourceOrder()
) { list, order ->
    list
        .filterNot { it.apiService.notWorking }
        .sortedBy { order.find { o -> o.source == it.packageName }?.order ?: 0 }
}

@SuppressLint("ComposableNaming")
@Composable
fun trackScreen(screenName: Screen) {
    LaunchedEffect(Unit) {
        runCatching {
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screenName.toString())
            }
        }.onFailure { it.printStackTrace() }
    }
}

@SuppressLint("ComposableNaming")
@Composable
fun trackScreen(screenName: String) {
    LaunchedEffect(Unit) {
        runCatching {
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            }
        }.onFailure { it.printStackTrace() }
    }
}