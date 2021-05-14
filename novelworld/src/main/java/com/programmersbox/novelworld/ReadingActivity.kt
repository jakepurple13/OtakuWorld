package com.programmersbox.novelworld

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizePx
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.helpfulutils.*
import com.programmersbox.models.ChapterModel
import com.programmersbox.novelworld.databinding.ActivityReadingBinding
import com.programmersbox.rxutils.invoke
import com.programmersbox.rxutils.toLatestFlowable
import com.programmersbox.uiviews.BaseMainActivity
import com.programmersbox.uiviews.utils.ChapterModelDeserializer
import com.programmersbox.uiviews.utils.batteryAlertPercent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlin.math.roundToInt

class ReadingActivity : AppCompatActivity() {

    private val disposable = CompositeDisposable()
    private var batteryInfo: BroadcastReceiver? = null

    private lateinit var binding: ActivityReadingBinding

    private val batteryLevelAlert = PublishSubject.create<Float>()
    private val batteryInfoItem = PublishSubject.create<Battery>()

    enum class BatteryViewType(val icon: GoogleMaterial.Icon) {
        CHARGING_FULL(GoogleMaterial.Icon.gmd_battery_charging_full),
        DEFAULT(GoogleMaterial.Icon.gmd_battery_std),
        FULL(GoogleMaterial.Icon.gmd_battery_full),
        ALERT(GoogleMaterial.Icon.gmd_battery_alert),
        UNKNOWN(GoogleMaterial.Icon.gmd_battery_unknown)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableImmersiveMode()
        batterySetup()

        intent.getStringExtra("model")
            ?.fromJson<ChapterModel>(ChapterModel::class.java to ChapterModelDeserializer(BaseMainActivity.genericInfo))
            ?.getChapterInfo()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.map { HtmlCompat.fromHtml(it.firstOrNull()?.link.orEmpty(), HtmlCompat.FROM_HTML_MODE_COMPACT) }
            ?.subscribeBy { binding.contentArea.text = it }
            ?.addTo(disposable)
    }

    @SuppressLint("SetTextI18n")
    private fun batterySetup() {
        val batteryInformation = binding.batteryInformation

        val normalBatteryColor = colorFromTheme(R.attr.colorOnBackground, Color.WHITE)

        batteryInformation.startDrawable = IconicsDrawable(this, GoogleMaterial.Icon.gmd_battery_std).apply {
            colorInt = normalBatteryColor
            sizePx = batteryInformation.textSize.roundToInt()
        }

        Flowables.combineLatest(
            batteryLevelAlert
                .map { it <= batteryAlertPercent }
                .map { if (it) Color.RED else normalBatteryColor }
                .toLatestFlowable(),
            batteryInfoItem
                .map {
                    when {
                        it.isCharging -> BatteryViewType.CHARGING_FULL
                        it.percent <= batteryAlertPercent -> BatteryViewType.ALERT
                        it.percent >= 95 -> BatteryViewType.FULL
                        it.health == BatteryHealth.UNKNOWN -> BatteryViewType.UNKNOWN
                        else -> BatteryViewType.DEFAULT
                    }
                }
                .distinctUntilChanged { t1, t2 -> t1 != t2 }
                .map { IconicsDrawable(this, it.icon).apply { sizePx = batteryInformation.textSize.roundToInt() } }
                .toLatestFlowable()
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                it.second.colorInt = it.first
                batteryInformation.startDrawable = it.second
                batteryInformation.setTextColor(it.first)
                batteryInformation.startDrawable?.setTint(it.first)
            }
            .addTo(disposable)

        batteryInfo = battery {
            batteryInformation.text = "${it.percent.toInt()}%"
            batteryLevelAlert(it.percent)
            batteryInfoItem(it)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(batteryInfo)
        disposable.dispose()
        super.onDestroy()
    }
}