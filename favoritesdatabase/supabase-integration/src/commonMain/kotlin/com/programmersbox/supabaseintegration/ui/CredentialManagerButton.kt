package com.programmersbox.supabaseintegration.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CredentialManagerSignInButton(
    onClick: (context: Any?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
)

@Composable
expect fun RegisterPasskeyButton(
    onClick: (context: Any?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
)

/**
 * Opaque per-call Android Activity context (see [com.programmersbox.supabaseintegration.credentials.CredentialSignIn]);
 * androidx.credentials requires it to save/retrieve a password. Null on platforms where it's unused.
 */
@Composable
expect fun rememberCredentialManagerContext(): Any?
