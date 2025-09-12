package com.programmersbox.otakuworld

import kotlinx.serialization.Serializable

@Serializable
data class CustomList(
    val item: CustomListItem,
    val list: List<CustomListInfo>,
)

@Serializable
data class CustomListItem(
    val uuid: String,
    val name: String,
    val time: Long,
    val useBiometric: Boolean,
)

@Serializable
data class CustomListInfo(
    val uniqueId: String,
    val uuid: String,
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val source: String,
)

@Serializable
data class DbModel(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String,
    val source: String,
    val numChapters: Int,
    val shouldCheckForUpdate: Boolean,
)