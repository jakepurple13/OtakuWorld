package com.programmersbox.uiviews.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.from
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.rememberDynamicColorScheme
import com.programmersbox.datastore.ThemeColor

val com.programmersbox.uiviews.ThemeColor.seedColor
    get() = when (this) {
        com.programmersbox.uiviews.ThemeColor.Dynamic -> Color.Transparent
        com.programmersbox.uiviews.ThemeColor.Blue -> Color.Blue
        com.programmersbox.uiviews.ThemeColor.Red -> Color.Red
        com.programmersbox.uiviews.ThemeColor.Green -> Color.Green
        com.programmersbox.uiviews.ThemeColor.Yellow -> Color.Yellow
        com.programmersbox.uiviews.ThemeColor.Cyan -> Color.Cyan
        com.programmersbox.uiviews.ThemeColor.Magenta -> Color.Magenta
        com.programmersbox.uiviews.ThemeColor.Custom -> Color.Transparent
        else -> Color.Transparent
    }

val ThemeColor.seedColor
    get() = when (this) {
        ThemeColor.Dynamic -> Color.Transparent
        ThemeColor.Blue -> Color.Blue
        ThemeColor.Red -> Color.Red
        ThemeColor.Green -> Color.Green
        ThemeColor.Yellow -> Color.Yellow
        ThemeColor.Cyan -> Color.Cyan
        ThemeColor.Magenta -> Color.Magenta
        ThemeColor.Custom -> Color.Transparent
        else -> Color.Transparent
    }

@Composable
fun ThemeItem(
    themeColor: ThemeColor,
    colorScheme: ColorScheme = rememberDynamicColorScheme(themeColor.seedColor, isDark = isSystemInDarkTheme(), isAmoled = false),
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            SelectableMiniPalette(
                selected = false,
                colorScheme = colorScheme
            )

            Text(themeColor.name)
        }
    }
}

@Composable
fun ThemeItem(
    onClick: () -> Unit,
    selected: Boolean,
    themeColor: ThemeColor,
    colorScheme: ColorScheme = rememberDynamicColorScheme(themeColor.seedColor, isDark = isSystemInDarkTheme(), isAmoled = false),
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            SelectableMiniPalette(
                selected = selected,
                colorScheme = colorScheme
            )

            Text(themeColor.name)
        }
    }
}

@Composable
fun SelectableMiniPalette(
    modifier: Modifier = Modifier,
    colorScheme: ColorScheme,
) {
    SelectableMiniPalette(
        modifier = modifier,
        selected = false,
        onClick = null,
        accents = remember(colorScheme) {
            listOf(
                TonalPalette.from(colorScheme.primary),
                TonalPalette.from(colorScheme.secondary),
                TonalPalette.from(colorScheme.tertiary)
            )
        }
    )
}

@Composable
fun SelectableMiniPalette(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
    colorScheme: ColorScheme,
) {
    SelectableMiniPalette(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        accents = remember(colorScheme) {
            listOf(
                TonalPalette.from(colorScheme.primary),
                TonalPalette.from(colorScheme.secondary),
                TonalPalette.from(colorScheme.tertiary)
            )
        }
    )
}

@Composable
fun SelectableMiniPaletteWithSurface(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
    accents: List<TonalPalette>,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        SelectableMiniPalette(
            modifier = modifier,
            selected = selected,
            onClick = onClick,
            accents = accents
        )
    }
}

@Composable
fun SelectableMiniPalette(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: (() -> Unit)? = null,
    accents: List<TonalPalette>,
) {
    val content: @Composable () -> Unit = {
        Box {
            Surface(
                modifier = Modifier
                    .size(50.dp)
                    .offset((-25).dp, 25.dp),
                color = Color(accents[1].tone(85)),
            ) {}
            Surface(
                modifier = Modifier
                    .size(50.dp)
                    .offset(25.dp, 25.dp),
                color = Color(accents[2].tone(75)),
            ) {}
            val animationSpec = spring<Float>(stiffness = Spring.StiffnessMedium)
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(animationSpec) + fadeIn(animationSpec),
                exit = scaleOut(animationSpec) + fadeOut(animationSpec),
            ) {
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Checked",
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
    }
    onClick?.let {
        Surface(
            onClick = onClick,
            modifier = modifier
                .padding(12.dp)
                .size(50.dp),
            shape = CircleShape,
            color = Color(accents[0].tone(60)),
        ) { content() }
    } ?: Surface(
        modifier = modifier
            .padding(12.dp)
            .size(50.dp),
        shape = CircleShape,
        color = Color(accents[0].tone(60)),
    ) { content() }
}