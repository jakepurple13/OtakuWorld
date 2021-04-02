package com.programmersbox.uiviews

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.utils.batteryAlertPercent
import com.programmersbox.uiviews.utils.currentService
import com.programmersbox.uiviews.utils.lastUpdateCheck
import com.programmersbox.uiviews.utils.updateCheckPublish
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : PreferenceFragmentCompat() {

    private val disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val genericInfo = BaseMainActivity.genericInfo

        generalPreferences(genericInfo)

        genericInfo.customPreferences(preferenceScreen)
    }

    private fun generalPreferences(genericInfo: GenericInfo) {

        findPreference<Preference>("current_source")?.let { p ->
            val list = genericInfo.sourceList().toTypedArray()
            p.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Choose a source")
                    .setSingleChoiceItems(
                        list.map { it.serviceName }.toTypedArray(),
                        list.indexOf(requireContext().currentService)
                    ) { d, i ->
                        sourcePublish.onNext(list[i])
                        requireContext().currentService = list[i]
                    }
                    .setPositiveButton("Done") { d, _ -> d.dismiss() }
                    .show()
                true
            }
            sourcePublish.subscribe { p.title = "Current Source: ${it.serviceName}" }
                .addTo(disposable)
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

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}