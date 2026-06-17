package com.programmersbox.supabaseintegration.sync

data class SyncConfig(
    val pollIntervalMs: Long = 5 * 60 * 1000L,
    val maxRetries: Int = 5,
    val initialBackoffMs: Long = 1_000L,
    val maxBackoffMs: Long = 30_000L,
)
