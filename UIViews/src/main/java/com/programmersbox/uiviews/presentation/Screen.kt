package com.programmersbox.uiviews.presentation

import android.net.Uri
import androidx.navigation.NavController
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.gsonutils.toJson
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.ApiServiceSerializer
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun NavController.navigateToDetails1(model: ItemModel) = navigate(
    Screen.DetailsScreen.route + "/${Uri.encode(model.toJson(ApiService::class.java to ApiServiceSerializer()))}"
) { launchSingleTop = true }

fun NavController.navigateToDetails(model: ItemModel) = navigate(
    Screen.DetailsScreen.Details(
        title = model.title.ifEmpty { "NA" },
        description = model.description.ifEmpty { "NA" },
        url = model.url.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) },
        imageUrl = model.imageUrl.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) },
        source = model.source.serviceName
    )
) { launchSingleTop = true }

fun Screen.DetailsScreen.Details.toItemModel(
    sourceRepository: SourceRepository,
    genericInfo: GenericInfo,
): ItemModel? = Uri.decode(source)
    .let { sourceRepository.toSourceByApiServiceName(it)?.apiService ?: genericInfo.toSource(it) }
    ?.let {
        ItemModel(
            title = Uri.decode(title),
            description = Uri.decode(description),
            url = Uri.decode(url),
            imageUrl = Uri.decode(imageUrl),
            source = it
        )
    }
