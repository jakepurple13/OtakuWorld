package com.programmersbox.uiviews.utils.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.programmersbox.uiviews.utils.InsetSmallTopAppBar
import com.programmersbox.uiviews.utils.LocalNavController

class ListBottomSheetItemModel(
    val primaryText: String,
    val overlineText: String? = null,
    val secondaryText: String? = null,
    val icon: ImageVector? = null
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun <T> ListBottomScreen(
    title: String,
    list: List<T>,
    onClick: (T) -> Unit,
    includeInsetPadding: Boolean = true,
    itemContent: (T) -> ListBottomSheetItemModel
) {
    val navController = LocalNavController.current
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.navigationBarsPadding()
    ) {
        stickyHeader {
            InsetSmallTopAppBar(
                insetPadding = if (includeInsetPadding) WindowInsets.statusBars else WindowInsets(0.dp),
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.Close, null) } },
                actions = { if (list.isNotEmpty()) Text("(${list.size})") }
            )
            Divider()
        }
        itemsIndexed(list) { index, it ->
            val c = itemContent(it)
            ListItem(
                modifier = Modifier.clickable { onClick(it) },
                leadingContent = c.icon?.let { i -> { Icon(i, null) } },
                headlineContent = { Text(c.primaryText) },
                supportingContent = c.secondaryText?.let { i -> { Text(i) } },
                overlineContent = c.overlineText?.let { i -> { Text(i) } }
            )
            if (index < list.size - 1) Divider()
        }
    }
}