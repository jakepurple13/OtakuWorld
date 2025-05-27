package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavController
import com.programmersbox.favoritesdatabase.CustomListInfo
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.presentation.NavigationActions
import org.koin.compose.koinInject

expect class BiometricPrompting {

    fun authenticate(
        onAuthenticationSucceeded: () -> Unit,
        onAuthenticationFailed: () -> Unit = {},
        title: String = "Authentication required",
        subtitle: String = "Please Authenticate",
        negativeButtonText: String = "Never Mind",
    )

    fun authenticate(promptInfo: PromptCallback)
}

data class PromptCallback(
    val onAuthenticationSucceeded: () -> Unit,
    val onAuthenticationFailed: () -> Unit = {},
    val title: String = "Authentication required",
    val subtitle: String = "Please Authenticate",
    val negativeButtonText: String = "Never Mind",
)

@Composable
fun rememberBiometricPrompt(
    onAuthenticationSucceeded: () -> Unit,
    onAuthenticationFailed: () -> Unit = {},
    title: String = "Authentication required",
    subtitle: String = "Please Authenticate",
    negativeButtonText: String = "Never Mind",
): PromptCallback {
    val succeed by rememberUpdatedState(onAuthenticationSucceeded)
    val failed by rememberUpdatedState(onAuthenticationFailed)
    return remember(succeed, failed, title, subtitle, negativeButtonText) {
        PromptCallback(
            onAuthenticationSucceeded = succeed,
            onAuthenticationFailed = failed,
            title = title,
            subtitle = subtitle,
            negativeButtonText = negativeButtonText
        )
    }
}

@Composable
expect fun rememberBiometricPrompting(): BiometricPrompting/* {
    val context = LocalContext.current
    val d = remember {
        BiometricPrompt(
            context.findActivity(),
            context.mainExecutor,
            authenticationCallback
        )
    }

    return d
}*/

/*fun biometricPrompting(
    context: Context,
    authenticationCallback: BiometricPrompt.AuthenticationCallback,
) = BiometricPrompt(
    context.findActivity(),
    context.mainExecutor,
    authenticationCallback
)*/

/*
val biometricPrompt = rememberBiometricPrompt(
    onAuthenticationSucceeded = {
        customListViewModel.setList(temp)
        temp = null
        navigate()
    },
    onAuthenticationFailed = {
        customListViewModel.setList(null)
        temp = null
    }
)

biometricPrompting(
    context.findActivity(),
    biometricPrompt
).authenticate(
    BiometricPrompt.PromptInfo.Builder()
        .setTitle("Authentication required")
        .setSubtitle("In order to view ${it.item.name}, please authenticate")
        .setNegativeButtonText("Never Mind")
        .build()
)
 */

/*@Composable
expect fun rememberBiometricPrompt(
    onAuthenticationSucceeded: () -> Unit,
    onAuthenticationFailed: () -> Unit = {},
)*//*: BiometricPrompt.AuthenticationCallback {
    val succeed by rememberUpdatedState(onAuthenticationSucceeded)
    val failed by rememberUpdatedState(onAuthenticationFailed)
    return remember(succeed, failed) {
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                failed()
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult,
            ) {
                super.onAuthenticationSucceeded(result)
                println("Authentication succeeded $result")
                succeed()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                println("Authentication failed")
                failed()
            }
        }
    }
}*/

@Composable
fun rememberBiometricOpening(): BiometricOpen {
    val biometricPrompting = rememberBiometricPrompting()
    val navController = LocalNavController.current
    val navBackStack = LocalNavActions.current
    val itemDao = koinInject<ItemDao>()
    return remember(biometricPrompting, navController, itemDao) {
        BiometricOpen(
            biometricPrompting = biometricPrompting,
            navController = navController,
            navigationActions = navBackStack,
            itemDao = itemDao
        )
    }
}

class BiometricOpen(
    private val biometricPrompting: BiometricPrompting,
    private val navController: NavController,
    private val navigationActions: NavigationActions,
    private val itemDao: ItemDao,
) {
    suspend fun openIfNotIncognito(kmpItemModel: KmpItemModel) {
        if (itemDao.doesIncognitoSourceExistSync(kmpItemModel.url)) {
            biometricPrompting.authenticate(
                //onAuthenticationSucceeded = { navController.navigateToDetails(kmpItemModel) },
                onAuthenticationSucceeded = { navigationActions.details(kmpItemModel) },
                title = "Authenticate to view ${kmpItemModel.title}",
                subtitle = "Authenticate to view media",
                negativeButtonText = "Cancel"
            )
        } else {
            //navController.navigateToDetails(kmpItemModel)
            navigationActions.details(kmpItemModel)
        }
    }

    suspend fun openIfNotIncognito(url: String, title: String, openAction: () -> Unit) {
        if (itemDao.doesIncognitoSourceExistSync(url)) {
            biometricPrompting.authenticate(
                onAuthenticationSucceeded = openAction,
                title = "Authenticate to view $title",
                subtitle = "Authenticate to view media",
                negativeButtonText = "Cancel"
            )
        } else {
            openAction()
        }
    }

    suspend fun openIfNotIncognito(vararg dbModel: DbModel, openAction: () -> Unit) {
        if (dbModel.any { itemDao.doesIncognitoSourceExistSync(it.url) }) {
            biometricPrompting.authenticate(
                onAuthenticationSucceeded = openAction,
                title = "Authenticate to view ${dbModel.firstOrNull()?.title}",
                subtitle = "Authenticate to view media",
                negativeButtonText = "Cancel"
            )
        } else {
            openAction()
        }
    }

    suspend fun openIfNotIncognito(vararg customListInfo: CustomListInfo, openAction: () -> Unit) {
        if (customListInfo.any { itemDao.doesIncognitoSourceExistSync(it.url) }) {
            biometricPrompting.authenticate(
                onAuthenticationSucceeded = openAction,
                title = "Authenticate to view ${customListInfo.firstOrNull()?.title}",
                subtitle = "Authenticate to view media",
                negativeButtonText = "Cancel"
            )
        } else {
            openAction()
        }
    }
}