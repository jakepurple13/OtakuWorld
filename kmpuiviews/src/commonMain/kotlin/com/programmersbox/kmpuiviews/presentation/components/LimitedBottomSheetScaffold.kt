package com.programmersbox.kmpuiviews.presentation.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitedBottomSheetScaffold(
    sheetContent: @Composable ColumnScope.(PaddingValues) -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    topBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = {},
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    colors: LimitedBottomSheetScaffoldColors = LimitedBottomSheetScaffoldDefaults.colors(),
    bottomSheet: LimitedBottomSheetScaffoldSheet = LimitedBottomSheetScaffoldDefaults.bottomSheet(),
    animations: LimitedBottomSheetScaffoldAnimations = LimitedBottomSheetScaffoldDefaults.animations(),
    content: @Composable (PaddingValues) -> Unit,
) {
    var layoutHeight by remember { mutableStateOf(0.dp) }

    val transitionData = updateTransitionData(
        colors = colors,
        animations = animations,
        bottomSheet = bottomSheet,
        sheetValue = scaffoldState.bottomSheetState.targetValue
    )

    Scaffold(
        topBar = {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(transitionData.color)
            ) {
                topBar()
            }
        },
        containerColor = colors.containerColor,
        contentColor = colors.contentColor,
        contentWindowInsets = contentWindowInsets,
        snackbarHost = { snackbarHost(scaffoldState.snackbarHostState) },
        modifier = modifier
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            content(padding)
            layoutHeight = this.maxHeight
        }

        BottomSheetScaffold(
            scaffoldState = rememberBottomSheetScaffoldState(scaffoldState.bottomSheetState),
            sheetShape = RoundedCornerShape(
                topStart = transitionData.cornerRadius,
                topEnd = transitionData.cornerRadius,
            ),
            sheetDragHandle = {
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier.graphicsLayer {
                        alpha = transitionData.dragHandleAlpha
                    },
                )
            },
            sheetContainerColor = colors.bottomSheetContainerColor,
            sheetContentColor = colors.bottomSheetContentColor,
            sheetPeekHeight = bottomSheet.sheetPeekHeight,
            sheetShadowElevation = bottomSheet.sheetShadowElevation,
            sheetTonalElevation = bottomSheet.sheetTonalElevation,
            sheetSwipeEnabled = bottomSheet.sheetSwipeEnabled,
            sheetContent = {
                Column(
                    modifier = Modifier.heightIn(
                        max = (layoutHeight - padding.calculateTopPadding())
                            .coerceAtLeast(bottomSheet.sheetPeekHeight)
                    )
                ) {
                    sheetContent(padding)
                }
            },
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
        ) {}
    }
}

object LimitedBottomSheetScaffoldDefaults {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun colors(
        topAppBarContainerColor: Color = MaterialTheme.colorScheme
            .surfaceColorAtElevation(BottomSheetDefaults.Elevation),
        topAppBarSheetExpandedContainerColor: Color = MaterialTheme.colorScheme
            .surfaceColorAtElevation(BottomSheetDefaults.Elevation),
        containerColor: Color = MaterialTheme.colorScheme.background,
        contentColor: Color = contentColorFor(backgroundColor = containerColor),
        sheetContainerColor: Color = BottomSheetDefaults.ContainerColor,
        sheetContentColor: Color = contentColorFor(sheetContainerColor),
    ): LimitedBottomSheetScaffoldColors = remember(
        topAppBarContainerColor,
        topAppBarSheetExpandedContainerColor,
        containerColor,
        contentColor,
        sheetContainerColor,
        sheetContentColor
    ) {
        LimitedBottomSheetScaffoldColors(
            topAppBarContainerColor = topAppBarContainerColor,
            topAppBarSheetExpandedContainerColor = topAppBarSheetExpandedContainerColor,
            containerColor = containerColor,
            contentColor = contentColor,
            bottomSheetContainerColor = sheetContainerColor,
            bottomSheetContentColor = sheetContentColor
        )
    }

    @Composable
    fun bottomSheetCornerRadius(
        expanded: Dp = BottomSheetCornerRadiusExpanded,
        unexpanded: Dp = BottomSheetCornerRadiusUnExpanded,
    ) = remember(expanded, unexpanded) {
        LimitedBottomSheetScaffoldCornerRadius(expanded = expanded, unexpanded = unexpanded)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun bottomSheet(
        sheetPeekHeight: Dp = BottomSheetDefaults.SheetPeekHeight,
        sheetTonalElevation: Dp = BottomSheetDefaults.Elevation,
        sheetShadowElevation: Dp = BottomSheetDefaults.Elevation,
        sheetSwipeEnabled: Boolean = true,
        sheetCornerRadius: LimitedBottomSheetScaffoldCornerRadius = bottomSheetCornerRadius(),
    ) = remember(
        sheetPeekHeight,
        sheetTonalElevation,
        sheetShadowElevation,
        sheetSwipeEnabled,
        sheetCornerRadius
    ) {
        LimitedBottomSheetScaffoldSheet(
            sheetPeekHeight = sheetPeekHeight,
            sheetTonalElevation = sheetTonalElevation,
            sheetShadowElevation = sheetShadowElevation,
            sheetSwipeEnabled = sheetSwipeEnabled,
            sheetCornerRadius = sheetCornerRadius
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun animations(
        colorAnimation: @Composable Transition.Segment<SheetValue>.() -> FiniteAnimationSpec<Color> =
            { tween(500) },
        cornerRadiusAnimation: @Composable Transition.Segment<SheetValue>.() -> FiniteAnimationSpec<Dp> =
            { tween(500) },
        dragHandleAlphaAnimation: @Composable Transition.Segment<SheetValue>.() -> FiniteAnimationSpec<Float> =
            { tween(500) },
    ) = remember(colorAnimation, cornerRadiusAnimation, dragHandleAlphaAnimation) {
        LimitedBottomSheetScaffoldAnimations(
            colorAnimation = colorAnimation,
            cornerRadiusAnimation = cornerRadiusAnimation,
            dragHandleAlphaAnimation = dragHandleAlphaAnimation
        )
    }

}

@Immutable
data class LimitedBottomSheetScaffoldColors(
    val topAppBarContainerColor: Color,
    val topAppBarSheetExpandedContainerColor: Color,
    val containerColor: Color,
    val contentColor: Color,
    val bottomSheetContainerColor: Color,
    val bottomSheetContentColor: Color,
) {
    internal fun containerColor(enabled: Boolean) =
        if (enabled) topAppBarSheetExpandedContainerColor else topAppBarContainerColor
}

@Immutable
data class LimitedBottomSheetScaffoldCornerRadius(
    val expanded: Dp,
    val unexpanded: Dp,
)

@Immutable
data class LimitedBottomSheetScaffoldSheet(
    val sheetPeekHeight: Dp,
    val sheetTonalElevation: Dp,
    val sheetShadowElevation: Dp,
    val sheetSwipeEnabled: Boolean,
    val sheetCornerRadius: LimitedBottomSheetScaffoldCornerRadius,
)

@Immutable
class LimitedBottomSheetScaffoldAnimations @OptIn(ExperimentalMaterial3Api::class) constructor(
    val colorAnimation: @Composable Transition.Segment<SheetValue>.() -> FiniteAnimationSpec<Color>,
    val cornerRadiusAnimation: @Composable Transition.Segment<SheetValue>.() -> FiniteAnimationSpec<Dp>,
    val dragHandleAlphaAnimation: @Composable Transition.Segment<SheetValue>.() -> FiniteAnimationSpec<Float>,
)

private class TransitionData(
    color: State<Color>,
    dragHandleAlpha: State<Float>,
    cornerRadius: State<Dp>,
) {
    val color by color
    val dragHandleAlpha by dragHandleAlpha
    val cornerRadius by cornerRadius
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun updateTransitionData(
    colors: LimitedBottomSheetScaffoldColors,
    animations: LimitedBottomSheetScaffoldAnimations,
    bottomSheet: LimitedBottomSheetScaffoldSheet,
    sheetValue: SheetValue,
): TransitionData {
    val transition = updateTransition(sheetValue, label = "limited_scaffold_state")

    val color = transition.animateColor(
        label = "top_app_bar_background",
        transitionSpec = animations.colorAnimation
    ) { colors.containerColor(it == SheetValue.Expanded) }

    val cornerRadius = transition.animateDp(
        label = "corner_radius",
        transitionSpec = animations.cornerRadiusAnimation
    ) { state ->
        when (state) {
            SheetValue.Expanded -> bottomSheet.sheetCornerRadius.expanded
            else -> bottomSheet.sheetCornerRadius.unexpanded
        }
    }

    val dragHandleAlpha = transition.animateFloat(
        label = "drag_handle_alpha",
        transitionSpec = animations.dragHandleAlphaAnimation
    ) { state ->
        when (state) {
            SheetValue.Expanded -> 0f
            else -> 1f
        }
    }

    return remember(transition) {
        TransitionData(
            color = color,
            dragHandleAlpha = dragHandleAlpha,
            cornerRadius = cornerRadius,
        )
    }
}

private val BottomSheetCornerRadiusExpanded = 0.dp
private val BottomSheetCornerRadiusUnExpanded = 28.0.dp