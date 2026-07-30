package com.programmersbox.kmpuiviews.utils

import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.sharedcomponents.backup.BackupDataSummary
import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedcomponents.backup.ItemResult
import io.github.vinceglb.filekit.PlatformFile

expect class Zipper {
    suspend fun zipFile(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>? = null,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult>

    suspend fun readZip(
        platformFile: PlatformFile,
        selectedKeys: Set<String>,
        selectedListIds: Set<String>? = null,
        onItemComplete: suspend (ItemResult) -> Unit,
    ): List<ItemResult>

    suspend fun peekZip(
        platformFile: PlatformFile,
        uiInfos: List<BackupUiInfo>,
    ): Map<String, BackupDataSummary>

    suspend fun peekListContents(platformFile: PlatformFile): List<CustomList>
}
