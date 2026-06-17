package com.programmersbox.supabaseintegration.migration

import java.io.File

class JvmMigrationPrefs : MigrationPrefs {
    private val file = File(System.getProperty("user.home"), ".otakuworld/migration_complete")
    override fun isMigrationComplete() = file.exists()
    override fun markMigrationComplete() { file.parentFile?.mkdirs(); file.createNewFile() }
}

actual fun createMigrationPrefs(context: Any?): MigrationPrefs = JvmMigrationPrefs()
