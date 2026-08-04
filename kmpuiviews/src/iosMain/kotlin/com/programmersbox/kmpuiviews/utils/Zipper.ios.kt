package com.programmersbox.kmpuiviews.utils

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import io.github.vinceglb.filekit.PlatformFile

actual class Zipper {
    actual suspend fun zipFile(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>?,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> {
        TODO("Not yet implemented")
    }

    actual suspend fun readZip(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>?,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult> {
        TODO("Not yet implemented")
    }

    actual suspend fun peekZip(
        platformFile: PlatformFile,
        uiInfos: List<BackupUiInfo>,
    ): Map<String, BackupDataSummary> {
        TODO("Not yet implemented")
    }

    actual suspend fun peekListContents(platformFile: PlatformFile): List<CustomList> {
        TODO("Not yet implemented")
    }
}