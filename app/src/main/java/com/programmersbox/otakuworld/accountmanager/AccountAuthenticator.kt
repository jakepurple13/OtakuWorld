package com.programmersbox.otakuworld.accountmanager

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.content.Context
import android.content.Intent
import android.os.Bundle

class AccountAuthenticator(
    private val context: Context,
) : AbstractAccountAuthenticator(context) {

    @Throws(NetworkErrorException::class)
    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String,
        authTokenType: String?,
        requiredFeatures: Array<String>?,
        options: Bundle,
    ): Bundle {
        val reply = Bundle()

        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, accountType)
        intent.putExtra(LoginActivity.ARG_AUTH_TOKEN_TYPE, authTokenType)
        intent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true)

        // return our AccountAuthenticatorActivity
        reply.putParcelable(AccountManager.KEY_INTENT, intent)

        return reply
    }

    @Throws(NetworkErrorException::class)
    override fun confirmCredentials(
        arg0: AccountAuthenticatorResponse,
        arg1: Account, arg2: Bundle,
    ): Bundle? {
        return null
    }

    override fun editProperties(arg0: AccountAuthenticatorResponse, arg1: String): Bundle? {
        return null
    }

    override fun addAccountFromCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        accountCredentials: Bundle?,
    ): Bundle {
        return super.addAccountFromCredentials(response, account, accountCredentials)
    }

    @Throws(NetworkErrorException::class)
    override fun getAuthToken(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String,
        options: Bundle,
    ): Bundle {

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        val am = AccountManager.get(context)

        val token = am.peekAuthToken(account, authTokenType)

        if (token != null) {
            val result = Bundle()

            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)

            return result
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, account.type)
        intent.putExtra(LoginActivity.ARG_AUTH_TOKEN_TYPE, authTokenType)

        // This is for the case multiple accounts are stored on the device
        // and the AccountPicker dialog chooses an account without auth token.
        // We can pass out the account name chosen to the user of write it
        // again in the Login activity intent returned.
        if (account != null) {
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name)
        }

        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }

    override fun getAuthTokenLabel(arg0: String): String? {
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun hasFeatures(
        arg0: AccountAuthenticatorResponse,
        arg1: Account,
        arg2: Array<String>,
    ): Bundle? {
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun updateCredentials(
        arg0: AccountAuthenticatorResponse,
        arg1: Account, arg2: String, arg3: Bundle,
    ): Bundle? {
        return null
    }
}
