package com.programmersbox.favoritesdatabase

import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel

fun KmpItemModel.toDbModel() = DbModel(
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    source = source.serviceName
)

fun DbModel.toItemModel(source: KmpApiService) = KmpItemModel(
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    source = source
)

fun KmpInfoModel.toDbModel(numChapters: Int = 0) = DbModel(
    title = title,
    description = description,
    url = url,
    imageUrl = imageUrl,
    source = source.serviceName,
    numChapters = numChapters
)