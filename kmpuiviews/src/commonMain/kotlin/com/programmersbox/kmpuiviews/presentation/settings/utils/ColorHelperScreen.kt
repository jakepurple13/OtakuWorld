package com.programmersbox.kmpuiviews.presentation.settings.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorHelperScreen() {
    HideNavBarWhileOnScreen()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Color Helper") },
                navigationIcon = { BackButton() }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    textColor = MaterialTheme.colorScheme.onPrimary,
                    text = "Primary/OnPrimary"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    text = "Primary Container/OnPrimary Container"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.inversePrimary,
                    textColor = MaterialTheme.colorScheme.onPrimary, // Assuming onPrimary for inversePrimary's text
                    text = "Inverse Primary"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.secondary,
                    textColor = MaterialTheme.colorScheme.onSecondary,
                    text = "Secondary/OnSecondary"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    text = "Secondary Container/OnSecondary Container"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.tertiary,
                    textColor = MaterialTheme.colorScheme.onTertiary,
                    text = "Tertiary/OnTertiary"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    textColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    text = "Tertiary Container/OnTertiary Container"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    textColor = MaterialTheme.colorScheme.onBackground,
                    text = "Background/OnBackground"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    text = "Surface/OnSurface"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = "Surface Variant/OnSurface Variant"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.surfaceTint,
                    textColor = MaterialTheme.colorScheme.onSurface, // Assuming onSurface for surfaceTint's text
                    text = "Surface Tint"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.inverseSurface,
                    textColor = MaterialTheme.colorScheme.inverseOnSurface,
                    text = "Inverse Surface/Inverse OnSurface"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.error,
                    textColor = MaterialTheme.colorScheme.onError,
                    text = "Error/OnError"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.errorContainer,
                    textColor = MaterialTheme.colorScheme.onErrorContainer,
                    text = "Error Container/OnError Container"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.outline,
                    textColor = MaterialTheme.colorScheme.onSurface, // Assuming onSurface for outline's text
                    text = "Outline"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.outlineVariant,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant, // Assuming onSurfaceVariant for outlineVariant's text
                    text = "Outline Variant"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.scrim,
                    textColor = MaterialTheme.colorScheme.onSurface, // Assuming onSurface for scrim's text
                    text = "Scrim"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.surfaceBright,
                    textColor = MaterialTheme.colorScheme.onSurface, // Assuming onSurface for surfaceBright's text
                    text = "Surface Bright"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.surfaceDim,
                    textColor = MaterialTheme.colorScheme.onSurface, // Assuming onSurface for surfaceDim's text
                    text = "Surface Dim"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                    textColor = MaterialTheme.colorScheme.onSurface, // Assuming onSurface for surfaceContainer's text
                    text = "Surface Container"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    textColor = MaterialTheme.colorScheme.onSurface, // Assuming onSurface for surfaceContainerHigh's text
                    text = "Surface Container High"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    textColor = MaterialTheme.colorScheme.onSurface, // Assuming onSurface for surfaceContainerHighest's text
                    text = "Surface Container Highest"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    textColor = MaterialTheme.colorScheme.onSurface, // Assuming onSurface for surfaceContainerLow's text
                    text = "Surface Container Low"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    textColor = MaterialTheme.colorScheme.onSurface, // Assuming onSurface for surfaceContainerLowest's text
                    text = "Surface Container Lowest"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.primaryFixed,
                    textColor = MaterialTheme.colorScheme.onPrimaryFixed,
                    text = "Primary Fixed/OnPrimary Fixed"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.primaryFixedDim,
                    textColor = MaterialTheme.colorScheme.onPrimaryFixedVariant, // Assuming onPrimaryFixedVariant for primaryFixedDim's text
                    text = "Primary Fixed Dim/OnPrimary Fixed Variant"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.secondaryFixed,
                    textColor = MaterialTheme.colorScheme.onSecondaryFixed,
                    text = "Secondary Fixed/OnSecondary Fixed"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.secondaryFixedDim,
                    textColor = MaterialTheme.colorScheme.onSecondaryFixedVariant, // Assuming onSecondaryFixedVariant for secondaryFixedDim's text
                    text = "Secondary Fixed Dim/OnSecondary Fixed Variant"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.tertiaryFixed,
                    textColor = MaterialTheme.colorScheme.onTertiaryFixed,
                    text = "Tertiary Fixed/OnTertiary Fixed"
                )
            }
            item {
                ColorItem(
                    backgroundColor = MaterialTheme.colorScheme.tertiaryFixedDim,
                    textColor = MaterialTheme.colorScheme.onTertiaryFixedVariant, // Assuming onTertiaryFixedVariant for tertiaryFixedDim's text
                    text = "Tertiary Fixed Dim/OnTertiary Fixed Variant"
                )
            }
        }
    }
}

@Composable
private fun ColorItem(
    backgroundColor: Color,
    textColor: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = backgroundColor,
        contentColor = textColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text,
            modifier = Modifier.padding(16.dp)
        )
    }
}