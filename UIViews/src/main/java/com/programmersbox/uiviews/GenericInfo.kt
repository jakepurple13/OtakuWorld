package com.programmersbox.uiviews

import androidx.compose.runtime.Composable
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import org.koin.androidx.compose.koinViewModel

interface GenericInfo : PlatformGenericInfo {
    @Composable
    override fun AccountContent() = com.programmersbox.uiviews.presentation.onboarding.AccountContent()

    @Composable
    override fun AccountSettings() = com.programmersbox.uiviews.presentation.settings.AccountSettings()

    @Composable
    override fun ProfileIcon(): String = koinViewModel<AccountViewModel>().accountInfo?.photoUrl?.toString().orEmpty()
}