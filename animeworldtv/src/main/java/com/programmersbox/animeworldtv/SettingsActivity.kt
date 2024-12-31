package com.programmersbox.animeworldtv

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.DialogPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.anime_sources.Sources
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.CustomFirebaseUser
import com.programmersbox.sharedutils.FirebaseAuthentication
import com.programmersbox.sharedutils.updateAppCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class SettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_fragment)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_fragment, SettingsFragment())
                .commitNow()
        }
    }
}

class SettingsFragment : LeanbackSettingsFragmentCompat(), DialogPreference.TargetFragment {
    private var mPreferenceFragment: PreferenceFragmentCompat? = null
    override fun onPreferenceStartInitialScreen() {
        mPreferenceFragment = buildPreferenceFragment(R.xml.leanback_preferences, null)
        startPreferenceFragment(mPreferenceFragment!!)
    }

    override fun onPreferenceStartFragment(
        preferenceFragment: PreferenceFragmentCompat,
        preference: Preference
    ): Boolean {
        return false
    }

    override fun <T : Preference?> findPreference(key: CharSequence): T? =
        mPreferenceFragment?.findPreference(key)

    override fun onPreferenceStartScreen(
        preferenceFragment: PreferenceFragmentCompat,
        preferenceScreen: PreferenceScreen
    ): Boolean {
        startPreferenceFragment(buildPreferenceFragment(R.xml.leanback_preferences, preferenceScreen.key))
        return true
    }

    private fun buildPreferenceFragment(preferenceResId: Int, root: String?): LeanbackPreferenceFragmentCompat {
        val fragment = PrefFragment()
        val args = Bundle()
        args.putInt(PREFERENCE_RESOURCE_ID, preferenceResId)
        args.putString(PREFERENCE_ROOT, root)
        fragment.arguments = args
        return fragment
    }

    class PrefFragment : LeanbackPreferenceFragmentCompat() {

        override fun onCreatePreferences(bundle: Bundle?, s: String?) {
            //setPreferencesFromResource(R.xml.leanback_preferences, s)
            val root = arguments?.getString(PREFERENCE_ROOT, null)
            val prefResId = arguments?.getInt(PREFERENCE_RESOURCE_ID)!!
            if (root == null) {
                addPreferencesFromResource(prefResId)
            } else {
                setPreferencesFromResource(prefResId, root)
            }

            findPreference<Preference>("user_account")?.let { p ->

                fun accountChanges(user: CustomFirebaseUser?) {
                    /*activity?.let {
                        Glide.with(this@PrefFragment)
                            .load(user?.photoUrl ?: R.mipmap.ic_launcher)
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                            .fallback(R.mipmap.ic_launcher)
                            .circleCrop()
                            .into<Drawable> { resourceReady { image, _ -> p.icon = image } }
                    }*/
                    p.title = user?.displayName ?: "User"
                }

                FirebaseAuthentication.addAuthStateListener { accountChanges(it) }
            }

            findPreference<PreferenceScreen>("prefs_about")?.let { p ->
                lifecycleScope.launch {
                    updateAppCheck
                        .filterNotNull()
                        .flowOn(Dispatchers.Main)
                        .onEach {
                            val appVersion = context?.packageManager?.getPackageInfo(requireContext().packageName, 0)?.versionName.orEmpty()
                            p.summary = if (AppUpdate.checkForUpdate(appVersion, it.updateRealVersion.orEmpty()))
                                getString(R.string.updateVersionAvailable, it.updateRealVersion.orEmpty())
                            else ""
                        }
                        .collect()
                }
            }

            findPreference<Preference>("about_version")?.let { p ->
                p.title = getString(
                    R.string.currentVersion,
                    context?.packageManager?.getPackageInfo(requireContext().packageName, 0)?.versionName.orEmpty()
                )

                p.setOnPreferenceClickListener {
                    updateSetter()
                    true
                }

            }

            findPreference<Preference>("updateAvailable")?.let { p ->
                p.isVisible = false
                lifecycleScope.launch {
                    updateAppCheck
                        .filterNotNull()
                        .flowOn(Dispatchers.Main)
                        .onEach {
                            p.summary = getString(R.string.currentVersion, it.updateRealVersion.orEmpty())
                            val appVersion = context?.packageManager?.getPackageInfo(requireContext().packageName, 0)?.versionName.orEmpty()
                            p.isVisible = AppUpdate.checkForUpdate(appVersion, it.updateRealVersion.orEmpty())
                        }
                        .collect()
                }
            }

            findPreference<Preference>("current_source")?.let { p ->

                p.setOnPreferenceClickListener {
                    val list = Sources.values().filterNot(Sources::notWorking)
                    val service = requireContext().currentService
                    MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MaterialComponents)
                        .setTitle(R.string.chooseASource)
                        .setSingleChoiceItems(
                            list.map { it.serviceName }.toTypedArray(),
                            list.indexOfFirst { it.serviceName == service }
                        ) { d, i ->
                            //sourceFlow.tryEmit(list[i])
                            requireContext().currentService = list[i].serviceName
                            d.dismiss()
                        }
                        .setPositiveButton(R.string.done) { d, _ -> d.dismiss() }
                        .show()
                    true
                }
                lifecycleScope.launch {
                    /*sourceFlow
                        .filterNotNull()
                        .flowOn(Dispatchers.Main)
                        .onEach { p.title = getString(R.string.currentSource, it.serviceName) }
                        .collect()*/
                }
            }
        }

        private val checker = AtomicBoolean(false)

        private fun updateSetter() {
            if (!checker.get()) {
                lifecycleScope.launch {
                    flow {
                        checker.set(true)
                        emit(AppUpdate.getUpdate())
                    }
                        .flowOn(Dispatchers.IO)
                        .onEach {
                            updateAppCheck.emit(it)
                            checker.set(false)
                            context?.let { c -> Toast.makeText(c, "Done Checking", Toast.LENGTH_SHORT).show() }
                        }
                        .collect()
                }
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            if (preference.key.equals("user_account")) {
                // Open an AuthenticationActivity
                //startActivity(Intent(activity, AuthenticationActivity::class.java))
                FirebaseAuthentication.signInOrOut(
                    this@PrefFragment.requireContext(),
                    requireActivity(),
                    R.string.browse_title,
                    R.string.browse_title,
                    R.string.browse_title,
                    R.string.browse_title
                )
                /*FirebaseAuthentication.currentUser?.let {
                    MaterialAlertDialogBuilder(this@PrefFragment.requireContext(), R.style.Theme_MaterialComponents)
                        .setTitle("Log Out")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Yes") { d, _ ->
                            FirebaseAuthentication.signOut()
                            d.dismiss()
                        }
                        .setNegativeButton("No") { d, _ -> d.dismiss() }
                        .show()
                } ?: FirebaseAuthentication.signIn(requireActivity())*/
            }
            return super.onPreferenceTreeClick(preference)
        }
    }

    companion object {
        private const val PREFERENCE_RESOURCE_ID = "preferenceResource"
        private const val PREFERENCE_ROOT = "root"
    }
}