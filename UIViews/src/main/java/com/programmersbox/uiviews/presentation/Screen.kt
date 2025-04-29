package com.programmersbox.uiviews.presentation

import android.net.Uri
import androidx.navigation.NavController
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.uiviews.GenericInfo
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun NavController.navigateToDetails(model: KmpItemModel) = navigate(
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
): KmpItemModel? = Uri.decode(source)
    .let { sourceRepository.toSourceByApiServiceName(it)?.apiService }
    ?.let {
        KmpItemModel(
            title = Uri.decode(title),
            description = Uri.decode(description),
            url = Uri.decode(url),
            imageUrl = Uri.decode(imageUrl),
            source = it
        )
    }
