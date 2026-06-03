package com.programmersbox.kmpuiviews.repository

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.MediaCheckerNetworkType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.ExtensionWatcher
import com.programmersbox.kmpuiviews.KmpFirebaseConnectionImpl
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.di.kmpModule
import com.programmersbox.kmpuiviews.domain.MediaUpdateChecker
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoModel
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandlerImpl.Companion.ManualSyncId
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import io.github.kdroidfilter.nucleus.scheduler.DesktopBootReceiver
import io.github.kdroidfilter.nucleus.scheduler.DesktopTask
import io.github.kdroidfilter.nucleus.scheduler.DesktopTaskScheduler
import io.github.kdroidfilter.nucleus.scheduler.ExistingTaskPolicy
import io.github.kdroidfilter.nucleus.scheduler.NetworkType
import io.github.kdroidfilter.nucleus.scheduler.TaskContext
import io.github.kdroidfilter.nucleus.scheduler.TaskId
import io.github.kdroidfilter.nucleus.scheduler.TaskRegistry
import io.github.kdroidfilter.nucleus.scheduler.TaskRequest
import io.github.kdroidfilter.nucleus.scheduler.TaskResult
import io.github.kdroidfilter.nucleus.scheduler.inputData
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File
import kotlin.time.Duration.Companion.hours

class BackgroundWorkHandlerImpl(
    private val settingsHandling: NewSettingsHandling,
) : BackgroundWorkHandler {
    override fun localToCloudListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())

    override fun cloudToLocalListener(): Flow<List<WorkInfoKmp>> = flowOf(emptyList())

    override fun syncLocalToCloud() {

    }

    override fun syncCloudToLocal() {

    }

    override fun setupPeriodicCheckers() {
        val scheduler = DesktopTaskScheduler
        settingsHandling
            .mediaCheckerSettings
            .asFlow()
            .onEach {
                if (it.shouldRun) {
                    scheduler.enqueue(
                        TaskRequest.periodic(
                            taskId = SyncId,
                            interval = it.interval.hours,
                        ) {
                            constraints {
                                requiresCharging = it.requiresCharging
                                requiresBatteryNotLow = it.requiresBatteryNotLow
                                requiredNetworkType = when (it.networkType) {
                                    MediaCheckerNetworkType.Connected -> NetworkType.CONNECTED
                                    else -> NetworkType.UNMETERED
                                }
                            }
                            existingTaskPolicy(ExistingTaskPolicy.KEEP)
                        }
                    )
                } else {
                    scheduler.cancel(SyncId)
                }
            }
            .launchIn(GlobalScope)
    }

    override fun workerInfoFlow(): Flow<List<WorkerInfoModel>> = flowOf(emptyList())

    override fun sourceUpdate() {

    }

    override fun cancel(uuid: String) {

    }

    override fun startBackup(file: PlatformFile) {}

    override fun startRestore(file: PlatformFile) {}

    companion object {
        val SyncId = TaskId("sync")
        val ManualSyncId = TaskId("manualSync")

        fun setupSyncCheckers(args: Array<String>): Boolean {
            if (DesktopBootReceiver.isSchedulerInvocation(args)) {
                startKoin {
                    modules(
                        module {
                            single {
                                AppConfig(
                                    "MangaWorld",
                                    BuildType.NoFirebase,
                                    false
                                )
                            }
                            single {
                                ExtensionWatcher(
                                    extensionsDir = get<MangaDesktopSettings>()
                                        .extensionDirectory
                                        .asFlow()
                                )
                            }
                        },
                        module {
                            includes(kmpModule)

                            singleOf<KmpFirebaseConnection>(::KmpFirebaseConnectionImpl)
                            factory<KmpFirebaseConnection.KmpFirebaseListener> { KmpFirebaseConnectionImpl.KmpFirebaseListenerImpl() }

                            singleOf(::DataStoreHandling)
                            single {
                                NewSettingsHandling(
                                    createProtobuf(
                                        serializer = SettingsSerializer(),
                                        fileName = File(
                                            System.getProperty("user.home"),
                                            "Settings.preferences_pb"
                                        ).absolutePath,
                                    ),
                                )
                            }
                        }
                    )
                }

                val registry = TaskRegistry.Builder()
                    .register(SyncId) { SyncCheckWorker() }
                    .register(ManualSyncId) { SyncCheckWorker() }
                    .build()
                DesktopBootReceiver.handle(args = args, registry = registry)
                return true// Don't open the UI
            }
            return false
        }
    }
}

class SyncCheckWorker : DesktopTask, KoinComponent {
    private val mediaUpdateChecker: MediaUpdateChecker by inject()
    private val dataStoreHandling: DataStoreHandling by inject()

    override suspend fun doWork(context: TaskContext): TaskResult {
        return try {
            //TODO: Need to implement notifications
            val data = context.inputData<SyncCheckData>()
            println("Data: $data")
            //update.sendRunningNotification(100, 0, getString(Res.string.startingCheck))
            runCatching {
                /*setForeground(
                    createNotification(
                        max = 100,
                        progress = 0,
                        contextText = getString(Res.string.startingCheck)
                    )
                )*/
            }
            logFirebaseMessage("Starting check here")
            dataStoreHandling.updateCheckingStart.set(System.currentTimeMillis())
            logFirebaseMessage("Start")

            val items = mediaUpdateChecker.getFavoritesThatNeedUpdates(
                checkAll = true,
                putMetric = { name, value -> },
                notificationUpdate = { max, progress, source ->
                    println("Progress: $progress, Max: $max, Source: $source")
                    //update.sendRunningNotification(max, progress, source)
                    //runCatching { setForeground(createNotification(max, progress, source)) }
                },
                setProgress = { max, progress, source ->
                    println("Progress: $progress, Max: $max, Source: $source")
                    /*setProgress(
                        workDataOf(
                            "max" to max,
                            "progress" to progress,
                            "source" to source,
                        )
                    )*/
                }
            )

            println("Items: $items")

            //update.onEnd(update.mapDbModel(items))/* { id, notification -> setForegroundInfo(id, notification) }*/
            logFirebaseMessage("Finished!")

            if (data?.cancel == true) {
                val scheduler = DesktopTaskScheduler
                val cancel = scheduler.cancel(ManualSyncId)
                println("Cancelled: $cancel")
            }

            TaskResult.Success
        } catch (e: Exception) {
            dataStoreHandling.updateCheckingEnd.set(System.currentTimeMillis())
            //update.sendFinishedNotification()
            TaskResult.Failure(e.message.orEmpty())
        }
    }

    @Serializable
    data class SyncCheckData(
        val cancel: Boolean,
    )
}