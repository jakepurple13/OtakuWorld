package com.programmersbox.sharedcomponents.qrcode

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import kotlinx.serialization.json.Json

class QrCodeViewModel(
    val qrCodeRepository: QrCodeRepository,
) : ViewModel() {
    suspend inline fun <reified T> scanQrCodeFromImage(bitmap: ImageBitmap): Result<T> {
        return qrCodeRepository
            .getInfoFromQRCode(bitmap)
            .mapCatching { texts ->
                val raw = texts.first()
                Json.decodeFromString<T>(raw)
                    ?: throw IllegalArgumentException("Unrecognized QR code format")
            }
            .onFailure { it.printStackTrace() }
    }
}