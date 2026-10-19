package com.programmersbox.supabaseintegration.migration

import java.io.File

// Desktop apps share ~/.otakuworld, so the flag is per app
class JvmMigrationPrefs(appId: String) : MigrationPrefs {
    private val file = File(System.getProperty("user.home"), ".otakuworld/migration_complete_$appId")
    override fun isMigrationComplete() = file.exists()
    override fun markMigrationComplete() { file.parentFile?.mkdirs(); file.createNewFile() }
}
