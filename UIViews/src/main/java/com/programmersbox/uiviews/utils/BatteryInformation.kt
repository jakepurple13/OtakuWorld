package com.programmersbox.uiviews.utils

import android.content.Context
import android.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BatteryUnknown
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.ui.graphics.vector.ImageVector
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.sizePx
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.helpfulutils.Battery
import com.programmersbox.helpfulutils.BatteryHealth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class BatteryInformation(val context: Context) : KoinComponent {

    val batteryLevel by lazy { MutableStateFlow<Float>(0f) }
    val batteryInfo by lazy { MutableSharedFlow<Battery>() }
    val settingsHandling: NewSettingsHandling by inject()

    private val batteryPercent by lazy { settingsHandling.batteryPercent }

    enum class BatteryViewType(val icon: GoogleMaterial.Icon, val composeIcon: ImageVector) {
        CHARGING_FULL(GoogleMaterial.Icon.gmd_battery_charging_full, Icons.Default.BatteryChargingFull),
        DEFAULT(GoogleMaterial.Icon.gmd_battery_std, Icons.Default.BatteryStd),
        FULL(GoogleMaterial.Icon.gmd_battery_full, Icons.Default.BatteryFull),
        ALERT(GoogleMaterial.Icon.gmd_battery_alert, Icons.Default.BatteryAlert),
        UNKNOWN(GoogleMaterial.Icon.gmd_battery_unknown, Icons.AutoMirrored.Filled.BatteryUnknown)
    }

    suspend fun composeSetupFlow(
        normalBatteryColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
        subscribe: suspend (Pair<androidx.compose.ui.graphics.Color, BatteryViewType>) -> Unit,
    ) = combine(
        combine(
            batteryLevel,
            batteryPercent.asFlow()
        ) { b, d -> b <= d }
            .map { if (it) androidx.compose.ui.graphics.Color.Red else normalBatteryColor },
        combine(
            batteryInfo,
            batteryPercent.asFlow()
        ) { b, d -> b to d }
            .map {
                when {
                    it.first.isCharging -> BatteryViewType.CHARGING_FULL
                    it.first.percent <= it.second -> BatteryViewType.ALERT
                    it.first.percent >= 95 -> BatteryViewType.FULL
                    it.first.health == BatteryHealth.UNKNOWN -> BatteryViewType.UNKNOWN
                    else -> BatteryViewType.DEFAULT
                }
            }
            .distinctUntilChanged { t1, t2 -> t1 != t2 },
    ) { l, b -> l to b }
        .onEach(subscribe)

    suspend fun setupFlow(
        normalBatteryColor: Int = Color.WHITE,
        size: Int,
        subscribe: (Pair<Int, IconicsDrawable>) -> Unit,
    ) {
        combine(
            combine(
                batteryLevel,
                batteryPercent.asFlow()
            ) { b, d -> b <= d }
                .map { if (it) Color.RED else normalBatteryColor },
            combine(
                batteryInfo,
                batteryPercent.asFlow()
            ) { b, d -> b to d }
                .map {
                    when {
                        it.first.isCharging -> BatteryViewType.CHARGING_FULL
                        it.first.percent <= it.second -> BatteryViewType.ALERT
                        it.first.percent >= 95 -> BatteryViewType.FULL
                        it.first.health == BatteryHealth.UNKNOWN -> BatteryViewType.UNKNOWN
                        else -> BatteryViewType.DEFAULT
                    }
                }
                .distinctUntilChanged { t1, t2 -> t1 != t2 }
                .map { IconicsDrawable(context, it.icon).apply { sizePx = size } },
        ) { l, b -> l to b }
            .onEach(subscribe)
            .collect()
    }
}