package com.programmersbox.otakuworld

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import com.bumptech.glide.Glide
import com.google.android.material.composethemeadapter.MdcTheme
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.models.ApiService
import com.programmersbox.models.ItemModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.toComposeColor
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.landscapist.palette.BitmapPalette
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import com.programmersbox.anime_sources.Sources as ASources
import com.programmersbox.manga_sources.Sources as MSources
import com.programmersbox.novel_sources.Sources as NSources

class MainActivity : ComponentActivity() {

    private var count = 0
    private val disposable = CompositeDisposable()
    private val sourceList = mutableStateListOf<ItemModel>()
    private val favorites = mutableStateListOf<DbModel>()

    @ExperimentalFoundationApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Single.merge(
            SourceChoice
                .values()
                .flatMap { it.choices.toList() }
                .fastMap {
                    it
                        .getRecent()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                }
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { sourceList.addAll(it) }
            .addTo(disposable)

        setContent {
            MdcTheme {

                /*BottomDrawer(
                    drawerContent = {
                        TestData()
                    }
                ) {
                    ModalDrawer(
                        drawerContent = {
                            TestData()
                        }
                    ) {
                        BackdropScaffold(
                            appBar = { TopAppBar(title = { Text("Hello World") }) },
                            backLayerContent = { TestData() },
                            frontLayerContent = { TestData() }
                        )
                    }
                }*/

                Scaffold(
                    topBar = { TopAppBar(title = { Text("UI Test") }) }
                ) { p ->
                    /*LazyVerticalGrid(
                        cells = GridCells.Fixed(2),
                        contentPadding = p
                    ) {
                        items(sourceList) {
                            InfoCard(info = it, favorites = emptyList())
                        }
                    }*/

                    LazyColumn(
                        contentPadding = p,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(sourceList) {
                            InfoCard(info = it, favorites = favorites) {
                                val db = it.toDbModel()
                                if (db in favorites) favorites.remove(db) else favorites.add(db)
                            }
                        }
                    }
                }

            }
        }
    }

    @Composable
    fun TestData() = Scaffold { Column { repeat(10) { Text("Hello ${count++}") } } }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}

enum class SourceChoice(vararg val choices: ApiService) {
    ANIME(ASources.GOGOANIME_VC, ASources.VIDSTREAMING),
    MANGA(MSources.MANGA_HERE, MSources.MANGAMUTINY),
    NOVEL(NSources.WUXIAWORLD)
}

@Composable
private fun Color.animate() = animateColorAsState(this)

@ExperimentalMaterialApi
@Composable
fun InfoCard(
    info: ItemModel,
    favorites: List<DbModel>,
    onClick: () -> Unit
) {

    val swatchInfo = remember { mutableStateOf<SwatchInfo?>(null) }

    Card(
        backgroundColor = swatchInfo.value?.rgb?.toComposeColor()?.animate()?.value ?: MaterialTheme.colors.surface,
        modifier = Modifier.padding(4.dp),
        onClick = onClick
    ) {
        Column {

            Row {

                GlideImage(
                    imageModel = info.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    requestBuilder = Glide.with(LocalContext.current.applicationContext).asDrawable(),
                    bitmapPalette = BitmapPalette { p ->
                        swatchInfo.value = p.vibrantSwatch?.let { s -> SwatchInfo(s.rgb, s.titleTextColor, s.bodyTextColor) }
                    },
                    modifier = Modifier
                        .clip(
                            MaterialTheme.shapes.medium
                                .copy(
                                    topEnd = CornerSize(0.dp),
                                    bottomStart = CornerSize(0.dp),
                                    bottomEnd = CornerSize(0.dp)
                                )
                        )
                        .size(
                            ComposableUtils.IMAGE_WIDTH / 1.5f,
                            ComposableUtils.IMAGE_HEIGHT / 1.5f
                        )
                )

                ListItem(
                    overlineText = {
                        Text(
                            info.source.serviceName,
                            color = swatchInfo.value?.titleColor?.toComposeColor()?.animate()?.value ?: Color.Unspecified
                        )
                    },
                    text = {
                        Text(
                            info.title,
                            color = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: Color.Unspecified
                        )
                    },
                    secondaryText = {
                        Text(
                            info.description,
                            color = swatchInfo.value?.titleColor?.toComposeColor()?.animate()?.value ?: Color.Unspecified
                        )
                    }
                )

            }

            Divider(
                thickness = 0.5.dp,
                color = swatchInfo.value?.titleColor?.toComposeColor()?.animate()?.value ?: MaterialTheme.colors.onSurface.copy(alpha = .12f)
            )

            Row {

                Box(Modifier.padding(5.dp)) {
                    if (favorites.fastAny { f -> f.url == info.url }) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = swatchInfo.value?.bodyColor?.toComposeColor()?.animate()?.value ?: MaterialTheme.colors.primary,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }

            }
        }
    }
}