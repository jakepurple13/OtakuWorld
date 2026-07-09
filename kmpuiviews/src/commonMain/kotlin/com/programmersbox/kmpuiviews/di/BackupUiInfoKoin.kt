package com.programmersbox.kmpuiviews.di

import com.programmersbox.sharedcomponents.backup.BackupUiInfo
import com.programmersbox.sharedtools.BackupProcessor
import org.koin.core.module.Module
import org.koin.core.module.dsl.new
import org.koin.core.qualifier.named
import org.koin.dsl.bind

inline fun <reified T> Module.backupProcessorWithUiInfo(
    named: String,
    crossinline factoryBlock: () -> T,
) where T : BackupProcessor, T : BackupUiInfo {
    val definition = factory(named(named)) { new(factoryBlock) }
    definition.bind(BackupProcessor::class)
    definition.bind(BackupUiInfo::class)
}
