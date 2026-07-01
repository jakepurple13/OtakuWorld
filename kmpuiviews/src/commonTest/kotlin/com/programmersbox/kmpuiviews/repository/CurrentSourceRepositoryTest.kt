package com.programmersbox.kmpuiviews.repository

import app.cash.turbine.test
import com.programmersbox.kmpmodels.ExampleService
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull

class CurrentSourceRepositoryTest {

    @Test fun `asFlow starts with null`() = runTest {
        val repository = CurrentSourceRepository()

        repository.asFlow().test {
            assertNull(awaitItem())
        }
    }

    @Test fun `emit publishes the new source to asFlow`() = runTest {
        val repository = CurrentSourceRepository()
        val service = ExampleService()

        repository.asFlow().test {
            assertNull(awaitItem())
            repository.emit(service)
            val emitted = awaitItem()
            assert(emitted === service)
        }
    }

    @Test fun `tryEmit publishes synchronously without suspending`() = runTest {
        val repository = CurrentSourceRepository()
        val service = ExampleService()

        repository.tryEmit(service)

        repository.asFlow().test {
            assert(awaitItem() === service)
        }
    }
}
