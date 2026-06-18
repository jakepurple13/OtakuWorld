package com.programmersbox.supabaseintegration.migration

import platform.Foundation.NSUserDefaults

class IosMigrationPrefs : MigrationPrefs {
    private val defaults = NSUserDefaults.standardUserDefaults
    override fun isMigrationComplete() = defaults.boolForKey("supabase_migration_complete")
    override fun markMigrationComplete() { defaults.setBool(true, "supabase_migration_complete") }
}

actual fun createMigrationPrefs(context: Any?): MigrationPrefs = IosMigrationPrefs()
