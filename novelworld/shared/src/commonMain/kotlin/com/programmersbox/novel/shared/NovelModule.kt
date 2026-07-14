package com.programmersbox.novel.shared

import com.programmersbox.novel.shared.reader.ReadViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun novelSharedModule(): Module = module {
    singleOf(::ChapterHolder)
    viewModelOf(::ReadViewModel)
}
