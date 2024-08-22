package com.programmersbox.uiviews.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.dokar.sonner.ToastType
import com.dokar.sonner.Toaster
import com.dokar.sonner.ToasterDefaults
import com.dokar.sonner.ToasterState
import com.dokar.sonner.listen
import com.dokar.sonner.rememberToasterState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

object ToasterUtils {
    const val LOADING_TOAST_ID = 1234
}

@Composable
fun ToasterSetup(
    toaster: ToasterState,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.BottomEnd,
) {
    Toaster(
        state = toaster,
        richColors = true,
        iconSlot = { toast ->
            if (toast.icon == ToasterUtils.LOADING_TOAST_ID) {
                Box(modifier = Modifier.padding(end = 16.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 1.5.dp,
                    )
                }
            } else {
                // Fallback to the default icon slot
                ToasterDefaults.iconSlot(toast)
            }
        },
        darkTheme = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES ||
                (isSystemInDarkTheme() && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
        alignment = alignment,
        modifier = modifier
    )
}

fun ToasterState.showErrorToast() {
    show(
        message = "Something went wrong",
        type = ToastType.Error,
        duration = ToasterDefaults.DurationShort
    )
}

val ErrorToast = Toast(
    id = ToasterUtils.LOADING_TOAST_ID,
    message = "Something went wrong",
    type = ToastType.Error,
    duration = ToasterDefaults.DurationShort
)

interface ToastItems {
    val toastState: MutableSharedFlow<Toast?>
    suspend fun removeError()
    suspend fun showError()
}

class DefaultToastItems : ToastItems {
    override val toastState: MutableSharedFlow<Toast?> = MutableSharedFlow()

    override suspend fun removeError() {
        toastState.emit(null)
    }

    override suspend fun showError() {
        toastState.emit(ErrorToast)
    }
}

@Composable
fun ToasterItemsSetup(
    toastItems: ToastItems,
    scope: CoroutineScope = rememberCoroutineScope(),
    toaster: ToasterState = rememberToasterState(
        onToastDismissed = { scope.launch { toastItems.removeError() } }
    ),
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.BottomEnd,
) {
    LaunchedEffect(toaster, toastItems) {
        toaster.listen(toastItems.toastState)
    }

    ToasterSetup(
        toaster = toaster,
        modifier = modifier,
        alignment = alignment
    )
}