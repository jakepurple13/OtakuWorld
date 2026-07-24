package com.programmersbox.supabaseintegration.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CredentialManagerSignInButton(onClick: (context: Any?) -> Unit, enabled: Boolean, modifier: Modifier) {
    // Not supported on iOS — intentionally renders nothing.
}

@Composable
actual fun RegisterPasskeyButton(onClick: (context: Any?) -> Unit, enabled: Boolean, modifier: Modifier) {
    // Not supported on iOS — intentionally renders nothing.
}

@Composable
actual fun rememberCredentialManagerContext(): Any? = null
