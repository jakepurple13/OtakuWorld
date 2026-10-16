package com.programmersbox.aniyomi.preferences

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.programmersbox.aniyomi.AniyomiApiService
import com.programmersbox.kmpmodels.SourceRepository
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.preferenceKey
import org.koin.java.KoinJavaComponent.get

/** Renders an Aniyomi ConfigurableAnimeSource's preferences, stored where the extension reads them. */
class AniyomiPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Host theme may lack preferenceTheme; force=false only fills attrs that are missing.
        requireContext().theme.applyStyle(androidx.preference.R.style.PreferenceThemeOverlay, false)
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val serviceName = requireArguments().getString(ARG_SERVICE_NAME).orEmpty()
        val source = (get<SourceRepository>(SourceRepository::class.java)
            .toSourceByApiServiceName(serviceName)
            ?.apiService as? AniyomiApiService)
            ?.source as? ConfigurableAnimeSource
            ?: return

        preferenceManager.sharedPreferencesName = source.preferenceKey()
        val screen = preferenceManager.createPreferenceScreen(requireContext())
        source.setupPreferenceScreen(screen)
        preferenceScreen = screen
    }

    companion object {
        const val ARG_SERVICE_NAME = "serviceName"
    }
}
