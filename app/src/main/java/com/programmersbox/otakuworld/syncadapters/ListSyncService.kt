package com.programmersbox.otakuworld.syncadapters

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.koin.android.ext.android.get

class MangaListSyncService : Service() {
    override fun onCreate() {
        super.onCreate()
        synchronized(syncAdapterLock) {
            syncAdapter = syncAdapter ?: ListSyncAdapter(
                context = applicationContext,
                serverHandler = get()
            )
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return syncAdapter?.syncAdapterBinder ?: throw IllegalStateException()
    }

    companion object {
        private var syncAdapter: ListSyncAdapter? = null
        private val syncAdapterLock = Any()
    }
}

class NovelListSyncService : Service() {
    override fun onCreate() {
        super.onCreate()
        synchronized(syncAdapterLock) {
            syncAdapter = syncAdapter ?: ListSyncAdapter(
                context = applicationContext,
                serverHandler = get()
            )
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return syncAdapter?.syncAdapterBinder ?: throw IllegalStateException()
    }

    companion object {
        private var syncAdapter: ListSyncAdapter? = null
        private val syncAdapterLock = Any()
    }
}

class AnimeListSyncService : Service() {
    override fun onCreate() {
        super.onCreate()
        synchronized(syncAdapterLock) {
            syncAdapter = syncAdapter ?: ListSyncAdapter(
                context = applicationContext,
                serverHandler = get()
            )
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return syncAdapter?.syncAdapterBinder ?: throw IllegalStateException()
    }

    companion object {
        private var syncAdapter: ListSyncAdapter? = null
        private val syncAdapterLock = Any()
    }
}