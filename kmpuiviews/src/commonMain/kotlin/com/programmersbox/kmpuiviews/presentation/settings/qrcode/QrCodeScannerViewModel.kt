package com.programmersbox.kmpuiviews.presentation.settings.qrcode

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpuiviews.repository.QrCodeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class QrCodeScannerViewModel(
    private val qrCodeRepository: QrCodeRepository,
) : ViewModel() {
    var qrCodeInfo by mutableStateOf<QrCodeInfo?>(null)

    fun scanQrCodeFromImage(bitmap: ImageBitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            qrCodeRepository.getInfoFromQRCode(bitmap)
                .mapCatching { Json.decodeFromString<QrCodeInfo>(it.first()) }
                .onSuccess { qrCodeInfo = it }
        }
    }
}