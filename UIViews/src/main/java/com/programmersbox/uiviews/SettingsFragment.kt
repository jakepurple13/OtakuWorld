package com.programmersbox.uiviews

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabsIntent
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseUser
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.models.sourcePublish
import com.programmersbox.thirdpartyutils.into
import com.programmersbox.thirdpartyutils.openInCustomChromeBrowser
import com.programmersbox.uiviews.utils.*
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class SettingsFragment : PreferenceFragmentCompat() {

    private val disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val genericInfo = BaseMainActivity.genericInfo

        accountPreferences()
        generalPreferences(genericInfo)
        aboutPreferences()

        val settingsDsl = SettingsDsl()

        genericInfo.customPreferences(settingsDsl)

        findPreference<PreferenceCategory>("generalCategory")?.let { settingsDsl.generalSettings(it) }
        findPreference<PreferenceCategory>("viewCategory")?.let { settingsDsl.viewSettings(it) }
    }

    private fun accountPreferences() {
        findPreference<Preference>("user_account")?.let { p ->

            fun accountChanges(user: FirebaseUser?) {
                Glide.with(this@SettingsFragment)
                    .load(user?.photoUrl)
                    .placeholder(OtakuApp.logo)
                    .error(OtakuApp.logo)
                    .fallback(OtakuApp.logo)
                    .circleCrop()
                    .into<Drawable> { resourceReady { image, _ -> p.icon = image } }
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
                        .setTitle("Log Out?")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes") { d, _ ->
                            FirebaseAuthentication.signOut()
                            d.dismiss()
                        }
                        .setNegativeButton("No") { d, _ -> d.dismiss() }
                        .show()
                } ?: FirebaseAuthentication.signIn(requireActivity())
                true
            }
        }
    }

    private fun generalPreferences(genericInfo: GenericInfo) {

        findPreference<PreferenceCategory>("aboutCategory")?.setIcon(OtakuApp.logo)

        findPreference<Preference>("current_source")?.let { p ->
            val list = genericInfo.sourceList().toTypedArray()
            p.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Choose a source")
                    .setSingleChoiceItems(
                        list.map { it.serviceName }.toTypedArray(),
                        list.indexOfFirst { it.serviceName == requireContext().currentService?.serviceName }
                    ) { d, i ->
                        sourcePublish.onNext(list[i])
                        requireContext().currentService = list[i]
                        d.dismiss()
                    }
                    .setPositiveButton("Done") { d, _ -> d.dismiss() }
                    .show()
                true
            }
            sourcePublish.subscribe { p.title = "Current Source: ${it.serviceName}" }
                .addTo(disposable)
        }

        findPreference<Preference>("view_source")?.let { p ->
            p.setOnPreferenceClickListener {
                requireContext().openInCustomChromeBrowser(sourcePublish.value!!.baseUrl) {
                    setStartAnimations(requireContext(), R.anim.fui_slide_in_right, R.anim.fui_slide_out_left)
                    setShareState(CustomTabsIntent.SHARE_STATE_ON)
                }
                true
            }
        }

        findPreference<Preference>("view_favorites")?.setOnPreferenceClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToFavoriteFragment())
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
        }

        findPreference<SeekBarPreference>("battery_alert")?.let { s ->
            s.showSeekBarValue = true
            s.setDefaultValue(requireContext().batteryAlertPercent)
            s.value = requireContext().batteryAlertPercent
            s.max = 100
            s.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Int) {
                    requireContext().batteryAlertPercent = newValue
                }
                true
            }
        }

    }

    private fun aboutPreferences() {
        val checker = AtomicBoolean(false)
        fun updateSetter() {
            if (!checker.get()) {
                GlobalScope.launch {
                    checker.set(true)
                    val request = Request.Builder()
                        .url("https://github.com/jakepurple13/OtakuWorld/releases/latest")
                        .get()
                        .build()
                    @Suppress("BlockingMethodInNonBlockingContext") val response = OkHttpClient().newCall(request).execute()
                    val f = response.request().url().path.split("/").lastOrNull()?.toDoubleOrNull()
                    runOnUIThread {
                        findPreference<Preference>("updateAvailable")?.let { p1 ->
                            p1.summary = "Version: $f"
                            p1.isVisible =
                                context?.packageManager?.getPackageInfo(
                                    requireContext().packageName,
                                    0
                                )?.versionName?.toDoubleOrNull() ?: 0.0 < f ?: 0.0
                        }
                    }
                    checker.set(false)
                }
            }
        }

        findPreference<Preference>("about_version")?.let { p ->
            p.title = "Version: ${context?.packageManager?.getPackageInfo(requireContext().packageName, 0)?.versionName}"
            p.summary = "Press to Check for Updates"
            p.setOnPreferenceClickListener {
                updateSetter()
                true
            }
        }

        findPreference<Preference>("updateAvailable")?.let { p ->
            p.isVisible = false
            updateSetter()
            p.setOnPreferenceClickListener {
                requireContext().openInCustomChromeBrowser("https://github.com/jakepurple13/OtakuWorld/releases/latest")
                true
            }
        }

        findPreference<Preference>("sync_time")?.let { s ->
            requireContext().lastUpdateCheck
                ?.let { SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault()).format(it) }
                ?.let { s.summary = it }

            updateCheckPublish
                .map { SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault()).format(it) }
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

    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}

class SettingsDsl {
    internal var generalSettings: (PreferenceCategory) -> Unit = {}

    fun generalSettings(block: (PreferenceCategory) -> Unit) {
        generalSettings = block
    }

    internal var viewSettings: (PreferenceCategory) -> Unit = {}

    fun viewSettings(block: (PreferenceCategory) -> Unit) {
        viewSettings = block
    }
}