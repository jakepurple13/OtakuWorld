package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpmodels.KmpItemModel

interface OptionsSheetScope {
    fun dismiss()

    @Composable
    fun OptionsItem(
        title: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Card(
                onClick = onClick,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                modifier = modifier
            ) {
                ListItem(
                    headlineContent = { Text(title) },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }

            HorizontalDivider()
        }
    }
}

interface OptionsSheetValues {
    val imageUrl: String
    val title: String
    val description: String
    val serviceName: String
    val url: String
}

class KmpItemModelOptionsSheet(
    val itemModel: KmpItemModel,
    override val imageUrl: String = itemModel.imageUrl,
    override val title: String = itemModel.title,
    override val description: String = itemModel.description,
    override val serviceName: String = itemModel.source.serviceName,
    override val url: String = itemModel.url,
) : OptionsSheetValues