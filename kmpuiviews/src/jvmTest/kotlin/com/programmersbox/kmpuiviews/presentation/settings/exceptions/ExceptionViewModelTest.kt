package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.programmersbox.favoritesdatabase.ExceptionDatabase
import com.programmersbox.favoritesdatabase.ExceptionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExceptionViewModelTest {

    private val viewModelStore = ViewModelStore()
    private lateinit var dbFile: File
    private lateinit var database: ExceptionDatabase

    // The ViewModel observes ExceptionDao's Room-generated Flow, which emits on Room's own
    // (real, non-test-controlled) dispatcher. A test-dispatcher virtual-clock advance
    // doesn't drive that emission, so wait for it with real time instead.
    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(5_000) {
                while (!condition()) delay(10)
            }
        }
    }

    private fun exception(time: Long, message: String = "message-$time") = ExceptionItem(
        time = time,
        message = message,
    )

    private fun viewModel() = ExceptionViewModel(
        exceptionDao = database.exceptionDao(),
    ).also { viewModelStore.put(System.identityHashCode(it).toString(), it) }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        dbFile = File.createTempFile("exception-viewmodel-test", ".db").also { it.deleteOnExit() }
        database = Room.databaseBuilder<ExceptionDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        viewModelStore.clear()
        Thread.sleep(50)
        Dispatchers.resetMain()
        database.close()
        dbFile.delete()
    }

    @Test fun `starts with no exceptions`() = runTest {
        val vm = viewModel()

        assertTrue(vm.exceptions.first().isEmpty())
    }

    @Test fun `inserted exception shows up in exceptions after collection`() = runTest {
        val dao = database.exceptionDao()
        dao.insertException(exception(time = 1_000L))

        val vm = viewModel()
        awaitCondition { vm.exceptions.first().isNotEmpty() }

        val all = vm.exceptions.first()
        assertEquals(1, all.size)
        assertEquals("message-1000", all[0].message)
    }

    @Test fun `deleteAll clears exceptions`() = runTest {
        val dao = database.exceptionDao()
        dao.insertException(exception(time = 1_000L))
        dao.insertException(exception(time = 2_000L))

        val vm = viewModel()
        awaitCondition { vm.exceptions.first().size == 2 }

        vm.deleteAll()

        awaitCondition { vm.exceptions.first().isEmpty() }
        assertTrue(vm.exceptions.first().isEmpty())
    }

    @Test fun `deleteItem removes only that exception`() = runTest {
        val dao = database.exceptionDao()
        val first = exception(time = 1_000L)
        val second = exception(time = 2_000L)
        dao.insertException(first)
        dao.insertException(second)

        val vm = viewModel()
        awaitCondition { vm.exceptions.first().size == 2 }

        vm.deleteItem(first)

        awaitCondition { vm.exceptions.first().size == 1 }
        val remaining = vm.exceptions.first()
        assertEquals(1, remaining.size)
        assertEquals("message-2000", remaining[0].message)
    }
}
