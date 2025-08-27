package com.programmersbox.kmpuiviews.utils

import io.github.vinceglb.filekit.PlatformFile

expect class Zipper {
    suspend fun zipFile(platformFile: PlatformFile)
    suspend fun readZip(platformFile: PlatformFile)
}