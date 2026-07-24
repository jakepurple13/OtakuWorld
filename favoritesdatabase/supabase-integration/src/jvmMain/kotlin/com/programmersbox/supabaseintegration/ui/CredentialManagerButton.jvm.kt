package com.programmersbox.supabaseintegration.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CredentialManagerSignInButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
    // Not supported on JVM/Desktop — intentionally renders nothing.
}

@Composable
actual fun RegisterPasskeyButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
    // Not supported on JVM/Desktop — intentionally renders nothing.
}
