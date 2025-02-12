package com.programmersbox.uiviews.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarColors
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDocked: Boolean = false,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = if (isDocked) SearchBarDefaults.dockedShape else SearchBarDefaults.inputFieldShape,
    colors: SearchBarColors = SearchBarDefaults.colors(),
    tonalElevation: Dp = SearchBarDefaults.TonalElevation,
    shadowElevation: Dp = SearchBarDefaults.ShadowElevation,
    windowInsets: WindowInsets = SearchBarDefaults.windowInsets,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isDocked) {
        DockedSearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    expanded = active,
                    onExpandedChange = onActiveChange,
                    enabled = enabled,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    interactionSource = interactionSource,
                    colors = colors.inputFieldColors
                )
            },
            expanded = active,
            onExpandedChange = onActiveChange,
            modifier = modifier.windowInsetsPadding(windowInsets),
            shape = shape,
            colors = colors,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            content = content,
        )
    } else {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    expanded = active,
                    onExpandedChange = onActiveChange,
                    enabled = enabled,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    interactionSource = interactionSource,
                    colors = colors.inputFieldColors
                )
            },
            expanded = active,
            onExpandedChange = onActiveChange,
            modifier = modifier,
            shape = shape,
            colors = colors,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            windowInsets = windowInsets,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchBarState: SearchBarState = rememberSearchBarState(),
    scrollBehavior: SearchBarScrollBehavior? = null,
    enabled: Boolean = true,
    isDocked: Boolean = false,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = if (isDocked) SearchBarDefaults.dockedShape else SearchBarDefaults.inputFieldShape,
    colors: SearchBarColors = SearchBarDefaults.colors(),
    tonalElevation: Dp = SearchBarDefaults.TonalElevation,
    shadowElevation: Dp = SearchBarDefaults.ShadowElevation,
    windowInsets: WindowInsets = SearchBarDefaults.windowInsets,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val inputField = @Composable {
        SearchBarDefaults.InputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = onSearch,
            enabled = enabled,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            interactionSource = interactionSource,
            colors = colors.inputFieldColors
        )
    }

    TopSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = colors,
        shape = shape,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        windowInsets = windowInsets,
        scrollBehavior = scrollBehavior,
        modifier = modifier,
    )

    if (isDocked) {
        ExpandedDockedSearchBar(
            inputField = inputField,
            state = searchBarState,
            content = content,
            colors = colors,
            shape = shape,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            modifier = modifier,
        )
    } else {
        ExpandedFullScreenSearchBar(
            inputField = inputField,
            state = searchBarState,
            content = content,
            colors = colors,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            modifier = modifier,
        )
    }
}