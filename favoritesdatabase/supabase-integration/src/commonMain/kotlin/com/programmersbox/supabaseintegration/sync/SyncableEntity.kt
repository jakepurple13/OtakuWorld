package com.programmersbox.supabaseintegration.sync

interface SyncableEntity {
    val supabaseId: String?
    val createdAt: Long
    val updatedAt: Long
    val isDeleted: Boolean
    val isDirty: Boolean
}
