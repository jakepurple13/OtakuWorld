package com.programmersbox.mangaworld

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.github.piasy.biv.BigImageViewer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizePx
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.helpfulutils.*
import com.programmersbox.mangaworld.databinding.ActivityReadBinding
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.Storage
import com.programmersbox.rxutils.invoke
import com.programmersbox.rxutils.toLatestFlowable
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.ChapterModelDeserializer
import com.programmersbox.uiviews.utils.batteryAlertPercent
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.android.inject
import java.io.File
import java.util.*
import kotlin.math.roundToInt

class ReadActivity : AppCompatActivity() {

    private val disposable = CompositeDisposable()
    private var model: ChapterModel? = null
    private var mangaTitle: String? = null
    private var isDownloaded = false
    private val loader by lazy { Glide.with(this) }
    /*private val adapter by lazy {
        loader.let {
            PageAdapter(
                fullRequest = it
                    .asDrawable()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop(),
                thumbRequest = it
                    .asDrawable()
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .transition(withCrossFade()),
                context = this@ReadActivity,
                dataList = mutableListOf()
            ) { image ->
                requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE) { p ->
                    if (p.isGranted) saveImage("${mangaTitle}_${model?.name}_${image.toUri().lastPathSegment}", image)
                }
            }
        }
    }*/


    private val genericInfo by inject<GenericInfo>()

    private fun View.slideUp() {
        val layoutParams = this.layoutParams
        if (layoutParams is CoordinatorLayout.LayoutParams) {
            @Suppress("UNCHECKED_CAST")
            (layoutParams.behavior as? CustomHideBottomViewOnScrollBehavior<View>)?.slideUp(this)
        }
    }

    private fun View.slideDown() {
        val layoutParams = this.layoutParams
        if (layoutParams is CoordinatorLayout.LayoutParams) {
            @Suppress("UNCHECKED_CAST")
            (layoutParams.behavior as? CustomHideBottomViewOnScrollBehavior<View>)?.slideDown(this)
        }
    }

    private val sliderMenu by lazy {
        val layoutParams = binding.bottomMenu.layoutParams
        if (layoutParams is CoordinatorLayout.LayoutParams) {
            @Suppress("UNCHECKED_CAST")
            layoutParams.behavior as? CustomHideBottomViewOnScrollBehavior<RelativeLayout>
        } else null
    }

    private val fab by lazy {
        val layoutParams = binding.scrollToTopManga.layoutParams
        if (layoutParams is CoordinatorLayout.LayoutParams) {
            @Suppress("UNCHECKED_CAST")
            layoutParams.behavior as? CustomHideBottomViewOnScrollBehavior<FloatingActionButton>
        } else null
    }

    private var menuToggle = false

    private val adapter2: PageAdapter by lazy {
        loader.let {
            val list = intent.getStringExtra("allChapters")
                ?.fromJson<List<ChapterModel>>(ChapterModel::class.java to ChapterModelDeserializer(genericInfo))
                .orEmpty().also(::println)
            //intent.getObjectExtra<List<ChapterModel>>("allChapters") ?: emptyList()
            val url = intent.getStringExtra("mangaUrl") ?: ""
            val mangaUrl = intent.getStringExtra("mangaInfoUrl") ?: ""
            PageAdapter(
                fullRequest = it
                    .asDrawable()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop(),
                thumbRequest = it
                    .asDrawable()
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .transition(withCrossFade()),
                activity = this@ReadActivity,
                dataList = mutableListOf(),
                onTap = {
                    menuToggle = !menuToggle
                    if (sliderMenu?.isShowing?.not() ?: menuToggle) binding.bottomMenu.slideUp() else binding.bottomMenu.slideDown()
                    if (fab?.isShowing?.not() ?: menuToggle) binding.scrollToTopManga.slideUp() else binding.scrollToTopManga.slideDown()
                },
                coordinatorLayout = binding.readLayout,
                chapterModels = list,
                currentChapter = list.indexOfFirst { l -> l.url == url },
                mangaUrl = mangaUrl,
                loadNewPages = this::loadPages
            ) { image ->
                requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE) { p ->
                    if (p.isGranted) saveImage("${mangaTitle}_${model?.name}_${image.toUri().lastPathSegment}", image)
                }
            }
        }
    }

    private var batteryInfo: BroadcastReceiver? = null

    private val batteryLevelAlert = PublishSubject.create<Float>()
    private val batteryInfoItem = PublishSubject.create<Battery>()

    enum class BatteryViewType(val icon: GoogleMaterial.Icon) {
        CHARGING_FULL(GoogleMaterial.Icon.gmd_battery_charging_full),
        DEFAULT(GoogleMaterial.Icon.gmd_battery_std),
        FULL(GoogleMaterial.Icon.gmd_battery_full),
        ALERT(GoogleMaterial.Icon.gmd_battery_alert),
        UNKNOWN(GoogleMaterial.Icon.gmd_battery_unknown)
    }

    private lateinit var binding: ActivityReadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*MobileAds.initialize(this) {}
        MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(listOf("BCF3E346AED658CDCCB1DDAEE8D84845")).build())*/

        enableImmersiveMode()

        //adViewing.loadAd(AdRequest.Builder().build())

        /*window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                println("Visible")
                titleManga.animate().alpha(1f).withStartAction { titleManga.visible() }.start()
            } else {
                println("Invisible")
                titleManga.animate().alpha(0f).withEndAction { titleManga.invisible() }.start()
            }
        }*/

        infoSetup()
        readerSetup()
    }

    private fun readerSetup() {
        val preloader: RecyclerViewPreloader<String> = RecyclerViewPreloader(loader, adapter2, ViewPreloadSizeProvider(), 10)
        val readView = binding.readView
        readView.addOnScrollListener(preloader)
        readView.setItemViewCacheSize(0)

        readView.adapter = adapter2

        readView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val l = recyclerView.layoutManager as LinearLayoutManager
                val image = l.findLastVisibleItemPosition()
                if (image > -1) {
                    val total = l.itemCount
                    binding.pageCount.text = String.format("%d/%d", image + 1, total)
                    binding.pageChoice.value = (image + 1).toFloat()
                    if (image + 1 == total) sliderMenu?.slideDown(binding.bottomMenu)
                }
            }
        })

        //readView.setRecyclerListener { (it as? PageHolder)?.image?.ssiv?.recycle() }

        /*val models = intent.getObjectExtra<List<ChapterModel>>("allChapters")
        val url = intent.getStringExtra("mangaUrl")
        var currentIndex = models?.indexOfFirst { it.url == url }
        var currentModel = currentIndex?.let { models?.getOrNull(it) }*/

        mangaTitle = intent.getStringExtra("mangaTitle")
        model = intent.getStringExtra("currentChapter")
            ?.fromJson<ChapterModel>(ChapterModel::class.java to ChapterModelDeserializer(genericInfo))

        isDownloaded = intent.getBooleanExtra("downloaded", false)
        val file = intent.getSerializableExtra("filePath") as? File
        if (isDownloaded && file != null) loadFileImages(file)
        else loadPages(model)

        binding.readRefresh.setOnRefreshListener {
            binding.readRefresh.isRefreshing = false
            adapter2.reloadChapter()
        }

        binding.scrollToTopManga.setOnClickListener { binding.readView.smoothScrollToPosition(0) }
        binding.pageChoice.addOnChangeListener { _, value, fromUser ->
            if (fromUser) binding.readView.scrollToPosition(value.toInt() - 1)
        }
    }

    private fun loadFileImages(file: File) {
        println(file.absolutePath)
        Single.create<List<String>> {
            file.listFiles()
                ?.sortedBy { f -> f.name.split(".").first().toInt() }
                ?.map(File::toUri)
                ?.map(Uri::toString)
                ?.let(it::onSuccess) ?: it.onError(Throwable("Cannot find files"))
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                BigImageViewer.prefetch(*it.map(Uri::parse).toTypedArray())
                binding.readLoading
                    .animate()
                    .alpha(0f)
                    .withEndAction { binding.readLoading.gone() }
                    .start()
                adapter2.setListNotify(it)

                binding.pageChoice.valueTo = try {
                    val f = it.size.toFloat() + 1
                    if (f == 1f) 2f else f
                } catch (e: Exception) {
                    2f
                }
            }
            .addTo(disposable)
    }

    private fun loadPages(model: ChapterModel?) {
        Glide.get(this).clearMemory()
        binding.readLoading
            .animate()
            .withStartAction { binding.readLoading.visible() }
            .alpha(1f)
            .start()
        adapter2.setListNotify(emptyList())
        model?.getChapterInfo()
            ?.map { it.mapNotNull(Storage::link) }
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.doOnError { Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show() }
            ?.subscribeBy { pages: List<String> ->
                BigImageViewer.prefetch(*pages.map(Uri::parse).toTypedArray())
                binding.readLoading
                    .animate()
                    .alpha(0f)
                    .withEndAction { binding.readLoading.gone() }
                    .start()
                adapter2.setListNotify(pages)
                binding.pageChoice.valueTo = try {
                    pages.size.toFloat() + 1
                } catch (e: Exception) {
                    2f
                }
                //adapter.addItems(pages)
                //binding.readView.layoutManager!!.scrollToPosition(model.url.let { defaultSharedPref.getInt(it, 0) })
            }
            ?.addTo(disposable)
    }

    private fun infoSetup() {
        batterySetup()
    }

    @SuppressLint("SetTextI18n")
    private fun batterySetup() {
        val normalBatteryColor = colorFromTheme(R.attr.colorOnBackground, Color.WHITE)

        binding.batteryInformation.startDrawable = IconicsDrawable(this, GoogleMaterial.Icon.gmd_battery_std).apply {
            colorInt = normalBatteryColor
            sizePx = binding.batteryInformation.textSize.roundToInt()
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
                .map { IconicsDrawable(this, it.icon).apply { sizePx = binding.batteryInformation.textSize.roundToInt() } }
                .toLatestFlowable()
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                it.second.colorInt = it.first
                binding.batteryInformation.startDrawable = it.second
                binding.batteryInformation.setTextColor(it.first)
                binding.batteryInformation.startDrawable?.setTint(it.first)
            }
            .addTo(disposable)

        batteryInfo = battery {
            binding.batteryInformation.text = "${it.percent.toInt()}%"
            batteryLevelAlert(it.percent)
            batteryInfoItem(it)
        }
    }

    private fun saveCurrentChapterSpot() {
        /*model?.let {
            defaultSharedPref.edit().apply {
                val currentItem = (readView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                if (currentItem >= adapter.dataList.size - 2) remove(it.url)
                else putInt(it.url, currentItem)
            }.apply()
        }*/
    }

    private fun saveImage(filename: String, downloadUrlOfImage: String) {
        val direct = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + File.separator + "MangaWorld" + File.separator)

        if (!direct.exists()) direct.mkdir()

        downloadManager.enqueue(this) {
            downloadUri = Uri.parse(downloadUrlOfImage)
            allowOverRoaming = true
            networkType = DownloadDslManager.NetworkType.WIFI_MOBILE
            title = filename
            mimeType = "image/jpeg"
            visibility = DownloadDslManager.NotificationVisibility.COMPLETED
            destinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, File.separator + "MangaWorld" + File.separator + filename)
        }
    }

    override fun onPause() {
        //saveCurrentChapterSpot()
        //adViewing.pause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Glide.get(this).clearMemory()
        unregisterReceiver(batteryInfo)
        disposable.dispose()
    }

}