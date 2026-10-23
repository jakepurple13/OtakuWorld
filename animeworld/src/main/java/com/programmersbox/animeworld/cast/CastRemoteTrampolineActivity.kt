package com.programmersbox.animeworld.cast

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.programmersbox.animeworld.MainActivity

/** Target of the Cast notification and expanded controller: opens the remote screen in [MainActivity]. */
class CastRemoteTrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.CAST_REMOTE), this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }
}
