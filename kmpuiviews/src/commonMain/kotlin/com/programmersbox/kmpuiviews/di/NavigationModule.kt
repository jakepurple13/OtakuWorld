package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.presentation.navactions.Navigation3Actions
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.sharedcomponents.Navigator
import org.koin.dsl.binds
import org.koin.dsl.module

val navigationModule = module {
    single { Navigation3Actions() } binds arrayOf(NavigationActions::class, Navigator::class)
}