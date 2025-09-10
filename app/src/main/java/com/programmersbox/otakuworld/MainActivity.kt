package com.programmersbox.otakuworld

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.programmersbox.otakuworld.info.InfoScreen
import kotlinx.serialization.Serializable

class MainActivity : AppCompatActivity() {

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //TODO: MAYBE this app (or a new one) becomes the syncing with a custom server
        // It'd use Account Manager
        // Maybe display to all of the sources for each app?
        // It'd have SyncAdapters for every one of the content providers
        // I'd need to modify the content providers to allow for everything that's allowed in the server
        // Login would need to be setup to communicate with the server,
        // making this app very modifiable and won't have any firebase stuff apart of it
        // I'd also need to make sure to notify whenever the databases change.
        // Maybe it'd also be able view everything? Maybe clicking on items open it in that app?
        // Probably also want to show some of the app info? Version code and name?
        // Maybe there's even reverse support? The apps try and detect if OtakuWorld is installed and logged in?
        // Maybe this app even handles firebase? And the OtakuWorld apps are purely local?
        // Maybe use Junie to create a sync strategy?

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }

            MaterialExpressiveTheme(
                colorScheme = colorScheme,
                motionScheme = MotionScheme.expressive()
            ) {

                val backStack = rememberNavBackStack(Info)

                NavDisplay(
                    backStack = backStack,
                    //onBack = { backStack.removeLastOrNull() },
                    onBack = { count ->
                        repeat(count) {
                            if (backStack.isNotEmpty()) {
                                backStack.removeLastOrNull()
                            }
                        }
                    },
                    entryDecorators = listOf(
                        rememberSceneSetupNavEntryDecorator(),
                        rememberSavedStateNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider<NavKey> {
                        entry<Info> { InfoScreen() }
                    },
                    transitionSpec = {
                        // Slide in from right when navigating forward
                        slideInHorizontally(initialOffsetX = { it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { -it })
                    },
                    popTransitionSpec = {
                        // Slide in from left when navigating back
                        slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { it })
                    },
                    predictivePopTransitionSpec = {
                        // Slide in from left when navigating back
                        slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                slideOutHorizontally(targetOffsetX = { it })
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Serializable
data object Info : NavKey