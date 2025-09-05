package com.programmersbox.otakuworld.syncadapters

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MangaFavoritesSyncService : Service() {
    override fun onCreate() {
        super.onCreate()
        synchronized(syncAdapterLock) {
            syncAdapter = syncAdapter ?: FavoritesSyncAdapter(applicationContext)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return syncAdapter?.syncAdapterBinder ?: throw IllegalStateException()
    }

    companion object {
        private var syncAdapter: FavoritesSyncAdapter? = null
        private val syncAdapterLock = Any()
    }
}

class NovelFavoritesSyncService : Service() {
    override fun onCreate() {
        super.onCreate()
        synchronized(syncAdapterLock) {
            syncAdapter = syncAdapter ?: FavoritesSyncAdapter(applicationContext)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return syncAdapter?.syncAdapterBinder ?: throw IllegalStateException()
    }

    companion object {
        private var syncAdapter: FavoritesSyncAdapter? = null
        private val syncAdapterLock = Any()
    }
}

class AnimeFavoritesSyncService : Service() {
    override fun onCreate() {
        super.onCreate()
        synchronized(syncAdapterLock) {
            syncAdapter = syncAdapter ?: FavoritesSyncAdapter(applicationContext)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return syncAdapter?.syncAdapterBinder ?: throw IllegalStateException()
    }

    companion object {
        private var syncAdapter: FavoritesSyncAdapter? = null
        private val syncAdapterLock = Any()
    }
}