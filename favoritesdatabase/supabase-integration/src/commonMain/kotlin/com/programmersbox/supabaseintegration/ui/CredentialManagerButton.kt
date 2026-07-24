package com.programmersbox.supabaseintegration.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CredentialManagerSignInButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
)

@Composable
expect fun RegisterPasskeyButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
)
