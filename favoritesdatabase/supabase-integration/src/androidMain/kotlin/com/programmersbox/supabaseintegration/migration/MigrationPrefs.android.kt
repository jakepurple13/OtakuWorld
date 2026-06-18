package com.programmersbox.supabaseintegration.migration

import android.content.Context

class AndroidMigrationPrefs(context: Context) : MigrationPrefs {
    private val prefs = context.getSharedPreferences("supabase_migration", Context.MODE_PRIVATE)
    override fun isMigrationComplete() = prefs.getBoolean("complete", false)
    override fun markMigrationComplete() { prefs.edit().putBoolean("complete", true).apply() }
}

actual fun createMigrationPrefs(context: Any?): MigrationPrefs = AndroidMigrationPrefs(context as Context)
