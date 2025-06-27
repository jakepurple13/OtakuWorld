package com.programmersbox.uiviews.utils

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.utils.printLogs
import com.programmersbox.uiviews.GenericInfo
import org.koin.core.definition.BeanDefinition
import org.koin.core.module.dsl.binds
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

fun <T : GenericInfo> BeanDefinition<T>.bindsGenericInfo() {
    binds(
        listOf(
            KmpGenericInfo::class,
            GenericInfo::class,
            PlatformGenericInfo::class
        )
    )
}