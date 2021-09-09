package com.programmersbox.uiviews

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.snackbar.Snackbar
import com.programmersbox.dragswipe.CheckAdapter
import com.programmersbox.dragswipe.CheckAdapterInterface
import com.programmersbox.dragswipe.DragSwipeAdapter
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.helpfulutils.layoutInflater
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.SwatchInfo
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.uiviews.databinding.ChapterItemBinding
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers

class ChapterAdapter(
    private val context: Context,
    private val genericInfo: GenericInfo,
    private val dao: ItemDao,
    private val disposable: CompositeDisposable,
    check: CheckAdapter<ChapterModel, ChapterWatched> = CheckAdapter()
) : DragSwipeAdapter<ChapterModel, ChapterAdapter.ChapterHolder>(), CheckAdapterInterface<ChapterModel, ChapterWatched> by check {

    init {
        check.adapter = this
    }

    var swatchInfo: SwatchInfo? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var itemUrl: String? = null
    var title: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterHolder =
        ChapterHolder(ChapterItemBinding.inflate(context.layoutInflater, parent, false))

    override fun ChapterHolder.onBind(item: ChapterModel, position: Int) = bind(item)

    inner class ChapterHolder(val binding: ChapterItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chapterModel: ChapterModel) {
            binding.chapter = chapterModel
            binding.swatch = swatchInfo
            binding.executePendingBindings()
            binding.readChapterButton.setOnClickListener { binding.chapterListCard.performClick() }
            //binding.markedReadButton.setOnClickListener { binding.readChapter.performClick() }
            //binding.uploadedInfo2.setOnClickListener { binding.chapterListCard.performClick() }
            binding.chapterListCard.setOnClickListener {
                genericInfo.chapterOnClick(chapterModel, dataList, context)
                binding.readChapter.isChecked = true
            }
            binding.downloadChapterButton.setOnClickListener {
                genericInfo.downloadChapter(chapterModel, title.orEmpty())
                binding.readChapter.isChecked = true
            }
            binding.readChapter.setOnCheckedChangeListener(null)
            binding.readChapter.isChecked = currentList.fastAny { it.url == chapterModel.url }
            binding.readChapter.setOnCheckedChangeListener { _, b ->
                itemUrl?.let { ChapterWatched(url = chapterModel.url, name = chapterModel.name, favoriteUrl = it) }
                    ?.let {
                        Completable.mergeArray(
                            if (b) FirebaseDb.insertEpisodeWatched(it) else FirebaseDb.removeEpisodeWatched(it),
                            if (b) dao.insertChapter(it) else dao.deleteChapter(it)
                        )
                    }
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe {
                        Snackbar.make(
                            binding.root,
                            if (b) R.string.addedChapterItem else R.string.removedChapterItem,
                            Snackbar.LENGTH_SHORT
                        )
                            .setAction(R.string.undo) { binding.readChapter.isChecked = !binding.readChapter.isChecked }
                            .show()
                    }
                    ?.addTo(disposable)
            }

        }
    }
}

@ExperimentalMaterialApi
@Composable
fun ComposeChapterItem(
    model: ChapterModel,
    watchedList: State<List<ChapterWatched>>,
    dao: ItemDao?,
    disposable: CompositeDisposable,
    itemUrl: String?,
    info: GenericInfo?,
    chapterList: List<ChapterModel>,
    title: String?,
    swatchInfo: SwatchInfo?
) {
    MdcTheme {

        val context = LocalView.current.context
        var check by remember { mutableStateOf(watchedList.value.any { it.url == model.url }) }

        Card(
            onClick = {
                info?.chapterOnClick(model, chapterList, context)
                check = true
            },
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .padding(vertical = 5.dp)
                .fillMaxWidth(),
            backgroundColor = swatchInfo?.rgb?.let { Color(it) } ?: MaterialTheme.colors.surface
        ) {

            Column(modifier = Modifier.padding(16.dp)) {

                Row {

                    Checkbox(
                        checked = check,
                        onCheckedChange = { b ->
                            println(b)
                            itemUrl?.let { ChapterWatched(url = model.url, name = model.name, favoriteUrl = it) }
                                ?.let {
                                    Completable.mergeArray(
                                        if (b) FirebaseDb.insertEpisodeWatched(it) else FirebaseDb.removeEpisodeWatched(it),
                                        if (b) dao?.insertChapter(it) else dao?.deleteChapter(it)
                                    )
                                }
                                ?.subscribeOn(Schedulers.io())
                                ?.observeOn(AndroidSchedulers.mainThread())
                                ?.subscribe {
                                    /*Snackbar.make(
                                        binding.root,
                                        if (b) R.string.addedChapterItem else R.string.removedChapterItem,
                                        Snackbar.LENGTH_SHORT
                                    )
                                        .setAction(R.string.undo) { binding.readChapter.isChecked = !binding.readChapter.isChecked }
                                        .show()*/
                                }
                                ?.addTo(disposable)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = swatchInfo?.bodyColor?.let { Color(it) } ?: MaterialTheme.colors.secondary,
                            uncheckedColor = swatchInfo?.bodyColor?.let { Color(it) } ?: MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            checkmarkColor = Color.Transparent
                        )
                    )

                    Text(
                        model.name,
                        style = MaterialTheme.typography.body1
                            .let { b -> swatchInfo?.bodyColor?.let { b.copy(color = Color(it)) } ?: b }
                    )

                }

                Text(
                    model.uploaded,
                    style = MaterialTheme.typography.subtitle2
                        .let { b -> swatchInfo?.bodyColor?.let { b.copy(color = Color(it)) } ?: b },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(5.dp)
                )

                Row {

                    OutlinedButton(
                        onClick = {
                            info?.chapterOnClick(model, chapterList, context)
                            check = true
                        },
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(horizontal = 5.dp),
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(1.dp, swatchInfo?.bodyColor?.let { Color(it) } ?: Color.White)
                    ) {
                        Column {
                            Icon(
                                Icons.Default.PlayArrow,
                                "Play",
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                tint = swatchInfo?.bodyColor?.let { Color(it) } ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                            )
                            Text(
                                stringResource(R.string.read),
                                style = MaterialTheme.typography.button
                                    .let { b -> swatchInfo?.bodyColor?.let { b.copy(color = Color(it)) } ?: b },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            info?.downloadChapter(model, title.orEmpty())
                            check = true
                        },
                        modifier = Modifier
                            .weight(1f, true)
                            .padding(horizontal = 5.dp),
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(1.dp, swatchInfo?.bodyColor?.let { Color(it) } ?: Color.White)
                    ) {
                        Column {
                            Icon(
                                Icons.Default.Download,
                                "Download",
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                tint = swatchInfo?.bodyColor?.let { Color(it) } ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                            )
                            Text(
                                stringResource(R.string.download_chapter),
                                style = MaterialTheme.typography.button
                                    .let { b -> swatchInfo?.bodyColor?.let { b.copy(color = Color(it)) } ?: b },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }

                }

            }

        }

    }
}
