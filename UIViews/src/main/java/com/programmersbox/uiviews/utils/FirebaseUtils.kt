package com.programmersbox.uiviews.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.programmersbox.loggingutils.Loged
import com.programmersbox.loggingutils.fa
import com.programmersbox.loggingutils.fd
import com.programmersbox.uiviews.R

object FirebaseAuthentication {

    private val RC_SIGN_IN = 32

    private var gso: GoogleSignInOptions? = null

    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private var googleSignInClient: GoogleSignInClient? = null

    fun authenticate(context: Context) {
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso!!)
    }

    fun signIn(activity: Activity) {
        //val signInIntent = googleSignInClient!!.signInIntent
        //activity.startActivityForResult(signInIntent, RC_SIGN_IN)
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent
        activity.startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setTheme(R.style.Theme_OtakuWorld)
                //.setLogo(R.mipmap.big_logo)
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    fun signOut() {
        auth.signOut()
        //currentUser = null
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, context: Context) {
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                //currentUser = auth.currentUser//FirebaseAuth.getInstance().currentUser
                Loged.fd(currentUser)
                // ...
                //val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                //val account = task.getResult(ApiException::class.java)!!
                //Loged.d("firebaseAuthWithGoogle:" + account.id)
                //googleAccount = account
                //firebaseAuthWithGoogle(account.idToken!!)
                Toast.makeText(context, "Signed in Successfully", Toast.LENGTH_SHORT).show()
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Loged.fa(response?.error?.errorCode)
                Toast.makeText(context, "Signed in Unsuccessfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, context: Context, activity: Activity) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(activity) { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                //Log.d(TAG, "signInWithCredential:success")
                //val user = auth.currentUser
                //updateUI(user)
                //currentUser = auth.currentUser
                Toast.makeText(context, "Signed in Successfully", Toast.LENGTH_SHORT).show()
            } else {
                // If sign in fails, display a message to the user.
                //Log.w(TAG, "signInWithCredential:failure", task.exception)
                // ...
                //Snackbar.make(view, "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
                //updateUI(null)
                Toast.makeText(context, "Signed in Unsuccessfully", Toast.LENGTH_SHORT).show()
            }

            // ...
        }
    }

    fun onStart() {
        //currentUser = auth.currentUser
    }

    /*companion object {
        //var googleAccount: GoogleSignInAccount? = null
        //private set
        val currentUser: FirebaseUser? get() = FirebaseAuth.getInstance().currentUser
        //private set
    }*/

    val currentUser: FirebaseUser? get() = FirebaseAuth.getInstance().currentUser

}