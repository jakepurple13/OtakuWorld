/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.programmersbox.wearcompanion.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.programmersbox.wearcompanion.R
import com.programmersbox.wearcompanion.presentation.theme.OtakuWorldTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val data by lazy {
        Wearable.getDataClient(this)
    }
    val dataListener = DataClient.OnDataChangedListener { events ->
        events.forEach {
            println(it.dataItem.data?.decodeToString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        //TODO: This app will show the current saved notifications
        // and will allow them to choose to mark as read, remove, notify



        setContent {
            WearApp("Android")
        }
    }

    override fun onResume() {
        super.onResume()
        data.addListener(dataListener)
    }

    override fun onPause() {
        super.onPause()
        data.addListener(dataListener)
    }
}

@Composable
fun WearApp(greetingName: String) {
    OtakuWorldTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Greeting(greetingName = greetingName)
        }
    }
}

@Composable
fun Greeting(greetingName: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary,
        text = stringResource(R.string.hello_world, greetingName)
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}

class DataLayerListenerService : WearableListenerService() {

    private val messageClient by lazy { Wearable.getMessageClient(this) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        dataEvents.forEach { dataEvent ->
            val uri = dataEvent.dataItem.uri
            when (uri.path) {
                COUNT_PATH -> {
                    println(dataEvent.dataItem.data?.decodeToString())
                    scope.launch {
                        try {
                            /*val nodeId = uri.host!!
                            val payload = uri.toString().toByteArray()
                            messageClient.sendMessage(
                                nodeId,
                                DATA_ITEM_RECEIVED_PATH,
                                payload
                            ).await()*/
                            //Log.d(TAG, "Message sent successfully")
                        } catch (cancellationException: CancellationException) {
                            throw cancellationException
                        } catch (exception: Exception) {
                            //Log.d(TAG, "Message failed")
                        }
                    }
                }
            }
        }
    }

    // When the message to start the Wearable app is received, this method starts the Wearable app.
    // Alternative to this implementation, Horologist offers a DataHelper API which allows to
    // start the main activity or a different activity of your choice from the Wearable app
    // see https://google.github.io/horologist/datalayer-helpers-guide/#launching-a-specific-activity-on-the-other-device
    // for details
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        println(messageEvent.data.decodeToString())

        when (messageEvent.path) {
            START_ACTIVITY_PATH -> {
                /*startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )*/
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "DataLayerService"

        private const val START_ACTIVITY_PATH = "/start-activity"
        private const val DATA_ITEM_RECEIVED_PATH = "/data-item-received"
        const val COUNT_PATH = "/COUNT_PATH"
        const val IMAGE_PATH = "/image"
        const val IMAGE_KEY = "photo"
    }
}