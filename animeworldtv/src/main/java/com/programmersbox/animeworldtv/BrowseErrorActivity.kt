package com.programmersbox.animeworldtv

import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.DialogPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen

/**
 * BrowseErrorActivity shows how to use ErrorFragment.
 */
class BrowseErrorActivity : FragmentActivity() {

    private lateinit var mErrorFragment: ErrorFragment
    private lateinit var mSpinnerFragment: SpinnerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()
        }
        testError()
    }

    private fun testError() {
        mErrorFragment = ErrorFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.main_browse_fragment, mErrorFragment)
            .commit()

        mSpinnerFragment = SpinnerFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.main_browse_fragment, mSpinnerFragment)
            .commit()

        val handler = Handler()
        handler.postDelayed({
            supportFragmentManager
                .beginTransaction()
                .remove(mSpinnerFragment)
                .commit()
            mErrorFragment.setErrorContent()
        }, TIMER_DELAY)
    }

    class SpinnerFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val progressBar = ProgressBar(container?.context)
            if (container is FrameLayout) {
                val layoutParams = FrameLayout.LayoutParams(SPINNER_WIDTH, SPINNER_HEIGHT, Gravity.CENTER)
                progressBar.layoutParams = layoutParams
            }
            return progressBar
        }
    }

    companion object {
        private val TIMER_DELAY = 3000L
        private val SPINNER_WIDTH = 100
        private val SPINNER_HEIGHT = 100
    }
}

class SettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_fragment)
        /*if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commitNow()
        }*/
    }
}

class SettingsFragment : LeanbackSettingsFragmentCompat(), DialogPreference.TargetFragment {
    private var mPreferenceFragment: PreferenceFragmentCompat? = null
    override fun onPreferenceStartInitialScreen() {
        mPreferenceFragment = buildPreferenceFragment(R.xml.leanback_preferences, null)
        startPreferenceFragment(mPreferenceFragment!!)
    }

    override fun onPreferenceStartFragment(
        preferenceFragment: PreferenceFragmentCompat?,
        preference: Preference?
    ): Boolean {
        return false
    }

    override fun <T : Preference?> findPreference(key: CharSequence): T? =
        mPreferenceFragment?.findPreference(key)

    override fun onPreferenceStartScreen(
        preferenceFragment: PreferenceFragmentCompat?,
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
            setPreferencesFromResource(R.xml.leanback_preferences, s)
            /*val root = arguments?.getString(PREFERENCE_ROOT, null)
            val prefResId = arguments?.getInt(PREFERENCE_RESOURCE_ID)!!
            if (root == null) {
                addPreferencesFromResource(prefResId)
            } else {
                setPreferencesFromResource(prefResId, root)
            }*/
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            /*if (preference.key.equals(getString(R.string.pref_key_login))) {
                // Open an AuthenticationActivity
                startActivity(Intent(activity, AuthenticationActivity::class.java))
            }*/
            return super.onPreferenceTreeClick(preference)
        }
    }

    companion object {
        private const val PREFERENCE_RESOURCE_ID = "preferenceResource"
        private const val PREFERENCE_ROOT = "root"
    }
}