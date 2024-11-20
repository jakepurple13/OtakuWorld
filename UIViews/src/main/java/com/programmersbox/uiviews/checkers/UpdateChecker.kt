@file:OptIn(DelicateCoroutinesApi::class)

package com.programmersbox.uiviews.checkers

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.programmersbox.extensionloader.SourceLoader
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.favoritesdatabase.toItemModel
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.helpfulutils.GroupBehavior
import com.programmersbox.helpfulutils.NotificationDslBuilder
import com.programmersbox.helpfulutils.SemanticActions
import com.programmersbox.helpfulutils.intersect
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.loggingutils.Loged
import com.programmersbox.loggingutils.fd
import com.programmersbox.models.InfoModel
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.R
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.UPDATE_CHECKING_END
import com.programmersbox.uiviews.utils.UPDATE_CHECKING_START
import com.programmersbox.uiviews.utils.appVersion
import com.programmersbox.uiviews.utils.logFirebaseMessage
import com.programmersbox.uiviews.utils.recordFirebaseException
import com.programmersbox.uiviews.utils.updatePref
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.HttpURLConnection
import java.net.URL


class AppCheckWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), KoinComponent {

    private val logo: NotificationLogo by inject()

    override suspend fun doWork(): Result {
        return try {
            val f = withTimeoutOrNull(60000) { AppUpdate.getUpdate()?.update_real_version.orEmpty() }
            logFirebaseMessage("Current Version: $f")
            val appVersion = applicationContext.appVersion
            if (f != null && AppUpdate.checkForUpdate(appVersion, f)) {
                val n = NotificationDslBuilder.builder(
                    applicationContext,
                    "appUpdate",
                    logo.notificationId
                ) {
                    title = applicationContext.getString(R.string.theresAnUpdate)
                    subText = applicationContext.getString(R.string.versionAvailable, f)
                }
                applicationContext.notificationManager.notify(12, n)
            }
            Result.success()
        } catch (e: Exception) {
            recordFirebaseException(e)
            Result.success()
        }
    }
}

class UpdateFlowWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), KoinComponent {

    private val update by lazy { UpdateNotification(this.applicationContext) }
    private val dao by lazy { ItemDatabase.getInstance(this@UpdateFlowWorker.applicationContext).itemDao() }

    private val genericInfo: GenericInfo by inject()
    private val sourceRepository: SourceRepository by inject()
    private val sourceLoader: SourceLoader by inject()

    companion object {
        const val CHECK_ALL = "check_all"
    }

    override suspend fun doWork(): Result = try {
        update.sendRunningNotification(100, 0, applicationContext.getString(R.string.startingCheck))
        Loged.fd("Starting check here")
        applicationContext.updatePref(UPDATE_CHECKING_START, System.currentTimeMillis())

        Loged.fd("Start")

        val checkAll = inputData.getBoolean(CHECK_ALL, false)

        //Getting all favorites
        val list = listOf(
            if (checkAll) dao.getAllFavoritesSync() else dao.getAllNotifyingFavoritesSync(),
            FirebaseDb.getAllShows().requireNoNulls()
                .let { firebase -> if (checkAll) firebase else firebase.filter { it.shouldCheckForUpdate } }
        )
            .flatten()
            .groupBy(DbModel::url)
            .map { it.value.fastMaxBy(DbModel::numChapters)!! }

        //Making sure we have our sources
        if (sourceRepository.list.isEmpty()) {
            sourceLoader.blockingLoad()
        }

        logFirebaseMessage("Sources: ${sourceRepository.apiServiceList.joinToString { it.serviceName }}")

        val sourceSize = sourceRepository.apiServiceList.size

        // Getting all recent updates
        val newList = list.intersect(
            sourceRepository.apiServiceList
                .filter { s -> list.fastAny { m -> m.source == s.serviceName } }
                .mapIndexedNotNull { index, m ->
                    runCatching {
                        //TODO: Make this easier to handle
                        setProgress(
                            workDataOf(
                                "max" to sourceSize,
                                "progress" to index,
                                "source" to m.serviceName,
                            )
                        )
                        withTimeoutOrNull(10000) {
                            m.getRecentFlow()
                                .catch { emit(emptyList()) }
                                .firstOrNull()
                        }
                    }
                        .onFailure {
                            recordFirebaseException(it)
                            it.printStackTrace()
                        }
                        .getOrNull()
                }.flatten()
        ) { o, n -> o.url == n.url }
            .distinctBy { it.url }

        // Checking if any have updates
        println("Checking for updates")
        val items = newList.mapIndexedNotNull { index, model ->
            update.sendRunningNotification(newList.size, index, model.title)
            setProgress(
                workDataOf(
                    "max" to newList.size,
                    "progress" to index,
                    "source" to model.title,
                )
            )
            runCatching {
                val newData = sourceRepository.toSourceByApiServiceName(model.source)
                    ?.apiService
                    ?.let {
                        withTimeoutOrNull(10000) {
                            model.toItemModel(it).toInfoModel()
                                .firstOrNull()
                                ?.getOrNull()
                        }
                    }
                println("Old: ${model.numChapters} New: ${newData?.chapters?.size}")
                // To test notifications, comment the takeUnless out
                Pair(newData, model)
                    .takeUnless { it.second.numChapters >= (it.first?.chapters?.size ?: -1) }
            }
                .onFailure {
                    recordFirebaseException(it)
                    it.printStackTrace()
                }
                .getOrNull()
        }

        // Saving updates
        update.updateManga(dao, items)
        update.onEnd(update.mapDbModel(dao, items, genericInfo), info = genericInfo)
        Loged.fd("Finished!")

        Result.success()
    } catch (e: Exception) {
        recordFirebaseException(e)
        applicationContext.updatePref(UPDATE_CHECKING_END, System.currentTimeMillis())
        update.sendFinishedNotification()
        Result.success()
    } finally {
        applicationContext.updatePref(UPDATE_CHECKING_END, System.currentTimeMillis())
        update.sendFinishedNotification()
        Result.success()
    }
}

class UpdateNotification(private val context: Context) : KoinComponent {

    private val icon: NotificationLogo by inject()

    suspend fun updateManga(dao: ItemDao, triple: List<Pair<InfoModel?, DbModel>>) {
        triple.fastForEach {
            val item = it.second
            item.numChapters = it.first?.chapters?.size ?: item.numChapters
            dao.insertFavorite(item)
            FirebaseDb.updateShowFlow(item).catch {
                recordFirebaseException(it)
                println("Something went wrong: ${it.message}")
            }.collect()
        }
    }

    suspend fun mapDbModel(dao: ItemDao, list: List<Pair<InfoModel?, DbModel>>, info: GenericInfo) = list.mapIndexed { index, pair ->
        sendRunningNotification(list.size, index, pair.second.title)
        //index + 3 + (Math.random() * 50).toInt() //for a possible new notification value
        dao.insertNotification(
            NotificationItem(
                id = pair.second.hashCode(),
                url = pair.second.url,
                summaryText = context.getString(R.string.hadAnUpdate, pair.second.title, pair.first?.chapters?.firstOrNull()?.name ?: ""),
                notiTitle = pair.second.title,
                imageUrl = pair.second.imageUrl,
                source = pair.second.source,
                contentTitle = pair.second.title
            )
        )
        pair.second.hashCode() to NotificationDslBuilder.builder(
            context,
            "otakuChannel",
            icon.notificationId
        ) {
            title = pair.second.title
            subText = pair.second.source
            getBitmapFromURL(pair.second.imageUrl, pair.first?.extras.orEmpty())?.let {
                largeIconBitmap = it
                pictureStyle {
                    bigPicture = it
                    largeIcon = it
                    contentTitle = pair.first?.chapters?.firstOrNull()?.name ?: ""
                    summaryText = context.getString(
                        R.string.hadAnUpdate,
                        pair.second.title,
                        pair.first?.chapters?.firstOrNull()?.name ?: ""
                    )
                }
            } ?: bigTextStyle {
                contentTitle = pair.first?.chapters?.firstOrNull()?.name ?: ""
                bigText = context.getString(
                    R.string.hadAnUpdate,
                    pair.second.title,
                    pair.first?.chapters?.firstOrNull()?.name.orEmpty()
                )
            }
            showWhen = true
            groupId = "otakuGroup"
            addAction {
                actionTitle = context.getString(R.string.mark_read)
                actionIcon = icon.notificationId
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) semanticAction = SemanticActions.MARK_AS_READ
                pendingActionIntent {
                    val intent = Intent(context, DeleteNotificationReceiver::class.java)
                    intent.action = "NOTIFICATION_DELETED_ACTION"
                    intent.putExtra("url", pair.second.url)
                    intent.putExtra("id", pair.second.hashCode())
                    PendingIntent.getBroadcast(context, pair.second.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
                }
            }
            /*deleteIntent { context ->
                val intent = Intent(context, DeleteNotificationReceiver::class.java)
                intent.action = "NOTIFICATION_DELETED_ACTION"
                intent.putExtra("url", pair.second.url)
                PendingIntent.getBroadcast(context, 0, intent, 0)
            }*/
            pendingIntent { context -> info.deepLinkDetails(context, pair.second.toItemModel(pair.first!!.source)) }
        }
    }

    fun onEnd(list: List<Pair<Int, Notification>>, notificationId: Int = 42, info: GenericInfo) {
        val n = context.notificationManager
        val currentNotificationSize = n.activeNotifications.filterNot { list.fastAny { l -> l.first == it.id } }.size - 1
        list.fastForEach { pair -> n.notify(pair.first, pair.second) }
        if (list.isNotEmpty()) n.notify(
            notificationId,
            NotificationDslBuilder.builder(context, "otakuChannel", icon.notificationId) {
                title = context.getText(R.string.app_name)
                val size = list.size + currentNotificationSize
                subText = context.resources.getQuantityString(R.plurals.updateAmount, size, size)
                showWhen = true
                groupSummary = true
                groupAlertBehavior = GroupBehavior.ALL
                groupId = "otakuGroup"
                pendingIntent { context -> info.deepLinkSettings(context) }
            }
        )
    }

    fun sendRunningNotification(max: Int, progress: Int, contextText: CharSequence = "") {
        val notification = NotificationDslBuilder.builder(context, "updateCheckChannel", icon.notificationId) {
            onlyAlertOnce = true
            ongoing = true
            progress {
                this.max = max
                this.progress = progress
                indeterminate = progress == 0
            }
            showWhen = true
            message = contextText
            subText = context.getString(R.string.checking)
        }
        context.notificationManager.notify(13, notification)
        Loged.fd("Checking for $contextText", showPretty = false)
    }

    fun sendFinishedNotification() {
        val notification = NotificationDslBuilder.builder(context, "updateCheckChannel", icon.notificationId) {
            onlyAlertOnce = true
            subText = context.getString(R.string.finishedChecking)
            timeoutAfter = 750L
        }
        context.notificationManager.notify(13, notification)
    }
}

class DeleteNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val dao by lazy { context?.let { ItemDatabase.getInstance(it).itemDao() } }
        val url = intent?.getStringExtra("url")
        val id = intent?.getIntExtra("id", -1)
        println(url)
        GlobalScope.launch {
            runCatching {
                url?.let { dao?.getNotificationItem(it) }
                    .also { println(it) }
                    ?.let { dao?.deleteNotification(it) }
            }.onFailure { recordFirebaseException(it) }
            id?.let { if (it != -1) context?.notificationManager?.cancel(it) }
            val g = context?.notificationManager?.activeNotifications?.map { it.notification }?.filter { it.group == "otakuGroup" }.orEmpty()
            if (g.size == 1) context?.notificationManager?.cancel(42)
        }
    }
}

class BootReceived : BroadcastReceiver(), KoinComponent {

    private val logo: NotificationLogo by inject()
    private val info: GenericInfo by inject()
    private val sourceRepository: SourceRepository by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        Loged.d("BootReceived")
        println(intent?.action)
        context?.let { SavedNotifications.viewNotificationsFromDb(it, logo, info, sourceRepository) }
    }
}

class NotifySingleWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams), KoinComponent {

    private val logo: NotificationLogo by inject()
    private val genericInfo: GenericInfo by inject()
    private val sourceRepository: SourceRepository by inject()

    override suspend fun doWork(): Result {
        inputData.getString("notiData")
            ?.fromJson<NotificationItem>()
            ?.let { d -> SavedNotifications.viewNotificationFromDb(applicationContext, d, logo, genericInfo, sourceRepository) }
        return Result.success()
    }
}

object SavedNotifications {

    fun viewNotificationFromDb(
        context: Context,
        n: NotificationItem,
        notificationLogo: NotificationLogo,
        info: GenericInfo,
        sourceRepository: SourceRepository,
    ) {
        val icon = notificationLogo.notificationId
        val update = UpdateNotification(context)
        (n.id to NotificationDslBuilder.builder(
            context,
            "otakuChannel",
            icon
        ) {
            title = n.notiTitle
            subText = n.source
            getBitmapFromURL(n.imageUrl)?.let {
                largeIconBitmap = it
                pictureStyle {
                    bigPicture = it
                    largeIcon = it
                    contentTitle = n.contentTitle
                    summaryText = n.summaryText
                }
            } ?: bigTextStyle {
                contentTitle = n.contentTitle
                bigText = n.summaryText
            }
            showWhen = true
            groupId = "otakuGroup"
            addAction {
                actionTitle = context.getString(R.string.mark_read)
                actionIcon = notificationLogo.notificationId
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) semanticAction = SemanticActions.MARK_AS_READ
                pendingActionIntent {
                    val intent = Intent(context, DeleteNotificationReceiver::class.java)
                    intent.action = "NOTIFICATION_DELETED_ACTION"
                    intent.putExtra("url", n.url)
                    intent.putExtra("id", n.id)
                    PendingIntent.getBroadcast(context, n.id, intent, PendingIntent.FLAG_IMMUTABLE)
                }
            }
            /*deleteIntent { context ->
                val intent1 = Intent(context, DeleteNotificationReceiver::class.java)
                intent1.action = "NOTIFICATION_DELETED_ACTION"
                intent1.putExtra("url", n.url)
                PendingIntent.getBroadcast(context, n.id, intent1, 0)
            }*/
            pendingIntent { context ->
                runBlocking {
                    val itemModel = sourceRepository.toSourceByApiServiceName(n.source)
                        ?.apiService
                        ?.getSourceByUrlFlow(n.url)
                        ?.firstOrNull()

                    /*val d = Screen.DetailsScreen.Details(
                        title = itemModel?.title.orEmpty(),
                        imageUrl = itemModel?.imageUrl.orEmpty(),
                        source = itemModel?.source?.serviceName.orEmpty(),
                        url = itemModel?.url.orEmpty(),
                        description = itemModel?.description.orEmpty(),
                    )

                    val c = d.generateRouteWithArgs(
                        mapOf<String, NavType<Any?>>(
                            "title" to NavType.StringType,
                            "imageUrl" to NavType.StringType,
                            "source" to NavType.StringType,
                            "url" to NavType.StringType,
                            "description" to NavType.StringType
                        )
                    )

                    NavDeepLinkBuilder(context)
                        .addDestination(c)
                        .createPendingIntent()*/

                    info.deepLinkDetails(context, itemModel)
                }
            }
        })
            .let { update.onEnd(listOf(it), info = info) }
    }

    fun viewNotificationsFromDb(
        context: Context,
        logo: NotificationLogo,
        info: GenericInfo,
        sourceRepository: SourceRepository,
    ) {
        val dao by lazy { ItemDatabase.getInstance(context).itemDao() }
        val icon = logo.notificationId
        val update = UpdateNotification(context)
        GlobalScope.launch {
            dao.getAllNotifications()
                .fastMap { n ->
                    println(n)
                    n.id to NotificationDslBuilder.builder(
                        context,
                        "otakuChannel",
                        icon
                    ) {
                        title = n.notiTitle
                        subText = n.source
                        getBitmapFromURL(n.imageUrl)?.let {
                            largeIconBitmap = it
                            pictureStyle {
                                bigPicture = it
                                largeIcon = it
                                contentTitle = n.contentTitle
                                summaryText = n.summaryText
                            }
                        } ?: bigTextStyle {
                            contentTitle = n.contentTitle
                            bigText = n.summaryText
                        }
                        showWhen = true
                        groupId = "otakuGroup"
                        addAction {
                            actionTitle = context.getString(R.string.mark_read)
                            actionIcon = logo.notificationId
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) semanticAction = SemanticActions.MARK_AS_READ
                            pendingActionIntent {
                                val intent = Intent(context, DeleteNotificationReceiver::class.java)
                                intent.action = "NOTIFICATION_DELETED_ACTION"
                                intent.putExtra("url", n.url)
                                intent.putExtra("id", n.id)
                                PendingIntent.getBroadcast(context, n.id, intent, PendingIntent.FLAG_IMMUTABLE)
                            }
                        }
                        /*deleteIntent { context ->
                            val intent1 = Intent(context, DeleteNotificationReceiver::class.java)
                            intent1.action = "NOTIFICATION_DELETED_ACTION"
                            intent1.putExtra("url", n.url)
                            PendingIntent.getBroadcast(context, n.id, intent1, 0)
                        }*/
                        pendingIntent { context ->
                            runBlocking {
                                val itemModel = sourceRepository.toSourceByApiServiceName(n.source)//UpdateWorker.sourceFromString(n.source)
                                    ?.apiService
                                    ?.getSourceByUrlFlow(n.url)
                                    ?.firstOrNull()

                                info.deepLinkDetails(context, itemModel)
                            }
                        }
                    }
                }
                .let { update.onEnd(it, info = info) }
        }
    }
}

private fun getBitmapFromURL(strURL: String?, headers: Map<String, Any> = emptyMap()): Bitmap? = runCatching {
    val url = URL(strURL)
    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
    headers.forEach { connection.setRequestProperty(it.key, it.value.toString()) }
    connection.doInput = true
    connection.connect()
    BitmapFactory.decodeStream(connection.inputStream)
}
    .onFailure {
        logFirebaseMessage("Getting bitmap from $strURL")
        recordFirebaseException(it)
        it.printStackTrace()
    }
    .getOrNull()