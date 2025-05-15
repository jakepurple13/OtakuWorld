package com.programmersbox.kmpuiviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.kmpuiviews.di.appModule
import com.programmersbox.kmpuiviews.di.databases
import com.programmersbox.kmpuiviews.di.repositories
import com.programmersbox.kmpuiviews.di.viewModels
import com.programmersbox.kmpuiviews.presentation.UrlOpenerScreen
import com.programmersbox.kmpuiviews.presentation.settings.qrcode.ScanQrCode
import org.koin.compose.KoinApplication
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.awt.Cursor

@Composable
fun ApplicationScope.BaseDesktopUi(
    title: String,
    moduleBlock: KoinApplication.() -> Unit,
) {
    //TODO: add a screen where you paste a url and select a source that then opens the details screen


    //TODO: Also need to create a generic module in kmpuiviews
    KoinApplication(
        application = {
            modules(
                module {
                    includes(
                        appModule,
                        viewModels,
                        repositories,
                        databases,
                    )

                    singleOf(::DataStoreHandling)
                    single {
                        NewSettingsHandling(
                            createProtobuf(
                                serializer = SettingsSerializer()
                            ),
                        )
                    }

                    moduleBlock()
                }
            )
        }
    ) {
        val windowState = rememberWindowState()

        Window(
            onCloseRequest = ::exitApplication,
            title = title,
            state = windowState,
            undecorated = true,
            transparent = true,
        ) {
            MaterialTheme(
                createColorScheme(
                    isSystemInDarkTheme(),
                    isExpressive = true
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        CustomTitleBar(
                            title = title,
                            onMinimizeClick = { windowState.isMinimized = true },
                            onCloseClick = ::exitApplication
                        )
                        HorizontalDivider()
                        //TODO: UI Goes here!
                        UrlOpenerScreen()
                        ScanQrCode()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameWindowScope.CustomTitleBar(
    title: String,
    onMinimizeClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    WindowDraggableArea {
        TopAppBar(
            title = { Text(title) },
            actions = {
                IconButton(
                    onClick = onMinimizeClick,
                    modifier = Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                ) {
                    Icon(
                        Icons.Default.Minimize,
                        contentDescription = "Minimize",
                    )
                }

                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            )
        )
    }
}
