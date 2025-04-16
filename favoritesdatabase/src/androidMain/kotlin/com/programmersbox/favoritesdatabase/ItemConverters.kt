package com.programmersbox.favoritesdatabase

import com.programmersbox.models.ApiService
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel

fun ItemModel.toDbModel() = DbModel(
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    source = source.serviceName
)

fun DbModel.toItemModel(source: ApiService) = ItemModel(
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    source = source
)

fun DbModel.toInfoModel(source: ApiService) = InfoModel(
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    chapters = emptyList(),
    genres = emptyList(),
    alternativeNames = emptyList(),
    source = source
)

fun InfoModel.toDbModel(numChapters: Int = 0) = DbModel(
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    source = source.serviceName,
    numChapters = numChapters
)

fun CustomListInfo.toItemModel(source: ApiService) = ItemModel(
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    source = source
)