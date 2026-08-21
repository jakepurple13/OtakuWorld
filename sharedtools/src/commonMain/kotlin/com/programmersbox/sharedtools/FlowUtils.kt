package com.programmersbox.sharedtools

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * Cancels the flow gracefully after the given total duration,
 * regardless of emission frequency.
 */
fun <T> Flow<T>.cancelAfter(duration: Duration): Flow<T> = flow {
    withTimeoutOrNull(duration) {
        // "this@cancelAfter" refers to the upstream flow
        this@cancelAfter.collect { value ->
            emit(value)
        }
    }
}