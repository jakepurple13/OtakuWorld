package com.programmersbox.mihon

import android.app.Application
import android.content.Context
import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.fullType

/** Registers what Mihon extensions expect to find in Injekt. Must run before any extension class loads. */
object MihonRuntime {
    @Volatile
    private var initialized = false

    fun init(application: Application) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            Injekt.addSingleton(fullType<Application>(), application)
            Injekt.addSingleton(fullType<Context>(), application)
            Injekt.addSingletonFactory(fullType<NetworkHelper>()) { NetworkHelper(application) }
            Injekt.addSingletonFactory(fullType<JavaScriptEngine>()) { JavaScriptEngine(application) }
            Injekt.addSingletonFactory(fullType<Json>()) {
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            }
            Injekt.addSingletonFactory(fullType<ProtoBuf>()) { ProtoBuf }
            initialized = true
        }
    }
}
