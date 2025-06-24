package com.programmersbox.uiviews.utils

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavKey
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.utils.printLogs
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
inline fun <T> MutableList<T>.addNewList(@BuilderInference builderAction: MutableList<T>.() -> Unit): Boolean =
    addAll(buildList(builderAction))

fun recordFirebaseException(throwable: Throwable) = runCatching {
    Firebase.crashlytics.recordException(throwable)
}

fun logFirebaseMessage(message: String) = runCatching {
    printLogs { message }
    Firebase.crashlytics.log(message)
}.onFailure { printLogs { message } }

@SuppressLint("ComposableNaming")
@Composable
fun trackScreen(screenName: Screen) {
    LaunchedEffect(Unit) {
        runCatching {
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screenName.toString())
            }
        }
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
        }
    }
}

@Composable
fun trackScreen(screenName: NavKey) {
    trackScreen(screenName.toString())
}