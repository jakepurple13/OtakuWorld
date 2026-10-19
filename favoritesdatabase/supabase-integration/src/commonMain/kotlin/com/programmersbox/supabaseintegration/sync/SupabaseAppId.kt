package com.programmersbox.supabaseintegration.sync

/**
 * Identifies which app (e.g. "mangaworld") owns the rows it syncs, so apps signed into
 * the same account keep separate data. Stored in the `app` column of every synced table.
 */
data class SupabaseAppId(val value: String)

/** Thrown when the Supabase project predates the `app` column. */
class SchemaOutdatedException(cause: Throwable) : Exception(
    "Supabase schema is out of date. Run docs/supabase/migration_add_app_column.sql in your project's SQL Editor.",
    cause
)
