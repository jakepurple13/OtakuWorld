package com.programmersbox.uiviews.utils.components

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.programmersbox.uiviews.utils.currentColorScheme

class ListBottomSheetItemModel(
    val primaryText: String,
    val overlineText: String? = null,
    val secondaryText: String? = null,
    val icon: ImageVector? = null
)

class ListBottomSheet<T>(
    private val title: String,
    private val list: List<T>,
    private val onClick: (T) -> Unit,
    private val itemContent: (T) -> ListBottomSheetItemModel
) : BottomSheetDialogFragment() {
    @OptIn(
        ExperimentalMaterialApi::class,
        ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class
    )
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext())
        .apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                MaterialTheme(currentColorScheme) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        stickyHeader {
                            SmallTopAppBar(
                                title = { Text(title) },
                                navigationIcon = {
                                    IconButton(onClick = { dismiss() }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                },
                                actions = { if (list.isNotEmpty()) Text("(${list.size})") }
                            )
                        }

                        itemsIndexed(list) { index, it ->
                            val c = itemContent(it)
                            ListItem(
                                modifier = Modifier.clickable {
                                    dismiss()
                                    onClick(it)
                                },
                                leadingContent = c.icon?.let { i -> { Icon(i, null) } },
                                headlineText = { Text(c.primaryText) },
                                supportingText = c.secondaryText?.let { i -> { Text(i) } },
                                overlineText = c.overlineText?.let { i -> { Text(i) } }
                            )
                            if (index < list.size - 1) Divider()
                        }
                    }
                }
            }
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = super.onCreateDialog(savedInstanceState)
        .apply {
            setOnShowListener {
                val sheet = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                val bottomSheet = BottomSheetBehavior.from(sheet)
                bottomSheet.skipCollapsed = true
                bottomSheet.isHideable = false
            }
        }
}