package com.programmersbox.supabaseintegration.sync

import com.programmersbox.supabaseintegration.auth.AuthManager
import com.programmersbox.supabaseintegration.auth.AuthState
import com.programmersbox.supabaseintegration.client.SupabaseClientProvider
import com.programmersbox.supabaseintegration.sync.syncprocessor.SyncProcessor
import dev.jordond.connectivity.Connectivity
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.time.measureTime

class SyncEngineImpl(
    private val clientProvider: SupabaseClientProvider,
    private val authManager: AuthManager,
    private val connectivityMonitor: ConnectivityMonitor,
    syncProcessors: List<SyncProcessor<*, *>>,
) : SyncEngine {
    private val syncProcessorMap by lazy {
        syncProcessors.associateBy { it.tableName }
    }

    private val client get() = clientProvider.getOrCreate() ?: error("Client not initialized")
    private val userId
        get() = (authManager.authState.value as? AuthState.Authenticated)?.user?.id
            ?: error("Not authenticated")

    override suspend fun pushLocalChanges(): Unit = coroutineScope {
        if (connectivityMonitor.isOnline.value is Connectivity.Status.Disconnected) return@coroutineScope
        val uid = runCatching { userId }.getOrNull() ?: return@coroutineScope
        val client = runCatching { client }.getOrNull() ?: return@coroutineScope

        syncProcessorMap.forEach { (tableName, processor) ->
            pushAndRecordTime(tableName) { processor.push(client, uid) }
        }
    }

    override suspend fun pullRemoteChanges(since: Long, tables: Set<String>?) = coroutineScope {
        if (connectivityMonitor.isOnline.value is Connectivity.Status.Disconnected) return@coroutineScope
        val uid = runCatching { userId }.getOrNull() ?: return@coroutineScope
        val client = runCatching { client }.getOrNull() ?: return@coroutineScope

        if (tables != null) {
            tables.forEach {
                syncProcessorMap[it]?.let { processor ->
                    pullAndRecordTime(it) { processor.pull(client, uid, since) }
                }
            }
        } else {
            syncProcessorMap.forEach { (tableName, processor) ->
                pullAndRecordTime(tableName) { processor.pull(client, uid, since) }
            }
        }
    }

    private fun CoroutineScope.pushAndRecordTime(dbId: String, block: suspend () -> Unit) =
        handleAndRecordTime(dbId, "Pushing", block)

    private fun CoroutineScope.pullAndRecordTime(dbId: String, block: suspend () -> Unit) =
        handleAndRecordTime(dbId, "Pulling", block)

    private fun CoroutineScope.handleAndRecordTime(
        dbId: String,
        direction: String,
        block: suspend () -> Unit,
    ) {
        launch(Dispatchers.IO) {
            val duration = measureTime {
                runCatching { block() }
            }

            println("$direction $dbId took $duration")
        }
    }

    override fun observeLocalChanges(): Flow<Unit> = syncProcessorMap
        .values
        .map {
            it
                .observeDirtyItems()
                .onStart { println("Observing ${it.tableName}") }
        }
        .merge()
        .filter { it > 0 }.map { }

    override suspend fun fullSync() {
        pushLocalChanges()
        pullRemoteChanges(since = 0L)
    }

    override fun subscribeRealtime(scope: CoroutineScope, onEvent: suspend (Set<String>) -> Unit): Job = scope.launch {
        val uid = runCatching { userId }.getOrNull() ?: return@launch
        val client = runCatching { client }.getOrNull() ?: return@launch
        val channel = client.channel("otakuworld-sync-$uid")

        // Buffered: preserves table names so the consumer knows exactly which tables changed.
        val trigger = Channel<String>(Channel.BUFFERED)

        val tables = syncProcessorMap.keys

        tables.forEach { table ->
            channel
                .postgresChangeFlow<PostgresAction>("public") {
                    this.table = table
                    filter("user_id", FilterOperator.EQ, uid)
                }
                .onEach {
                    println(it)
                    trigger.trySend(table)
                }
                .launchIn(this)
        }

        // Single-consumer loop — drains all queued table names into a Set, then syncs only those tables.
        launch {
            for (first in trigger) {
                val changed = mutableSetOf(first)
                var next = trigger.tryReceive()
                while (next.isSuccess) {
                    next.getOrNull()?.let { changed.add(it) }
                    next = trigger.tryReceive()
                }
                onEvent(changed)
            }
        }

        try {
            channel.subscribe(blockUntilSubscribed = true)
            awaitCancellation()
        } finally {
            trigger.close()
            channel.unsubscribe()
            runCatching { client.realtime.removeChannel(channel) }
        }
    }
}
