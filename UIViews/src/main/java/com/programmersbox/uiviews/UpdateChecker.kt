package com.programmersbox.uiviews

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
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.programmersbox.favoritesdatabase.*
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.helpfulutils.*
import com.programmersbox.loggingutils.Loged
import com.programmersbox.loggingutils.fd
import com.programmersbox.models.InfoModel
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.utils.*
import io.reactivex.Single
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit


class AppCheckWorker(context: Context, workerParams: WorkerParameters) : RxWorker(context, workerParams), KoinComponent {

    private val logo: NotificationLogo by inject()

    override fun createWork(): Single<Result> = Single.create<Result> {
        try {
            val f = AppUpdate.getUpdate()?.update_real_version.orEmpty()
            val appVersion = applicationContext.packageManager?.getPackageInfo(applicationContext.packageName, 0)?.versionName.orEmpty()
            if (AppUpdate.checkForUpdate(appVersion, f)) {
                val n = NotificationDslBuilder.builder(
                    applicationContext,
                    "appUpdate",
                    logo.notificationId
                ) {
                    title = applicationContext.getString(R.string.theresAnUpdate)
                    subText = applicationContext.getString(R.string.versionAvailable, f)
                    pendingIntent { context ->
                        NavDeepLinkBuilder(context)
                            .setDestination(Screen.SettingsScreen.route)
                            //.setGraph(R.navigation.setting_nav)
                            //.setDestination(R.id.settingsFragment)
                            .createPendingIntent()
                    }

                }

                applicationContext.notificationManager.notify(12, n)

                it.onSuccess(Result.success())
            }
        } catch (e: Exception) {
            it.onSuccess(Result.success())
        }
    }
        .timeout(1, TimeUnit.MINUTES)
        .onErrorReturn { Result.success() }


}

class UpdateWorker(context: Context, workerParams: WorkerParameters) : RxWorker(context, workerParams), KoinComponent {

    private val update by lazy { UpdateNotification(this.applicationContext) }
    private val dao by lazy { ItemDatabase.getInstance(this@UpdateWorker.applicationContext).itemDao() }

    /*override fun startWork(): ListenableFuture<Result> {
        update.sendRunningNotification(100, 0, "Starting check")
        return super.startWork()
    }*/

    private val genericInfo: GenericInfo by inject()

    override fun createWork(): Single<Result> {
        update.sendRunningNotification(100, 0, applicationContext.getString(R.string.startingCheck))
        Loged.fd("Starting check here")
        applicationContext.lastUpdateCheck = System.currentTimeMillis()
        applicationContext.lastUpdateCheck?.let { updateCheckPublish.onNext(it) }
        return Single.create<List<DbModel>> { emitter ->
            Loged.fd("Start")
            val list = listOf(
                dao.getAllFavoritesSync(),
                FirebaseDb.getAllShows().requireNoNulls()
            ).flatten().groupBy(DbModel::url).map { it.value.fastMaxBy(DbModel::numChapters)!! }
            //applicationContext.dbAndFireMangaSync3(dao)
            /*val sourceList = Sources.getUpdateSearches()
                .filter { s -> list.any { m -> m.source == s } }
                .flatMap { m -> m.getManga() }*/

            val newList = list.intersect(
                //sourcesList
                genericInfo.sourceList()
                    .filter { s -> list.fastAny { m -> m.source == s.serviceName } }
                    .mapNotNull { m ->
                        try {
                            m.getRecent()
                                .timeout(10, TimeUnit.SECONDS)
                                .onErrorReturnItem(emptyList())
                                .blockingGet()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }.flatten()
            ) { o, n -> o.url == n.url }
            emitter.onSuccess(newList.distinctBy { it.url })
        }
            .map { list ->
                Loged.fd("Map1")
                /*val loadMarkersJob: AtomicReference<Job?> = AtomicReference(null)
                fun methodReturningJob() = GlobalScope.launch {
                    println("Before Delay")
                    delay(30000)
                    println("After Delay")
                    throw Exception("Finished")
                }*/
                list.mapIndexedNotNull { index, model ->
                    update.sendRunningNotification(list.size, index, model.title)
                    try {
                        //loadMarkersJob.getAndSet(methodReturningJob())?.cancel()
                        //val newData = sourceFromString(model.source)?.let { model.toItemModel(it).toInfoModel().blockingGet() }
                        val newData = genericInfo.toSource(model.source)
                            ?.let {
                                model.toItemModel(it).toInfoModel()
                                    .timeout(30, TimeUnit.SECONDS)
                                    .blockingGet()
                            }
                        println("Old: ${model.numChapters} New: ${newData?.chapters?.size}")
                        if (model.numChapters >= newData?.chapters?.size ?: -1) null
                        else Pair(newData, model)
                    } catch (e: Exception) {
                        //e.crashlyticsLog(model.title, "manga_load_error")
                        e.printStackTrace()
                        null
                    }
                }.also {
                    try {
                        //loadMarkersJob.get()?.cancel()
                    } catch (ignored: Exception) {
                    }
                }
            }
            .map {
                update.updateManga(dao, it)
                update.mapDbModel(dao, it, genericInfo)
            }
            .map { update.onEnd(it, info = genericInfo).also { Loged.fd("Finished!") } }
            .map {
                update.sendFinishedNotification()
                Result.success()
            }
            .timeout(5, TimeUnit.MINUTES)
            .onErrorReturn {
                println(it)
                update.sendFinishedNotification()
                Result.success()
            }
    }

}

class UpdateNotification(private val context: Context) : KoinComponent {

    private val icon: NotificationLogo by inject()

    fun updateManga(dao: ItemDao, triple: List<Pair<InfoModel?, DbModel>>) {
        triple.fastForEach {
            val item = it.second
            item.numChapters = it.first?.chapters?.size ?: item.numChapters
            dao.insertFavorite(item).subscribe()
            FirebaseDb.updateShow(item).subscribe()
        }
    }

    fun mapDbModel(dao: ItemDao, list: List<Pair<InfoModel?, DbModel>>, info: GenericInfo) = list.mapIndexed { index, pair ->
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
        ).subscribe()
        pair.second.hashCode() to NotificationDslBuilder.builder(
            context,
            "otakuChannel",
            icon.notificationId
        ) {
            title = pair.second.title
            subText = pair.second.source
            getBitmapFromURL(pair.second.imageUrl)?.let {
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

    private fun getBitmapFromURL(strURL: String?): Bitmap? = try {
        val url = URL(strURL)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        BitmapFactory.decodeStream(connection.inputStream)
    } catch (e: IOException) {
        //e.printStackTrace()
        Loged.e(e.localizedMessage, showPretty = false)
        null
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
        context.lastUpdateCheckEnd = System.currentTimeMillis()
        context.lastUpdateCheckEnd?.let { updateCheckPublishEnd.onNext(it) }
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
            url?.let { dao?.getNotificationItem(it) }
                .also { println(it) }
                ?.let { dao?.deleteNotification(it)?.subscribe() }
            id?.let { if (it != -1) context?.notificationManager?.cancel(it) }
            val g = context?.notificationManager?.activeNotifications?.map { it.notification }?.filter { it.group == "otakuGroup" }.orEmpty()
            if (g.size == 1) context?.notificationManager?.cancel(42)
        }
    }
}

class BootReceived : BroadcastReceiver(), KoinComponent {

    private val logo: NotificationLogo by inject()
    private val info: GenericInfo by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        Loged.d("BootReceived")
        context?.let { SavedNotifications.viewNotificationsFromDb(it, logo, info) }
    }
}

class NotifySingleWorker(context: Context, workerParams: WorkerParameters) : RxWorker(context, workerParams), KoinComponent {

    private val logo: NotificationLogo by inject()
    private val genericInfo: GenericInfo by inject()

    override fun createWork(): Single<Result> = Single.create<Result> {
        inputData.getString("notiData")
            ?.fromJson<NotificationItem>()
            ?.let { d -> SavedNotifications.viewNotificationFromDb(applicationContext, d, logo, genericInfo) }

        it.onSuccess(Result.success())
    }
        .timeout(1, TimeUnit.MINUTES)
        .onErrorReturn { Result.success() }

}

object SavedNotifications {

    fun viewNotificationFromDb(context: Context, n: NotificationItem, notificationLogo: NotificationLogo, info: GenericInfo) {
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
                val itemModel = info.toSource(n.source)//UpdateWorker.sourceFromString(n.source)
                    ?.getSourceByUrl(n.url)
                    ?.onErrorReturn { null }
                    ?.blockingGet()

                info.deepLinkDetails(context, itemModel)

                /*NavDeepLinkBuilder(context)
                    .setDestination(
                        Screen.DetailsScreen.route,
                        Bundle().apply { putSerializable("itemInfo", itemModel) }
                    )
                    //.setGraph(R.navigation.recent_nav)
                    //.setDestination(R.id.detailsFragment2)
                    //.setArguments(Bundle().apply { putSerializable("itemInfo", itemModel) })
                    .createPendingIntent()*/
            }
        })
            .let { update.onEnd(listOf(it), info = info) }
    }

    fun viewNotificationsFromDb(context: Context, logo: NotificationLogo, info: GenericInfo) {
        val dao by lazy { ItemDatabase.getInstance(context).itemDao() }
        val icon = logo.notificationId
        val update = UpdateNotification(context)
        GlobalScope.launch {
            dao.getAllNotifications()
                .blockingGet()
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
                            val itemModel = info.toSource(n.source)//UpdateWorker.sourceFromString(n.source)
                                ?.getSourceByUrl(n.url)
                                ?.onErrorReturn { null }
                                ?.blockingGet()

                            info.deepLinkDetails(context, itemModel)
                        }
                    }
                }
                .let { update.onEnd(it, info = info) }
        }
    }

    private fun getBitmapFromURL(strURL: String?): Bitmap? = try {
        val url = URL(strURL)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        BitmapFactory.decodeStream(connection.inputStream)
    } catch (e: IOException) {
        null
    }
}