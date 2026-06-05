package com.programmersbox.koogintegration.integrator

abstract class KoogIntegrator<T> {
    abstract suspend fun map(input: T): String
}