package com.programmersbox.animeworldtv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class SearchActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, CustomSearchFragment())
                .commit()
        }
    }
}