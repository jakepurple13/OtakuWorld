package com.programmersbox.uiviews

import androidx.compose.runtime.Composable
import com.programmersbox.kmpuiviews.PlatformGenericInfo

interface GenericInfo : PlatformGenericInfo {
    @Composable
    override fun AccountContent() {

    }

    @Composable
    override fun AccountSettings() {

    }

    @Composable
    override fun ProfileIcon(): String = ""
}