package com.programmersbox.novel.shared

import androidx.datastore.preferences.core.intPreferencesKey
import com.programmersbox.datastore.DataStoreHandler

/** Body text size in sp for the novel reader. */
val novelTextSize = DataStoreHandler(
    key = intPreferencesKey("novel_text_size"),
    defaultValue = 16,
)
