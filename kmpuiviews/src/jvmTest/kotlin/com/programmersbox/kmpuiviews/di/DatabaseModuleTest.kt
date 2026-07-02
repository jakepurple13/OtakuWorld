package com.programmersbox.kmpuiviews.di

import ca.gosyer.appdirs.AppDirs
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.check.checkKoinModules
import kotlin.test.Test

class DatabaseModuleTest : KoinTest {

    @Test
    fun `databases module resolves every declared DAO and database`() {
        val appDirsModule = module {
            single {
                AppDirs {
                    appName = "DatabaseModuleTest"
                    appAuthor = "jakepurple13"
                }
            }
        }

        checkKoinModules(modules = listOf(databases, appDirsModule))
    }
}
