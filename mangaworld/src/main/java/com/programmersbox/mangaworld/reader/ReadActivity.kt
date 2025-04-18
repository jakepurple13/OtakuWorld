package com.programmersbox.mangaworld.reader

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.util.fastMap
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.github.piasy.biv.BigImageViewer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizePx
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.helpfulutils.battery
import com.programmersbox.helpfulutils.colorFromTheme
import com.programmersbox.helpfulutils.enableImmersiveMode
import com.programmersbox.helpfulutils.gone
import com.programmersbox.helpfulutils.startDrawable
import com.programmersbox.helpfulutils.visible
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.mangaworld.CustomHideBottomViewOnScrollBehavior
import com.programmersbox.mangaworld.R
import com.programmersbox.mangaworld.databinding.ActivityReadBinding
import com.programmersbox.mangaworld.databinding.ReaderSettingsDialogBinding
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.Storage
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.BatteryInformation
import com.programmersbox.uiviews.utils.ChapterModelDeserializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import java.io.File
import kotlin.math.roundToInt

class ReadActivity : AppCompatActivity() {
    private var model: ChapterModel? = null
    private var mangaTitle: String? = null
    private var isDownloaded = false
    private val loader by lazy { Glide.with(this) }
    private val genericInfo by inject<GenericInfo>()
    private val settingsHandling: NewSettingsHandling by inject()
    private val mangaSettingsHandling: MangaNewSettingsHandling by inject()

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
                ?.fromJson<List<ChapterModel>>(ChapterModel::class.java to ChapterModelDeserializer())
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
                activity = this,
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
            )
        }
    }

    private var batteryInfo: BroadcastReceiver? = null

    private val batteryInformation by lazy { BatteryInformation(this) }

    private lateinit var binding: ActivityReadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableImmersiveMode()

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
            ?.fromJson<ChapterModel>(ChapterModel::class.java to ChapterModelDeserializer())

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

        var decor = VerticalSpaceItemDecoration(4)
        binding.readView.addItemDecoration(decor)
        lifecycleScope.launch {
            mangaSettingsHandling.pagePadding.flow
                .flowWithLifecycle(lifecycle)
                .onEach {
                    runOnUiThread {
                        binding.readView.removeItemDecoration(decor)
                        decor = VerticalSpaceItemDecoration(it)
                        binding.readView.addItemDecoration(decor)
                    }
                }
                .collect()
        }

        binding.readerSettings.setOnClickListener {
            val readerBinding = ReaderSettingsDialogBinding.inflate(layoutInflater)

            val padding = runBlocking { mangaSettingsHandling.pagePadding.flow.firstOrNull() ?: 4 }
            readerBinding.pagePaddingSlider.value = padding.toFloat()
            readerBinding.sliderValue.text = padding.toString()
            readerBinding.pagePaddingSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    lifecycleScope.launch(Dispatchers.IO) { mangaSettingsHandling.pagePadding.updateSetting(value.toInt()) }
                }
                readerBinding.sliderValue.text = value.toInt().toString()
            }

            val batteryPercent = settingsHandling.batteryPercent

            val battery = runBlocking { batteryPercent.get() }
            readerBinding.batterySlider.value = battery.toFloat()
            readerBinding.batterySliderValue.text = battery.toString()
            readerBinding.batterySlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    lifecycleScope.launch(Dispatchers.IO) { batteryPercent.set(value.toInt()) }
                }
                readerBinding.batterySliderValue.text = value.toInt().toString()
            }

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings)
                .setView(readerBinding.root)
                .setPositiveButton(R.string.ok) { d, _ -> d.dismiss() }
                .show()
        }
    }

    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            if (parent.getChildAdapterPosition(view) != parent.adapter!!.itemCount - 1) {
                outRect.bottom = view.context.dpToPx(verticalSpaceHeight)
                outRect.top = view.context.dpToPx(verticalSpaceHeight)
            }
        }

        private fun Context.dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
    }

    private fun loadFileImages(file: File) {
        println(file.absolutePath)
        lifecycleScope.launch {
            flow<List<String>> {
                file.listFiles()
                    ?.sortedBy { f -> f.name.split(".").first().toInt() }
                    ?.fastMap(File::toUri)
                    ?.fastMap(Uri::toString)
                    ?.let { emit(it) } ?: throw Exception("Cannot find files")
            }
                .catch { runOnUiThread { Toast.makeText(this@ReadActivity, it.localizedMessage, Toast.LENGTH_SHORT).show() } }
                .flowOn(Dispatchers.Main)
                .onEach { pages: List<String> ->
                    BigImageViewer.prefetch(*pages.fastMap(Uri::parse).toTypedArray())
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
                .collect()
        }
    }

    private fun loadPages(model: ChapterModel?) {
        Glide.get(this).clearMemory()
        binding.readLoading
            .animate()
            .withStartAction { binding.readLoading.visible() }
            .alpha(1f)
            .start()
        adapter2.setListNotify(emptyList())
        lifecycleScope.launch {
            model?.getChapterInfo()
                ?.map { it.mapNotNull(Storage::link) }
                ?.catch { runOnUiThread { Toast.makeText(this@ReadActivity, it.localizedMessage, Toast.LENGTH_SHORT).show() } }
                ?.flowOn(Dispatchers.Main)
                ?.onEach { pages: List<String> ->
                    BigImageViewer.prefetch(*pages.fastMap(Uri::parse).toTypedArray())
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
                ?.collect()
        }
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

        lifecycleScope.launch {
            batteryInformation.setupFlow(
                size = binding.batteryInformation.textSize.roundToInt(),
                normalBatteryColor = normalBatteryColor
            ) {
                it.second.colorInt = it.first
                binding.batteryInformation.startDrawable = it.second
                binding.batteryInformation.setTextColor(it.first)
                binding.batteryInformation.startDrawable?.setTint(it.first)
            }
        }

        batteryInfo = battery {
            binding.batteryInformation.text = "${it.percent.toInt()}%"
            batteryInformation.batteryLevel.tryEmit(it.percent)
            batteryInformation.batteryInfo.tryEmit(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Glide.get(this).clearMemory()
        unregisterReceiver(batteryInfo)
    }

}