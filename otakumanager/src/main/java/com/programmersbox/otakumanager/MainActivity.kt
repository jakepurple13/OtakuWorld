package com.programmersbox.otakumanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.material.composethemeadapter.MdcTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.models.ApiService
import com.programmersbox.otakumanager.ui.theme.OtakuWorldTheme
import com.programmersbox.sharedutils.FirebaseAuthentication
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.CoverCard
import com.programmersbox.uiviews.utils.CustomChip
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject
import com.programmersbox.anime_sources.Sources as ASources
import com.programmersbox.manga_sources.Sources as MSources
import com.programmersbox.novel_sources.Sources as NSources

class MainActivity : ComponentActivity() {

    private val animeListener = FirebaseDb2("favoriteShows", "episodesWatched", "animeworld", "showUrl", "numEpisodes").FirebaseListener()
    private val mangaListener = FirebaseDb2("favoriteManga", "chaptersRead", "mangaworld", "mangaUrl", "chapterCount").FirebaseListener()
    private val novelListener = FirebaseDb2("favoriteNovels", "novelsChaptersRead", "novelworld", "novelUrl", "novelNumChapters").FirebaseListener()

    private val genericInfo by inject<GenericInfo>()

    private val allSources: List<ApiService> by lazy {
        genericInfo
            .sourceList()
            .sortedBy { it.serviceName }
    }

    private val animeSources = ASources.values().map { it.serviceName }
    private val mangaSources = MSources.values().map { it.serviceName }
    private val novelSources = NSources.values().map { it.serviceName }

    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MdcTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    MainUi()
                }
            }
        }
    }

    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    @Composable
    fun MainUi() {

        val focusManager = LocalFocusManager.current

        var searchText by remember { mutableStateOf("") }

        val favorites by Flowable.combineLatest(
            animeListener.getAllShowsFlowable(),
            mangaListener.getAllShowsFlowable()
        ) { a, m -> (a + m).sortedBy { it.title } }
            .map { it.filter { it.source in allSources.map { it.serviceName } } }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAsState(initial = emptyList())

        val selectedSources = remember { allSources.map { it.serviceName }.toMutableStateList() }

        val showing = favorites
            .filter { it.title.contains(searchText, true) && it.source in selectedSources }

        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier.padding(5.dp)
                ) {

                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text(resources.getQuantityString(R.plurals.numFavorites, showing.size, showing.size)) },
                        trailingIcon = { IconButton(onClick = { searchText = "" }) { Icon(Icons.Default.Cancel, null) } },
                        modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )

                    Row(
                        modifier = Modifier.padding(top = 5.dp)
                    ) {

                        LazyRow {

                            item {
                                CustomChip(
                                    "ALL",
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                selectedSources.clear()
                                                selectedSources.addAll(allSources.map { it.serviceName })
                                            },
                                            onLongClick = {
                                                selectedSources.clear()
                                            }
                                        ),
                                    backgroundColor = MaterialTheme.colors.primary,
                                    textColor = MaterialTheme.colors.onPrimary
                                )
                            }

                            items(
                                (allSources.map { it.serviceName } + showing.map { it.source })
                                    .groupBy { it }
                                    .toList()
                                    .sortedBy { it.first }
                            ) {

                                CustomChip(
                                    "${it.first}: ${it.second.size - 1}",
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                if (it.first in selectedSources) selectedSources.remove(it.first)
                                                else selectedSources.add(it.first)
                                            },
                                            onLongClick = {
                                                selectedSources.clear()
                                                selectedSources.add(it.first)
                                            }
                                        ),
                                    backgroundColor = animateColorAsState(if (it.first in selectedSources) MaterialTheme.colors.primary else MaterialTheme.colors.surface).value,
                                    textColor = animateColorAsState(if (it.first in selectedSources) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface).value
                                )
                            }
                        }

                    }
                }
            },
            bottomBar = {
                if (FirebaseAuthentication.currentUser == null) {
                    Button(
                        onClick = {
                            //if (FirebaseAuthentication.currentUser == null) {
                            //TODO: if not logged in, show empty state for needing to login
                            FirebaseAuthentication.signIn(this@MainActivity)
                            //}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0f)
                    ) {
                        Text("Login")
                    }
                }
            }
        ) {

            LazyVerticalGrid(
                cells = GridCells.Adaptive(ComposableUtils.IMAGE_WIDTH),
                contentPadding = it
            ) {
                items(
                    showing
                        .groupBy(DbModel::title)
                        .entries
                        .toTypedArray()
                ) { info ->
                    CoverCard(
                        imageUrl = info.value.random().imageUrl,
                        name = info.key,
                        placeHolder = when (info.value.first().source) {
                            in animeSources -> R.drawable.animeworld_logo
                            in mangaSources -> R.drawable.mangaworld_logo
                            in novelSources -> R.drawable.novelworld_logo
                            else -> R.drawable.ic_launcher_foreground
                        }
                    ) {

                        if (info.value.size == 1) {
                            //TODO: Open a modified details view here
                            //val item = info.value.firstOrNull()?.let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                            //navController.navigate(FavoriteFragmentDirections.actionFavoriteFragmentToDetailsFragment(item))
                        } else {
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle(R.string.chooseASource)
                                .setItems(info.value.map { "${it.source} - ${it.title}" }.toTypedArray()) { d, i ->
                                    //val item = info.value[i].let { genericInfo.toSource(it.source)?.let { it1 -> it.toItemModel(it1) } }
                                    //navController.navigate(FavoriteFragmentDirections.actionFavoriteFragmentToDetailsFragment(item))
                                    d.dismiss()
                                }
                                .show()
                        }

                    }
                }
            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        animeListener.unregister()
        mangaListener.unregister()
        novelListener.unregister()
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OtakuWorldTheme {
        Greeting("Android")
    }
}