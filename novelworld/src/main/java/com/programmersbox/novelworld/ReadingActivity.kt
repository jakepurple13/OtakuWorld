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
import com.programmersbox.helpfulutils.battery
import com.programmersbox.helpfulutils.colorFromTheme
import com.programmersbox.helpfulutils.enableImmersiveMode
import com.programmersbox.helpfulutils.startDrawable
import com.programmersbox.models.ChapterModel
import com.programmersbox.novelworld.databinding.ActivityReadingBinding
import com.programmersbox.rxutils.invoke
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.BatteryInformation
import com.programmersbox.uiviews.utils.ChapterModelDeserializer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject
import kotlin.math.roundToInt

class ReadingActivity : AppCompatActivity() {

    private val disposable = CompositeDisposable()
    private var batteryInfo: BroadcastReceiver? = null

    private lateinit var binding: ActivityReadingBinding

    private val batteryInformation by lazy { BatteryInformation(this) }

    private val genericInfo by inject<GenericInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableImmersiveMode()
        batterySetup()

        intent.getStringExtra("model")
            ?.fromJson<ChapterModel>(ChapterModel::class.java to ChapterModelDeserializer(genericInfo))
            ?.getChapterInfo()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.map { HtmlCompat.fromHtml(it.firstOrNull()?.link.orEmpty(), HtmlCompat.FROM_HTML_MODE_COMPACT) }
            ?.subscribeBy { binding.contentArea.text = it }
            ?.addTo(disposable)
    }

    @SuppressLint("SetTextI18n")
    private fun batterySetup() {
        val normalBatteryColor = colorFromTheme(R.attr.colorOnBackground, Color.WHITE)

        binding.batteryInformation.startDrawable = IconicsDrawable(this, GoogleMaterial.Icon.gmd_battery_std).apply {
            colorInt = normalBatteryColor
            sizePx = binding.batteryInformation.textSize.roundToInt()
        }

        batteryInformation.setup(
            disposable = disposable,
            size = binding.batteryInformation.textSize.roundToInt(),
            normalBatteryColor = colorFromTheme(R.attr.colorOnBackground, Color.WHITE)
        ) {
            it.second.colorInt = it.first
            binding.batteryInformation.startDrawable = it.second
            binding.batteryInformation.setTextColor(it.first)
            binding.batteryInformation.startDrawable?.setTint(it.first)
        }

        batteryInfo = battery {
            binding.batteryInformation.text = "${it.percent.toInt()}%"
            batteryInformation.batteryLevelAlert(it.percent)
            batteryInformation.batteryInfoItem(it)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(batteryInfo)
        disposable.dispose()
        super.onDestroy()
    }
}