package com.programmersbox.otakuworld.providers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.net.toUri
import com.programmersbox.otakuworld.IncognitoSource


class IncognitoSourceContentHelper(uri: Uri) {

    private val contentUri = "content://$uri/incognito".toUri()

    fun getAllIncognitoSources(context: Context): Cursor? = context
        .contentResolver
        .query(contentUri, null, null, null, null)

    fun getAllIncognitoSourcesFlow(context: Context) = context
        .contentResolver
        .observeUri(contentUri) { getAllIncognitoSources(context)?.let { mapIncognitoSources(it) } }

    private fun mapIncognitoSources(cursor: Cursor): List<IncognitoSource> {
        val sources = mutableListOf<IncognitoSource>()
        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val isIncognito = cursor.getInt(cursor.getColumnIndexOrThrow("isIncognito")) == 1
                val source = cursor.getString(cursor.getColumnIndexOrThrow("source"))
                sources.add(IncognitoSource(source, name, isIncognito))
            } while (cursor.moveToNext())
        }
        return sources
    }

    fun deleteIncognitoSource(context: Context, source: String): Int = context
        .contentResolver
        .delete(
            contentUri,
            "source=?",
            arrayOf(source)
        )

    fun updateIncognitoSource(context: Context, source: IncognitoSource): Int = context
        .contentResolver
        .update(
            contentUri,
            ContentValues().apply {
                put("name", source.name)
                put("isIncognito", if (source.isIncognito) 1 else 0)
                put("source", source.source)
            },
            "source=?",
            arrayOf(source.source)
        )
}
