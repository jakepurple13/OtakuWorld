package com.programmersbox.cloudstream.preferences

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.programmersbox.cloudstream.ConfigurableCloudstreamApiService
import com.programmersbox.kmpmodels.SourceRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Transparent AppCompat host for a plugin's `openSettings`. Plugins show AppCompat/Material
 * dialogs and fragments, which the app's FragmentActivity cannot host. Finishes once nothing the
 * plugin showed is left on screen.
 */
class CloudstreamSettingsActivity : AppCompatActivity(), KoinComponent {
    private val sourceRepository: SourceRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Plugin fragments live in the plugin's class loader and cannot be restored; start over.
        super.onCreate(null)
        val serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME)
        val service = sourceRepository.list
            .firstOrNull { it.apiService.serviceName == serviceName }
            ?.apiService as? ConfigurableCloudstreamApiService
        if (service == null) {
            finish()
            return
        }
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) = finishIfDone()
            },
            false,
        )
        try {
            service.openSettings(this)
        } catch (t: Throwable) {
            t.printStackTrace()
            Toast.makeText(this, "Could not open settings: ${t.message ?: t}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // Focus returns when the plugin's dialog closes, or when the user comes back from an
    // activity the plugin started. A plugin that showed nothing leaves focus here at once.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) finishIfDone()
    }

    private fun finishIfDone() {
        if (!isFinishing && hasWindowFocus() && supportFragmentManager.fragments.isEmpty()) finish()
    }

    companion object {
        const val EXTRA_SERVICE_NAME = "serviceName"

        fun intent(context: Context, serviceName: String): Intent =
            Intent(context, CloudstreamSettingsActivity::class.java).putExtra(EXTRA_SERVICE_NAME, serviceName)
    }
}
