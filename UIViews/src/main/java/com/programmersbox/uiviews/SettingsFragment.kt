package com.programmersbox.uiviews

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.helpfulutils.setEnumSingleChoiceItems
import com.programmersbox.models.sourcePublish
import com.programmersbox.uiviews.utils.currentService
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo

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

    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}