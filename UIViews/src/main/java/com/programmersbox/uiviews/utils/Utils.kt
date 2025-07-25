package com.programmersbox.uiviews.utils

import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.uiviews.GenericInfo
import org.koin.core.definition.BeanDefinition
import org.koin.core.module.dsl.binds
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
inline fun <T> MutableList<T>.addNewList(@BuilderInference builderAction: MutableList<T>.() -> Unit): Boolean =
    addAll(buildList(builderAction))

fun <T : GenericInfo> BeanDefinition<T>.bindsGenericInfo() {
    binds(
        listOf(
            KmpGenericInfo::class,
            GenericInfo::class,
            PlatformGenericInfo::class
        )
    )
}