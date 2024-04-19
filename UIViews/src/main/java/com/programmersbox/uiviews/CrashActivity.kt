package com.programmersbox.uiviews

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.system.exitProcess

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val exception = GlobalExceptionHandler.getThrowableFromIntent(intent)
        exception?.printStackTrace()
        exception?.let { Firebase.crashlytics.recordException(it) }
        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
                darkTheme -> darkColorScheme(
                    primary = Color(0xff90CAF9),
                    secondary = Color(0xff90CAF9)
                )

                else -> lightColorScheme(
                    primary = Color(0xff2196F3),
                    secondary = Color(0xff90CAF9)
                )
            }
            MaterialTheme(
                colorScheme = colorScheme
            ) {
                CrashScreen(
                    exception = exception,
                    onRestartClick = {
                        finishAffinity()
                        startActivity(packageManager.getLaunchIntentForPackage(packageName))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrashScreen(
    exception: Throwable?,
    onRestartClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("You Crashed") },
                navigationIcon = { Icon(Icons.Default.BugReport, null) }
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = onRestartClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restart")
                }
            }
        }
    ) { p ->
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(p)
        ) {
            Text("Something happened and the app crashed. Sorry!")
            Card(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(
                        exception?.toString().orEmpty(),
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}

class GlobalExceptionHandler private constructor(
    private val applicationContext: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler,
    private val activityToBeLaunched: Class<*>,
) : Thread.UncaughtExceptionHandler {

    object ThrowableSerializer : KSerializer<Throwable> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Throwable", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Throwable =
            Throwable(message = decoder.decodeString())

        override fun serialize(encoder: Encoder, value: Throwable) =
            encoder.encodeString(value.stackTraceToString())
    }

    @SuppressLint("LogNotLoged")
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            Log.e("Error", "Crash", exception)
            launchActivity(applicationContext, activityToBeLaunched, exception)
            exitProcess(0)
        } catch (_: Exception) {
            defaultHandler.uncaughtException(thread, exception)
        }
    }

    private fun launchActivity(
        applicationContext: Context,
        activity: Class<*>,
        exception: Throwable,
    ) {
        val intent = Intent(applicationContext, activity).apply {
            putExtra(INTENT_EXTRA, Json.encodeToString(ThrowableSerializer, exception))
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        applicationContext.startActivity(intent)
    }

    companion object {
        private const val INTENT_EXTRA = "Throwable"

        fun initialize(
            applicationContext: Context,
            activityToBeLaunched: Class<*>,
        ) {
            val handler = GlobalExceptionHandler(
                applicationContext,
                Thread.getDefaultUncaughtExceptionHandler() as Thread.UncaughtExceptionHandler,
                activityToBeLaunched,
            )
            Thread.setDefaultUncaughtExceptionHandler(handler)
        }

        @SuppressLint("LogNotLoged")
        fun getThrowableFromIntent(intent: Intent): Throwable? {
            return try {
                Json.decodeFromString(ThrowableSerializer, intent.getStringExtra(INTENT_EXTRA)!!)
            } catch (e: Exception) {
                Log.e("Error", "Wasn't able to retrieve throwable from intent")
                null
            }
        }
    }
}