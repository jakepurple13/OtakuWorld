package com.programmersbox.uiviews

import android.Manifest
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
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
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

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

        findPreference<Preference>("debugMenu")?.let { p ->
            p.isVisible = BuildConfig.DEBUG
            p.setOnPreferenceClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToDebugFragment())
                true
            }
        }
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
                                        DownloadUpdate(requireContext(), requireContext().packageName).downloadUpdate(a)
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

            listOfNotNull(
                requireContext().lastUpdateCheck
                    ?.let { "Start: ${requireContext().getSystemDateTimeFormat().format(it)}" },
                requireContext().lastUpdateCheckEnd
                    ?.let { "End: ${requireContext().getSystemDateTimeFormat().format(it)}" }
            )
                .joinToString("\n")
                .let { s.summary = it }

            Observables.combineLatest(
                updateCheckPublish.map { "Start: ${requireContext().getSystemDateTimeFormat().format(it)}" },
                updateCheckPublishEnd.map { "End: ${requireContext().getSystemDateTimeFormat().format(it)}" }
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