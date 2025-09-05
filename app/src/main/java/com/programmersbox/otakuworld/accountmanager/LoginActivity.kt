package com.programmersbox.otakuworld.accountmanager

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.programmersbox.otakuworld.App
import com.programmersbox.otakuworld.AppInfo
import com.programmersbox.otakuworld.BuildConfig
import com.programmersbox.otakuworld.OtakuProvider
import org.koin.android.ext.android.inject

class AuthenticatorService : Service() {
    private val authenticator by lazy { AccountAuthenticator(this) }
    override fun onBind(intent: Intent): IBinder? {
        var binder: IBinder? = null
        if (intent.action == AccountManager.ACTION_AUTHENTICATOR_INTENT) {
            binder = authenticator.iBinder
        }
        return binder
    }
}

class LoginActivity : CustomAccountAuthenticatorActivity() {

    private val accountManager: AccountManager by lazy { AccountManager.get(this) }

    private val appInfo by inject<AppInfo>()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!BuildConfig.DEBUG)
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val name = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
                darkTheme -> darkColorScheme()
                else -> expressiveLightColorScheme()
            }

            MaterialExpressiveTheme(colorScheme) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Button(
                        onClick = {
                            createAccountWith(
                                AccountInfo(
                                    username = "Otaku!",
                                    password = "null"
                                )
                            )
                        }
                    ) { Text(text = "Login") }
                }
            }
        }
    }

    private fun createAccountWith(
        accountInfo: AccountInfo,
    ) {
        val intent = Intent()

        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountInfo.username)
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, BuildConfig.ACCOUNT_TYPE)
        intent.putExtra(PARAM_USER_PASSWORD, accountInfo.password)

        val account = Account(
            accountInfo.username,
            intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
                ?: BuildConfig.ACCOUNT_TYPE
        )

        intent.putExtra(AccountManager.KEY_AUTHTOKEN, "")

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server
            // to authenticate the user)
            accountManager.addAccountExplicitly(
                account,
                accountInfo.password,
                Bundle.EMPTY
            )

            accountManager.setAccountVisibilityForPackages(
                accounts = arrayOf(account),
                packages = App.entries.map { app ->
                    OtakuProvider.OtakuBuilder()
                        .setPackage(app)
                        .setProvider(appInfo.provider)
                        .build()
                },
                visibility = AccountManager.VISIBILITY_VISIBLE
            ).onFailure { it.printStackTrace() }
        } else {
            accountManager.setPassword(account, accountInfo.password)
        }

        setAccountAuthenticatorResult(intent.extras)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun AccountManager.setAccountVisibilityForPackages(
        accounts: Array<out Account>,
        packages: List<String>,
        visibility: Int = AccountManager.VISIBILITY_VISIBLE,
    ) = runCatching {
        accounts.forEach {
            packages.forEach { pkg ->
                setAccountVisibility(
                    it,
                    pkg,
                    visibility
                )
            }
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    companion object {
        const val ARG_ACCOUNT_TYPE: String = "accountType"
        const val ARG_AUTH_TOKEN_TYPE: String = "authTokenType"
        const val ARG_IS_ADDING_NEW_ACCOUNT: String = "isAddingNewAccount"
        const val PARAM_USER_PASSWORD: String = "password"
    }
}

abstract class CustomAccountAuthenticatorActivity : ComponentActivity() {
    protected var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var mResultBundle: Bundle? = null

    /**
     * Set the result that is to be sent as the result of the request that caused this
     * Activity to be launched. If result is null or this method is never called then
     * the request will be canceled.
     * @param result this is returned as the result of the AbstractAccountAuthenticator request
     */
    fun setAccountAuthenticatorResult(result: Bundle?) {
        mResultBundle = result
    }

    /**
     * Retrieves the AccountAuthenticatorResponse from either the intent of the icicle, if the
     * icicle is non-zero.
     * @param savedInstanceState the save instance data of this Activity, may be null
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accountAuthenticatorResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                AccountAuthenticatorResponse::class.java
            )
        } else {
            intent.getParcelableExtra(
                AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE
            )
        }

        accountAuthenticatorResponse?.onRequestContinued()
    }

    /**
     * Sends the result or a Constants.ERROR_CODE_CANCELED error if a result isn't present.
     */
    override fun finish() {
        if (accountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                accountAuthenticatorResponse!!.onResult(mResultBundle)
            } else {
                accountAuthenticatorResponse!!.onError(
                    AccountManager.ERROR_CODE_CANCELED,
                    "canceled"
                )
            }
            accountAuthenticatorResponse = null
        }
        super.finish()
    }
}

data class AccountInfo(
    val username: String,
    val password: String,
)