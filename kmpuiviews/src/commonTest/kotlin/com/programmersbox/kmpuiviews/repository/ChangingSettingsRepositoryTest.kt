package com.programmersbox.kmpuiviews.repository

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ChangingSettingsRepositoryTest {

    @Test fun `showNavBar defaults to true`() = runTest {
        val repository = ChangingSettingsRepository()

        repository.showNavBar.test {
            assertTrue(awaitItem())
        }
    }

    @Test fun `showInsets defaults to true`() = runTest {
        val repository = ChangingSettingsRepository()

        repository.showInsets.test {
            assertTrue(awaitItem())
        }
    }

    @Test fun `setting showNavBar value updates it`() = runTest {
        val repository = ChangingSettingsRepository()

        repository.showNavBar.test {
            assertTrue(awaitItem())
            repository.showNavBar.value = false
            assertTrue(!awaitItem())
        }
    }

    @Test fun `setting showInsets value updates it`() = runTest {
        val repository = ChangingSettingsRepository()

        repository.showInsets.test {
            assertTrue(awaitItem())
            repository.showInsets.value = false
            assertTrue(!awaitItem())
        }
    }

    @Test fun `changing showNavBar does not affect showInsets`() = runTest {
        val repository = ChangingSettingsRepository()

        repository.showNavBar.value = false

        repository.showInsets.test {
            assertTrue(awaitItem())
        }
    }

    @Test fun `changing showInsets does not affect showNavBar`() = runTest {
        val repository = ChangingSettingsRepository()

        repository.showInsets.value = false

        repository.showNavBar.test {
            assertTrue(awaitItem())
        }
    }
}
