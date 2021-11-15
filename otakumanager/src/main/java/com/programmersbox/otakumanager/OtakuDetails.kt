package com.programmersbox.otakumanager

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.uiviews.utils.*
import com.skydoves.landscapist.glide.GlideImage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun DetailsScreen(
    info: ItemModel,
    logoId: Int,
    firebase: FirebaseDb2?,
    itemListener: FirebaseDb2.FirebaseListener?,
    chapterListener: FirebaseDb2.FirebaseListener?,
    navController: NavController
) {

    val disposable = remember { CompositeDisposable() }

    val systemUi = rememberSystemUiController()

    val model by info
        .toInfoModel()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeAsState(initial = null)

    val favorite by itemListener!!
        .findItemByUrl(info.url)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeAsState(initial = false)

    val watchedList by chapterListener!!
        .getAllEpisodesByShow(info.url)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeAsState(emptyList())

    var swatchInfo by remember { mutableStateOf<SwatchInfo?>(null) }

    val context = LocalContext.current

    Glide.with(context)
        .load(model?.imageUrl)
        .override(360, 480)
        .into<Drawable> {
            resourceReady { image, _ ->
                swatchInfo = image.getPalette().vibrantSwatch?.let { SwatchInfo(it.rgb, it.titleTextColor, it.bodyTextColor) }
            }
        }

    systemUi.setStatusBarColor(animateColorAsState(swatchInfo?.rgb?.toComposeColor() ?: MaterialTheme.colors.primaryVariant).value)

    DisposableEffect(model) {
        onDispose {
            disposable.dispose()
            itemListener?.unregister()
            chapterListener?.unregister()
        }
    }

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val closeDrawer: () -> Unit = { scope.launch { scaffoldState.bottomSheetState.collapse() } }

    BottomSheetScaffold(
        topBar = {
            TopAppBar(
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (scaffoldState.bottomSheetState.isCollapsed) scaffoldState.bottomSheetState.expand()
                                else scaffoldState.bottomSheetState.collapse()
                            }
                        }
                    ) { Icon(Icons.Default.MoreVert, null) }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                },
                title = {
                    Text(
                        info.title,
                        style = MaterialTheme.typography.h6
                    )
                },
                backgroundColor = swatchInfo?.rgb?.toComposeColor() ?: MaterialTheme.colors.primarySurface
            )
        },
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetShape = MaterialTheme.shapes.medium.copy(CornerSize(4.dp), CornerSize(4.dp), CornerSize(0.dp), CornerSize(0.dp)),
        sheetContent = {
            MenuItem(info.title, style = MaterialTheme.typography.h4.copy(color = MaterialTheme.colors.onSurface), enabled = false)
            Divider()
            MenuItem(R.string.share, Icons.Filled.Share) {
                closeDrawer()
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, info.url)
                    putExtra(Intent.EXTRA_TITLE, info.title)
                }, "Share ${info.title}"))
            }
            Divider()
            MenuItem(R.string.markAs) {
                closeDrawer()
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.markAs)
                    .setMultiChoiceItems(
                        model?.chapters.orEmpty().map(ChapterModel::name).toTypedArray(),
                        BooleanArray(model?.chapters?.size ?: 0) { i -> watchedList.any { it1 -> it1.url == model?.chapters?.get(i)?.url } }
                    ) { _, i, _ ->

                        /*(binding.infoChapterList.findViewHolderForAdapterPosition(i) as? ChapterAdapter.ChapterHolder)
                            ?.binding?.readChapter?.performClick()*/
                    }
                    .setPositiveButton(R.string.done) { d, _ -> d.dismiss() }
                    .show()
            }
            Divider()
            MenuItem(R.string.fallback_menu_item_open_in_browser) {
                closeDrawer()
                context.openInCustomChromeBrowser(info.url) { setShareState(CustomTabsIntent.SHARE_STATE_ON) }
            }
            Divider()
        }
    ) { p ->

        model?.let {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = p
            ) {

                item {
                    DetailsHeader(
                        model = it,
                        logoId = logoId,
                        swatchInfo = swatchInfo,
                        isFavorite = favorite
                    ) { b ->
                        fun addItem(model: InfoModel) {
                            val db = model.toDbModel(model.chapters.size)
                            firebase?.insertShow(db)
                                ?.subscribeOn(Schedulers.io())
                                ?.observeOn(AndroidSchedulers.mainThread())
                                ?.subscribe()
                                ?.addTo(disposable)
                        }

                        fun removeItem(model: InfoModel) {
                            val db = model.toDbModel(model.chapters.size)
                            firebase?.removeShow(db)
                                ?.subscribeOn(Schedulers.io())
                                ?.observeOn(AndroidSchedulers.mainThread())
                                ?.subscribe()
                                ?.addTo(disposable)
                        }

                        (if (b) ::removeItem else ::addItem)(it)
                    }
                }

                //chapterListener.getAllEpisodesByShow(info.url) -> for watchedList

                items(it.chapters) { c ->
                    ComposeChapterItem(
                        model = c,
                        watchedList = watchedList,
                        firebase = firebase,
                        disposable = disposable,
                        itemUrl = info.url,
                        swatchInfo = swatchInfo
                    )
                }

            }
        }

    }

}

@Composable
private fun MenuItem(
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    style: TextStyle = LocalTextStyle.current,
    action: () -> Unit = {}
) {
    DropdownMenuItem(onClick = action, enabled = enabled) {
        icon?.let { Icon(imageVector = it, null) }
        Text(text, style = style, modifier = Modifier.padding(horizontal = 5.dp))
    }
}

@Composable
private fun MenuItem(
    @StringRes text: Int,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    style: TextStyle = LocalTextStyle.current,
    action: () -> Unit = {}
) {
    DropdownMenuItem(onClick = action, enabled = enabled) {
        icon?.let { Icon(imageVector = it, null) }
        Text(stringResource(text), style = style, modifier = Modifier.padding(horizontal = 5.dp))
    }
}

@ExperimentalMaterialApi
@Composable
fun DetailsHeader(
    model: InfoModel,
    logoId: Int,
    swatchInfo: SwatchInfo?,
    isFavorite: Boolean,
    favoriteClick: (Boolean) -> Unit
) {

    var descriptionVisibility by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize()
    ) {

        GlideImage(
            imageModel = model.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    ColorUtils
                        .setAlphaComponent(
                            ColorUtils.blendARGB(
                                MaterialTheme.colors.primarySurface.toArgb(),
                                swatchInfo?.rgb ?: Color.Transparent.toArgb(),
                                0.25f
                            ),
                            200//127
                        )
                        .toComposeColor()
                )
        )

        Row(
            modifier = Modifier
                .padding(5.dp)
                .animateContentSize()
        ) {

            Card(
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier.padding(5.dp)
            ) {
                GlideImage(
                    imageModel = model.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    requestBuilder = {
                        Glide.with(LocalView.current)
                            .asDrawable()
                            //.override(360, 480)
                            .placeholder(logoId)
                            .error(logoId)
                            .fallback(logoId)
                            .transform(RoundedCorners(5))
                    },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT),
                    failure = {
                        Image(
                            painter = painterResource(id = logoId),
                            contentDescription = model.title,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                        )
                    }
                )
            }

            Column(
                modifier = Modifier.padding(start = 5.dp)
            ) {

                LazyRow(
                    modifier = Modifier.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(model.genres) {
                        CustomChip(
                            category = it,
                            textColor = swatchInfo?.rgb?.toComposeColor() ?: MaterialTheme.colors.onSurface,
                            backgroundColor = swatchInfo?.bodyColor?.toComposeColor()?.copy(1f) ?: MaterialTheme.colors.surface,
                            modifier = Modifier.fadeInAnimation()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clickable { favoriteClick(isFavorite) }
                        .padding(vertical = 5.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = swatchInfo?.rgb?.toComposeColor() ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Text(
                        stringResource(if (isFavorite) R.string.removeFromFavorites else R.string.addToFavorites),
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Text(
                    model.description,
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth()
                        .clickable { descriptionVisibility = !descriptionVisibility },
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (descriptionVisibility) Int.MAX_VALUE else 2,
                    style = MaterialTheme.typography.body2,
                )

            }

        }
    }
}

@ExperimentalMaterialApi
@Composable
fun ComposeChapterItem(
    model: ChapterModel,
    watchedList: List<ChapterWatched>,
    disposable: CompositeDisposable,
    firebase: FirebaseDb2?,
    itemUrl: String?,
    swatchInfo: SwatchInfo?
) {
    MdcTheme {

        val checkChange: (Boolean) -> Unit = { b ->
            /*itemUrl?.let { ChapterWatched(url = model.url, name = model.name, favoriteUrl = it) }
                ?.let { if (b) firebase?.insertEpisodeWatched(it) else firebase?.removeEpisodeWatched(it) }
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe()
                ?.addTo(disposable)*/
        }

        println(watchedList.map { it.url })

        val check = remember { watchedList.any { it.url == model.url } }

        Card(
            onClick = { checkChange(check) },
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .padding(vertical = 5.dp)
                .fillMaxWidth(),
            backgroundColor = swatchInfo?.rgb?.let { Color(it) } ?: MaterialTheme.colors.surface
        ) {

            Column(modifier = Modifier.padding(16.dp)) {

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {

                    Checkbox(
                        checked = check,
                        onCheckedChange = checkChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = swatchInfo?.bodyColor?.toComposeColor() ?: MaterialTheme.colors.secondary,
                            uncheckedColor = swatchInfo?.bodyColor?.toComposeColor() ?: MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            checkmarkColor = swatchInfo?.rgb?.toComposeColor() ?: MaterialTheme.colors.surface
                        )
                    )

                    Text(
                        model.name,
                        style = MaterialTheme.typography.body1
                            .let { b -> swatchInfo?.bodyColor?.toComposeColor()?.let { b.copy(color = it) } ?: b }
                    )

                }

                Text(
                    model.uploaded,
                    style = MaterialTheme.typography.subtitle2
                        .let { b -> swatchInfo?.bodyColor?.toComposeColor()?.let { b.copy(color = it) } ?: b },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(5.dp)
                )

            }

        }

    }
}