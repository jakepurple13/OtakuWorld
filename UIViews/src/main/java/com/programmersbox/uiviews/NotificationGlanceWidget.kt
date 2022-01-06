package com.programmersbox.uiviews

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.programmersbox.favoritesdatabase.NotificationItem
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.gsonutils.toJson
import com.programmersbox.helpfulutils.nextColor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random


//TODO: Try adding the glance widget to show notifications and clicking on them opens the app to the details screen

var notiPublish: List<NotificationItem> by mutableStateOf(emptyList())

class FirstGlanceWidget : GlanceAppWidget(), KoinComponent {

    @Composable
    override fun Content() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(day = Color.White, night = Color.DarkGray)
                .appWidgetBackground()
                .cornerRadius(16.dp)
                .padding(8.dp)
        ) {
            val context = LocalContext.current
            Text(context.getString(R.string.current_notification_count, notiPublish.size))

            LazyColumn {
                itemsIndexed(notiPublish) { index, item ->
                    /*Surface(
                        onClick = {},
                        tonalElevation = 5.dp,
                        indication = rememberRipple(),
                        onClickLabel = item.notiTitle,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    ) {*/

                    Row(
                        modifier = GlanceModifier
                            .cornerRadius(6.dp)
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(remember { Color(Random.nextColor()) })
                            .clickable(
                                actionRunCallback<NotificationClickAction>(
                                    actionParametersOf(notificationKey to item.toJson())
                                )
                            )
                    ) {
                        /*item.imageUrl?.toUri()?.let { ImageProvider(it) }?.let {
                            Image(
                                provider = it*//*rememberImagePainter(item.imageUrl) {
                                                            //placeholder(AppCompatResources.getDrawable(context, placeHolder)!!)
                                                            //error(AppCompatResources.getDrawable(context, error)!!)
                                                            crossfade(true)
                                                            lifecycle(LocalLifecycleOwner.current)
                                                            size(480, 360)
                                                        }*//*,
                                contentDescription = item.notiTitle,
                                modifier = GlanceModifier
                                    .padding(5.dp)
                                    .size(ComposableUtils.IMAGE_WIDTH, ComposableUtils.IMAGE_HEIGHT)
                            )
                        }*/

                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()//weight(1f)
                                .padding(start = 16.dp, top = 4.dp)
                        ) {
                            //Text(item)
                            //M3MaterialTheme.typography.labelMedium
                            Text(
                                item.source,
                                style = TextStyle(fontWeight = FontWeight.Bold)
                            )
                            //, style = M3MaterialTheme.typography.titleSmall
                            Text(
                                item.notiTitle,
                                style = TextStyle(fontWeight = FontWeight.Bold)
                            )
                            //, style = M3MaterialTheme.typography.bodyMedium
                            /*Text(
                                item.summaryText,
                                style = TextStyle(fontWeight = FontWeight.Bold)
                            )*/
                        }
                    }
                }
            }
        }
    }
}

class FirstGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FirstGlanceWidget()
}

private val notificationKey = ActionParameters.Key<String>("notification_key")

class NotificationClickAction : ActionCallback, KoinComponent {

    val genericInfo: GenericInfo by inject()

    override suspend fun onRun(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val item = parameters[notificationKey]?.fromJson<NotificationItem>()
        println(item)
        item?.let {
            /*actionStartActivity<BaseMainActivity>(
                parameters = androidx.glance.appwidget.action.actionStartActivity()
            )*/
            /*val itemModel = genericInfo.toSource(item.source)
                ?.getSourceByUrl(item.url)
                ?.onErrorReturn { null }
                ?.blockingGet()*/

            /*NavDeepLinkBuilder(context)
                .setGraph(R.navigation.recent_nav)
                .setDestination(R.id.detailsFragment2)
                .setArguments(Bundle().apply { putSerializable("itemInfo", itemModel) })
                .createPendingIntent()*/

            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(context.packageName)
                ?.apply {
                    data = item.url.toUri()
                }

            intent?.let { it1 -> context.startActivity(it1) }
            /*genericInfo.toSource(it.source)
                ?.getSourceByUrl(item.url)
                ?.subscribeOn(Schedulers.io())
                ?.doOnError { context.showErrorToast() }
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.blockingGet()*/
        }

    }
}

/**
 * Glance widget that showcases how to use:
 * - Compound buttons
 * - LazyColumn
 * - State management using GlanceStateDefinition
 *//*

class TodoListGlanceWidget : GlanceAppWidget() {

    override var stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun Content() {
        MaterialTheme.colors.background
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(day = Color.White, night = Color.DarkGray)
                .appWidgetBackground()
                .cornerRadius(16.dp)
                .padding(8.dp)
        ) {
            val context = LocalContext.current
            val prefs = currentState<Preferences>()
            Text(
                text = context.getString(R.string.glance_todo_list),
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(8.dp),
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
            )
            CountChecked()
            LazyColumn {
                items(groceryStringIds) {
                    val idString = it.toString()
                    val checked = prefs[booleanPreferencesKey(idString)] ?: false
                    CheckBox(
                        text = context.getString(it),
                        checked = checked,
                        onCheckedChange = actionRunCallback<CheckboxClickAction>(actionParametersOf(toggledStringIdKey to idString,)),
                        modifier = GlanceModifier.padding(12.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun CountChecked() {
    val prefs = currentState<Preferences>()
    val checkedCount = groceryStringIds.filter {
        prefs[booleanPreferencesKey(it.toString())] ?: false
    }.size

    Text(
        text = "$checkedCount checkboxes checked",
        modifier = GlanceModifier.padding(start = 8.dp)
    )
}

private val toggledStringIdKey = ActionParameters.Key<String>("ToggledStringIdKey")

class CheckboxClickAction : ActionCallback {
    override suspend fun onRun(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val toggledStringId = requireNotNull(parameters[toggledStringIdKey]) {
            "Add $toggledStringIdKey parameter in the ActionParameters."
        }

        // The checked state of the clicked checkbox can be added implicitly to the parameters and
        // can be retrieved by using the ToggleableStateKey
        val checked = requireNotNull(parameters[ToggleableStateKey]) {
            "This action should only be called in response to toggleable events"
        }
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {
            it.toMutablePreferences()
                .apply { this[booleanPreferencesKey(toggledStringId)] = checked }
        }
        TodoListGlanceWidget().update(context, glanceId)
    }
}

class TodoListGlanceWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = TodoListGlanceWidget()
}*/
