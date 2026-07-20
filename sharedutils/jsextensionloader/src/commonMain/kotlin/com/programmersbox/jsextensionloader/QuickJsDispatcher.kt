package com.programmersbox.jsextensionloader

import kotlinx.coroutines.CoroutineDispatcher

/**
 * QuickJs records its native stack-overflow check relative to whichever OS thread
 * created the context. Dispatching later evaluate() calls onto a different thread
 * (e.g. a shared Dispatchers.Default pool) compares that baseline against an
 * unrelated thread's stack pointer, which QuickJs reports as a spurious "stack
 * overflow" - so every use of a given QuickJs instance, from creation onward, must
 * be confined to the single thread this returns.
 */
expect fun singleThreadQuickJsDispatcher(name: String): CoroutineDispatcher

expect fun closeQuickJsDispatcher(dispatcher: CoroutineDispatcher)
