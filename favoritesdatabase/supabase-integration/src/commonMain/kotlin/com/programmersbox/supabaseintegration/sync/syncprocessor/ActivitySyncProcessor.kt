package com.programmersbox.supabaseintegration.sync.syncprocessor

import com.programmersbox.favoritesdatabase.ActivityDao
import com.programmersbox.favoritesdatabase.ActivityTable
import com.programmersbox.supabaseintegration.database.ActivityManagedTable
import com.programmersbox.supabaseintegration.database.ManagedTable
import com.programmersbox.supabaseintegration.sync.ActivityRow
import com.programmersbox.supabaseintegration.sync.BackupPreferenceRepository
import com.programmersbox.supabaseintegration.sync.toActivityRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class ActivitySyncProcessor(
    private val activityDao: ActivityDao,
    override val backupPreferenceRepository: BackupPreferenceRepository,
) : SyncProcessor<ActivityTable, ActivityRow>(tableName = "activity_timer"),
    ManagedTable by ActivityManagedTable(activityDao) {

    override val displayName: String = "Activity Timer"

    // Higher-value-wins push: fetch remote, compare, upsert the winner both places.
    override suspend fun push(client: SupabaseClient, uid: String) {
        if (!isBackupEnabled()) return
        val local = activityDao.getActivity() ?: return
        if (!local.isDirty) return

        val remote = client.postgrest[tableName].select { filter { eq("user_id", uid) } }
            .decodeSingleOrNull<ActivityRow>()
        val winner = maxOf(local.cumulativeSeconds, remote?.cumulativeSeconds ?: 0L)
        val timestamp = Clock.System.now().toEpochMilliseconds()

        client.postgrest[tableName].upsert(
            local.toActivityRow(uid, timestamp).copy(cumulativeSeconds = winner)
        ) { onConflict = "user_id" }

        activityDao.upsertSynced(winner, timestamp)
    }

    // Higher-value-wins pull: only overwrite local if remote is strictly greater.
    override suspend fun pull(client: SupabaseClient, uid: String, since: Long) {
        if (!isBackupEnabled()) return
        val remote = client.postgrest[tableName].select { filter { eq("user_id", uid) } }
            .decodeSingleOrNull<ActivityRow>() ?: return
        val local = activityDao.getActivity()
        if (remote.cumulativeSeconds > (local?.cumulativeSeconds ?: 0L)) {
            activityDao.upsertSynced(remote.cumulativeSeconds, remote.updatedAt)
        }
    }

    // Remaining abstract members from SyncProcessor are unused by our overridden
    // push()/pull() but must be implemented to satisfy the base class contract.
    override suspend fun getDirtyItems(): List<ActivityTable> =
        activityDao.getActivity()?.takeIf { it.isDirty }?.let { listOf(it) } ?: emptyList()

    override fun observeDirtyItems(): Flow<Int> =
        activityDao.observeActivity().map { if (it?.isDirty == true) 1 else 0 }.distinctUntilChanged()

    override fun isLocalDeleted(local: ActivityTable): Boolean = false

    override fun getLocalUpdatedAt(local: ActivityTable): Long = local.updatedAt

    override fun toRemoteRow(local: ActivityTable, uid: String, timestamp: Long): ActivityRow =
        local.toActivityRow(uid, timestamp)

    override suspend fun markLocalSynced(local: ActivityTable, timestamp: Long) {
        activityDao.upsertSynced(local.cumulativeSeconds, timestamp)
    }

    override suspend fun deleteLocal(local: ActivityTable) = Unit

    override suspend fun performUpsert(client: SupabaseClient, items: List<ActivityRow>) {
        client.postgrest[tableName].upsert(items) { onConflict = "user_id" }
    }

    override fun isRemoteDeleted(remote: ActivityRow): Boolean = false

    override fun getRemoteUpdatedAt(remote: ActivityRow): Long = remote.updatedAt

    override suspend fun getLocalEquivalent(remote: ActivityRow): ActivityTable? = activityDao.getActivity()

    override suspend fun upsertLocal(remote: ActivityRow) {
        activityDao.upsertSynced(remote.cumulativeSeconds, remote.updatedAt)
    }

    override suspend fun performSelect(postgrestResult: PostgrestResult): List<ActivityRow> =
        postgrestResult.decodeList<ActivityRow>()
}
