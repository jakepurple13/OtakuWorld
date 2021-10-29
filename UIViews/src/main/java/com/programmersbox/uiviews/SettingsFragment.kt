package com.programmersbox.uiviews

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.preference.*
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseUser
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.loggingutils.Loged
import com.programmersbox.models.sourcePublish
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.FirebaseAuthentication
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.sharedutils.appUpdateCheck
import com.programmersbox.uiviews.utils.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class SettingsFragment : PreferenceFragmentCompat() {

    private val disposable: CompositeDisposable = CompositeDisposable()

    private val genericInfo: GenericInfo by inject()

    private val logo: MainLogo by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        accountPreferences()
        generalPreferences(genericInfo)
        aboutPreferences(genericInfo)
        playPreferences()

        val settingsDsl = SettingsDsl()

        genericInfo.customPreferences(settingsDsl)
        settingsDsl.navigationSetup(this)
        findPreference<PreferenceCategory>("generalCategory")?.let { settingsDsl.generalSettings(this, it) }
        findPreference<PreferenceCategory>("viewCategory")?.let { settingsDsl.viewSettings(this, it) }
        findPreference<PreferenceCategory>("playCategory")?.let { settingsDsl.playSettings(this, it) }
    }

    private fun accountPreferences() {
        findPreference<Preference>("user_account")?.let { p ->

            fun accountChanges(user: FirebaseUser?) {
                activity?.let {
                    Glide.with(this@SettingsFragment)
                        .load(user?.photoUrl)
                        .placeholder(logo.logoId)
                        .error(logo.logoId)
                        .fallback(logo.logoId)
                        .circleCrop()
                        .into<Drawable> { resourceReady { image, _ -> p.icon = image } }
                }
                p.title = user?.displayName ?: "User"
            }

            FirebaseAuthentication.auth.addAuthStateListener {
                accountChanges(it.currentUser)
                //findPreference<Preference>("upload_favorites")?.isEnabled = it.currentUser != null
                //findPreference<Preference>("upload_favorites")?.isVisible = it.currentUser != null
            }

            accountChanges(FirebaseAuthentication.currentUser)

            p.setOnPreferenceClickListener {
                FirebaseAuthentication.currentUser?.let {
                    MaterialAlertDialogBuilder(this@SettingsFragment.requireContext())
                        .setTitle(R.string.logOut)
                        .setMessage(R.string.areYouSureLogOut)
                        .setPositiveButton(R.string.yes) { d, _ ->
                            FirebaseAuthentication.signOut()
                            d.dismiss()
                        }
                        .setNegativeButton(R.string.no) { d, _ -> d.dismiss() }
                        .show()
                } ?: FirebaseAuthentication.signIn(requireActivity())
                true
            }
        }
    }

    private fun generalPreferences(genericInfo: GenericInfo) {

        findPreference<PreferenceCategory>("aboutCategory")?.setIcon(logo.logoId)

        findPreference<Preference>("current_source")?.let { p ->
            p.setOnPreferenceClickListener {
                val list = genericInfo.sourceList().toTypedArray()
                val service = requireContext().currentService
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.chooseASource)
                    .setSingleChoiceItems(
                        list.map { it.serviceName }.toTypedArray(),
                        list.indexOfFirst { it.serviceName == service }
                    ) { d, i ->
                        sourcePublish.onNext(list[i])
                        requireContext().currentService = list[i].serviceName
                        d.dismiss()
                    }
                    .setPositiveButton(R.string.done) { d, _ -> d.dismiss() }
                    .show()
                true
            }
            sourcePublish.subscribe { p.title = getString(R.string.currentSource, it.serviceName) }
                .addTo(disposable)
        }

        findPreference<Preference>("view_source")?.let { p ->
            p.setOnPreferenceClickListener {
                requireContext().openInCustomChromeBrowser(sourcePublish.value!!.baseUrl) {
                    setStartAnimations(requireContext(), R.anim.slide_in_right, R.anim.slide_out_left)
                }
                true
            }
        }

        findPreference<Preference>("view_favorites")?.setOnPreferenceClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToFavoriteFragment())
            true
        }

        findPreference<Preference>("view_history")?.setOnPreferenceClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToHistoryFragment())
            true
        }

        findPreference<Preference>("view_global_search")?.setOnPreferenceClickListener {
            findNavController().navigate(GlobalNavDirections.showGlobalSearch())
            true
        }

        findPreference<ListPreference>("theme_setting")?.let { p ->
            p.setDefaultValue("system")
            p.setOnPreferenceChangeListener { _, newValue ->
                when (newValue) {
                    "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> null
                }?.let(AppCompatDelegate::setDefaultNightMode)
                true
            }

            when (p.value) {
                "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> null
            }?.let(AppCompatDelegate::setDefaultNightMode)
        }

        val itemDao = ItemDatabase.getInstance(requireContext()).itemDao()

        findPreference<PreferenceCategory>("notification_category")?.let { p ->
            itemDao.getAllNotificationCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { p.isVisible = it > 0 }
                .addTo(disposable)
        }

        findPreference<Preference>("saved_notifications")?.let { p ->
            itemDao.getAllNotificationCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { p.summary = getString(R.string.pending_saved_notifications, it) }
                .addTo(disposable)

            p.setOnPreferenceClickListener {
                //SavedNotifications.viewNotificationsFromDb(requireContext())
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToNotificationFragment())
                true
            }
        }

        findPreference<Preference>("delete_notifications")?.let { p ->
            p.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.are_you_sure_delete_notifications)
                    .setPositiveButton(R.string.yes) { d, _ ->
                        itemDao
                            .deleteAllNotifications()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .map { getString(R.string.deleted_notifications, it) }
                            .subscribeBy {
                                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                                requireContext().notificationManager.cancel(42)
                            }
                            .addTo(disposable)
                        d.dismiss()
                    }
                    .setNegativeButton(R.string.no) { d, _ -> d.dismiss() }
                    .show()
                true
            }
        }

    }

    private fun playPreferences() {
        findPreference<SeekBarPreference>("battery_alert")?.let { s ->
            s.showSeekBarValue = true
            s.setDefaultValue(20)
            s.max = 100
            s.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Int) {
                    lifecycleScope.launch(Dispatchers.IO) { requireContext().updatePref(BATTERY_PERCENT, newValue) }
                }
                true
            }
            lifecycleScope.launch {
                requireContext().batteryPercent
                    .flowWithLifecycle(lifecycle)
                    .collect { runOnUIThread { s.value = it } }
            }
        }
    }

    private fun aboutPreferences(genericInfo: GenericInfo) {
        val checker = AtomicBoolean(false)
        fun updateSetter() {
            if (!checker.get()) {
                Single.create<AppUpdate.AppUpdates> {
                    checker.set(true)
                    AppUpdate.getUpdate()?.let { d -> it.onSuccess(d) } ?: it.onError(Exception("Something went wrong"))
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError { }
                    .subscribeBy {
                        appUpdateCheck.onNext(it)
                        checker.set(false)
                        context?.let { c -> Toast.makeText(c, "Done Checking", Toast.LENGTH_SHORT).show() }
                    }
                    .addTo(disposable)
            }
        }

        findPreference<Preference>("about_version")?.let { p ->
            p.title =
                getString(R.string.currentVersion, context?.packageManager?.getPackageInfo(requireContext().packageName, 0)?.versionName.orEmpty())
            p.setOnPreferenceClickListener {
                updateSetter()
                true
            }
        }

        findPreference<Preference>("reset_checker")?.let { p ->
            p.setOnPreferenceClickListener {
                val work = WorkManager.getInstance(this.requireContext())
                work.cancelUniqueWork("updateChecks")
                work.pruneWork()
                OtakuApp.updateSetup(requireContext())
                Toast.makeText(requireContext(), R.string.cleared, Toast.LENGTH_SHORT).show()
                true
            }
        }

        findPreference<Preference>("view_on_github")?.setOnPreferenceClickListener {
            context?.openInCustomChromeBrowser(otakuWorldGithubUrl)
            true
        }

        findPreference<Preference>("updateAvailable")?.let { p ->
            p.isVisible = false
            appUpdateCheck
                .subscribe {
                    p.summary = getString(R.string.currentVersion, it.update_real_version.orEmpty())
                    val appVersion = AppUpdate.checkForUpdate(
                        context?.packageManager?.getPackageInfo(requireContext().packageName, 0)?.versionName.orEmpty(),
                        it.update_real_version.orEmpty()
                    )
                    p.isVisible = appVersion
                }
                .addTo(disposable)

            p.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.updateTo, p.summary))
                    .setMessage(R.string.please_update_for_leatest_features)
                    .setPositiveButton(R.string.update) { d, _ ->
                        activity?.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE) {
                            if (it.isGranted) {
                                appUpdateCheck.value
                                    ?.let { a ->
                                        val isApkAlreadyThere = File(
                                            context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/",
                                            a.let(genericInfo.apkString).toString()
                                        )
                                        if (isApkAlreadyThere.exists()) isApkAlreadyThere.delete()
                                        DownloadApk(
                                            requireContext(),
                                            a.downloadUrl(genericInfo.apkString),
                                            a.let(genericInfo.apkString).toString()
                                        ).startDownloadingApk()
                                    }
                            }
                        }
                        d.dismiss()
                    }
                    .setNeutralButton(R.string.gotoBrowser) { d, _ ->
                        context?.openInCustomChromeBrowser("https://github.com/jakepurple13/OtakuWorld/releases/latest")
                        d.dismiss()
                    }
                    .setNegativeButton(R.string.notNow) { d, _ -> d.dismiss() }
                    .show()

                true
            }
        }

        findPreference<Preference>("sync_time")?.let { s ->

            val format = SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.getDefault())

            listOfNotNull(
                requireContext().lastUpdateCheck
                    ?.let { "Start: ${format.format(it)}" },
                requireContext().lastUpdateCheckEnd
                    ?.let { "End: ${format.format(it)}" }
            )
                .joinToString("\n")
                .let { s.summary = it }

            Observables.combineLatest(
                updateCheckPublish.map { "Start: ${format.format(it)}" },
                updateCheckPublishEnd.map { "End: ${format.format(it)}" }
            )
                .map { "${it.first}\n${it.second}" }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { s.summary = it }
                .addTo(disposable)

            s.setOnPreferenceClickListener {
                WorkManager.getInstance(this.requireContext())
                    .enqueueUniqueWork(
                        "oneTimeUpdate",
                        ExistingWorkPolicy.KEEP,
                        OneTimeWorkRequestBuilder<UpdateWorker>()
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                    .setRequiresBatteryNotLow(false)
                                    .setRequiresCharging(false)
                                    .setRequiresDeviceIdle(false)
                                    .setRequiresStorageNotLow(false)
                                    .build()
                            )
                            .build()
                    )
                true
            }
        }

        findPreference<SwitchPreferenceCompat>("sync")?.let { s ->
            s.setDefaultValue(requireContext().shouldCheck)
            s.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Boolean) {
                    requireContext().shouldCheck = newValue
                    OtakuApp.updateSetup(requireContext())
                }
                true
            }
        }

        findPreference<Preference>("used_libraries")?.setOnPreferenceClickListener {
            findNavController()
                .navigate(
                    SettingsFragmentDirections.actionXToAboutLibs(
                        LibsBuilder()
                            .withSortEnabled(true)
                            .customUtils("loggingutils", "LoggingUtils")
                            //.customUtils("flowutils", "FlowUtils")
                            .customUtils("gsonutils", "GsonUtils")
                            .customUtils("helpfulutils", "HelpfulUtils")
                            .customUtils("dragswipe", "DragSwipe")
                            .customUtils("funutils", "FunUtils")
                            .customUtils("rxutils", "RxUtils")
                            //.customUtils("thirdpartyutils", "ThirdPartyUtils")
                            .withShowLoadingProgress(true)
                    )
                )
            true
        }

    }

    private fun LibsBuilder.customUtils(libraryName: String, newName: String) =
        withLibraryModification(
            libraryName,
            Libs.LibraryFields.LIBRARY_REPOSITORY_LINK,
            "https://www.github.com/jakepurple13/HelpfulTools"
        )
            .withLibraryModification(
                libraryName,
                Libs.LibraryFields.LIBRARY_NAME,
                newName
            )

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}

class SettingsDsl {
    internal var generalSettings: (SettingsFragment, PreferenceCategory) -> Unit = { _, _ -> }

    fun generalSettings(block: (SettingsFragment, PreferenceCategory) -> Unit) {
        generalSettings = block
    }

    internal var viewSettings: (SettingsFragment, PreferenceCategory) -> Unit = { _, _ -> }

    fun viewSettings(block: (SettingsFragment, PreferenceCategory) -> Unit) {
        viewSettings = block
    }

    internal var navigationSetup: (SettingsFragment) -> Unit = {}

    fun navigationSetup(block: (SettingsFragment) -> Unit) {
        navigationSetup = block
    }

    internal var playSettings: (SettingsFragment, PreferenceCategory) -> Unit = { _, _ -> }

    fun playSettings(block: (SettingsFragment, PreferenceCategory) -> Unit) {
        playSettings = block
    }

    companion object {
        val customAnimationOptions = navOptions {
            anim {
                enter = R.anim.slide_in
                exit = R.anim.fade_out
                popEnter = R.anim.fade_in
                popExit = R.anim.slide_out
            }
        }
    }
}

class DownloadApk(val context: Context, private val downloadUrl: String, private val outputName: String) : CoroutineScope, KoinComponent {
    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job // to run code in Main(UI) Thread

    private val logo: MainLogo by inject()

    // call this method to cancel a coroutine when you don't need it anymore,
    // e.g. when user closes the screen
    fun cancel() {
        job.cancel()
    }

    fun startDownloadingApk() {
        if (URLUtil.isValidUrl(downloadUrl)) execute()
    }

    private lateinit var bar: AlertDialog

    private fun execute() = launch {
        onPreExecute()
        val result = doInBackground() // runs in background thread without blocking the Main Thread
        onPostExecute(result)
    }

    private suspend fun onProgressUpdate(vararg values: Int?) = withContext(Dispatchers.Main) {
        values[0]?.let {
            bar.setMessage(if (it > 99) context.getString(R.string.finishing_dots) else context.getString(R.string.downloading_dots, it))
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun doInBackground(): Boolean = withContext(Dispatchers.IO) { // to run code in Background Thread
        // do async work
        var flag = false

        try {
            val url = URL(downloadUrl)
            val c = url.openConnection() as HttpURLConnection
            c.requestMethod = "GET"
            c.connect()
            val path = Environment.getExternalStorageDirectory().toString() + "/Download/"
            val file = File(path)
            file.mkdirs()
            val outputFile = File(file, outputName)

            if (outputFile.exists()) outputFile.delete()

            val fos = FileOutputStream(outputFile)
            val inputStream = c.inputStream
            val totalSize = c.contentLength.toFloat() //size of apk

            val buffer = ByteArray(1024)
            var len1: Int
            var downloaded = 0f
            while (inputStream.read(buffer).also { len1 = it } != -1) {
                fos.write(buffer, 0, len1)
                downloaded += len1
                onProgressUpdate((downloaded * 100 / totalSize).toInt())
            }
            fos.close()
            inputStream.close()
            openNewVersion(path)
            flag = true
        } catch (e: MalformedURLException) {
            Loged.e("Update Error: " + e.message, "DownloadApk")
            flag = false
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext flag
    }

    // Runs on the Main(UI) Thread
    private fun onPreExecute() {
        // show progress
        bar = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.updating_dots)
            .setMessage(R.string.downloading_dots_no_percent)
            .setCancelable(false)
            .setIcon(logo.logoId)
            .show()
    }

    // Runs on the Main(UI) Thread
    private fun onPostExecute(result: Boolean?) {
        // hide progress
        bar.dismiss()
        Toast.makeText(context, if (result == true) R.string.finishedDownloading else R.string.errorTryAgain, Toast.LENGTH_SHORT).show()
    }

    private fun openNewVersion(location: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(getUriFromFile(location), "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    private fun getUriFromFile(location: String): Uri {
        return if (Build.VERSION.SDK_INT < 24) {
            Uri.fromFile(File(location + outputName))
        } else {
            FileProvider.getUriForFile(context, context.packageName + ".provider", File(location + outputName))
        }
    }
}