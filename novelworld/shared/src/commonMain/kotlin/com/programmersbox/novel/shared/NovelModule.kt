package com.programmersbox.novel.shared

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import com.programmersbox.novel.shared.reader.NovelReadView
import com.programmersbox.novel.shared.reader.ReadViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
fun novelSharedModule(): Module = module {
    singleOf(::ChapterHolder)
    viewModelOf(::ReadViewModel)
    navigation<ReadViewModel.NovelReader> {
        NovelReadView(
            viewModel = koinViewModel { parametersOf(it) }
        )
    }
}
