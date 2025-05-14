package com.programmersbox.kmpuiviews.presentation.settings.qrcode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class QrCodeScannerViewModel : ViewModel() {
    var qrCodeInfo by mutableStateOf<QrCodeInfo?>(null)
}