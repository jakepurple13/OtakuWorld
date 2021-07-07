package com.programmersbox.animeworldtv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class FavoritesActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.favorites_browse_fragment, FavoriteFragment())
                .commitNow()
        }
    }
}