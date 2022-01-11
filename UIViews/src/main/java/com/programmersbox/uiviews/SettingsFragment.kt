package com.programmersbox.uiviews

import android.Manifest
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.preference.*
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.programmersbox.favoritesdatabase.HistoryDatabase
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.ItemDatabase
import com.programmersbox.helpfulutils.notificationManager
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.models.ApiService
import com.programmersbox.models.sourcePublish
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.FirebaseAuthentication
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.sharedutils.appUpdateCheck
import com.programmersbox.uiviews.utils.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class SettingsFragment1 : PreferenceFragmentCompat() {

    private val disposable: CompositeDisposable = CompositeDisposable()

    private val genericInfo: GenericInfo by inject()

    private val logo: MainLogo by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        accountPreferences()
        generalPreferences(genericInfo)
        aboutPreferences(genericInfo)
        playPreferences()

        val settingsDsl = SettingsDsl()

        genericInfo.customPreferences(settingsDsl)
        settingsDsl.navigationSetup(this)
        findPreference<PreferenceCategory>("generalCategory")?.let { settingsDsl.generalSettings(this, it) }
        findPreference<PreferenceCategory>("viewCategory")?.let { settingsDsl.viewSettings(this, it) }
        findPreference<PreferenceCategory>("playCategory")?.let { settingsDsl.playSettings(this, it) }

        findPreference<Preference>("debugMenu")?.let { p ->
            p.isVisible = BuildConfig.DEBUG
            p.setOnPreferenceClickListener {
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToDebugFragment())
                true
            }
        }
    }

    private fun accountPreferences() {
        findPreference<Preference>("user_account")?.let { p ->

            fun accountChanges(user: FirebaseUser?) {
                activity?.let {
                    Glide.with(this@SettingsFragment1)
                        .load(user?.photoUrl)
                        .placeholder(logo.logoId)
                        .error(logo.logoId)
                        .fallback(logo.logoId)
                        .circleCrop()
                        .into<Drawable> { resourceReady { image, _ -> p.icon = image } }
                }
                p.title = user?.displayName ?: "User"
            }

            FirebaseAuthentication.auth.addAuthStateListener {
                accountChanges(it.currentUser)
                //findPreference<Preference>("upload_favorites")?.isEnabled = it.currentUser != null
                //findPreference<Preference>("upload_favorites")?.isVisible = it.currentUser != null
            }

            accountChanges(FirebaseAuthentication.currentUser)

            p.setOnPreferenceClickListener {
                FirebaseAuthentication.currentUser?.let {
                    MaterialAlertDialogBuilder(this@SettingsFragment1.requireContext())
                        .setTitle(R.string.logOut)
                        .setMessage(R.string.areYouSureLogOut)
                        .setPositiveButton(R.string.yes) { d, _ ->
                            FirebaseAuthentication.signOut()
                            d.dismiss()
                        }
                        .setNegativeButton(R.string.no) { d, _ -> d.dismiss() }
                        .show()
                } ?: FirebaseAuthentication.signIn(requireActivity())
                true
            }
        }
    }

    private fun generalPreferences(genericInfo: GenericInfo) {

        findPreference<PreferenceCategory>("aboutCategory")?.setIcon(logo.logoId)

        findPreference<Preference>("current_source")?.let { p ->
            p.setOnPreferenceClickListener {
                val list = genericInfo.sourceList().toTypedArray()
                val service = requireContext().currentService
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.chooseASource)
                    .setSingleChoiceItems(
                        list.map { it.serviceName }.toTypedArray(),
                        list.indexOfFirst { it.serviceName == service }
                    ) { d, i ->
                        sourcePublish.onNext(list[i])
                        requireContext().currentService = list[i].serviceName
                        d.dismiss()
                    }
                    .setPositiveButton(R.string.done) { d, _ -> d.dismiss() }
                    .show()
                true
            }
            sourcePublish.subscribe { p.title = getString(R.string.currentSource, it.serviceName) }
                .addTo(disposable)
        }

        findPreference<Preference>("view_source")?.let { p ->
            p.setOnPreferenceClickListener {
                requireContext().openInCustomChromeBrowser(sourcePublish.value!!.baseUrl) {
                    setStartAnimations(requireContext(), R.anim.slide_in_right, R.anim.slide_out_left)
                }
                true
            }
        }

        findPreference<Preference>("view_favorites")?.setOnPreferenceClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToFavoriteFragment())
            true
        }

        findPreference<Preference>("view_history")?.setOnPreferenceClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToHistoryFragment())
            true
        }

        findPreference<Preference>("view_global_search")?.setOnPreferenceClickListener {
            findNavController().navigate(GlobalNavDirections.showGlobalSearch())
            true
        }

        findPreference<ListPreference>("theme_setting")?.let { p ->
            p.setDefaultValue("system")
            p.setOnPreferenceChangeListener { _, newValue ->
                when (newValue) {
                    "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> null
                }?.let(AppCompatDelegate::setDefaultNightMode)
                true
            }

            when (p.value) {
                "system" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> null
            }?.let(AppCompatDelegate::setDefaultNightMode)
        }

        findPreference<SwitchPreferenceCompat>("share_chapter")?.let { p ->
            p.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Boolean) {
                    lifecycleScope.launch(Dispatchers.IO) { requireContext().updatePref(SHARE_CHAPTER, newValue) }
                }
                true
            }

            lifecycleScope.launch {
                requireContext().shareChapter
                    .flowWithLifecycle(lifecycle)
                    .collect { runOnUIThread { p.isChecked = it } }
            }
        }

        val itemDao = ItemDatabase.getInstance(requireContext()).itemDao()

        findPreference<PreferenceCategory>("notification_category")?.let { p ->
            itemDao.getAllNotificationCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { p.isVisible = it > 0 }
                .addTo(disposable)
        }

        findPreference<Preference>("saved_notifications")?.let { p ->
            itemDao.getAllNotificationCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { p.summary = getString(R.string.pending_saved_notifications, it) }
                .addTo(disposable)

            p.setOnPreferenceClickListener {
                //SavedNotifications.viewNotificationsFromDb(requireContext())
                findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToNotificationFragment())
                true
            }
        }

        findPreference<Preference>("delete_notifications")?.let { p ->
            p.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.are_you_sure_delete_notifications)
                    .setPositiveButton(R.string.yes) { d, _ ->
                        itemDao
                            .deleteAllNotifications()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .map { getString(R.string.deleted_notifications, it) }
                            .subscribeBy {
                                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                                requireContext().notificationManager.cancel(42)
                            }
                            .addTo(disposable)
                        d.dismiss()
                    }
                    .setNegativeButton(R.string.no) { d, _ -> d.dismiss() }
                    .show()
                true
            }
        }

    }

    private fun playPreferences() {
        findPreference<SeekBarPreference>("battery_alert")?.let { s ->
            s.showSeekBarValue = true
            s.setDefaultValue(20)
            s.max = 100
            s.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Int) {
                    lifecycleScope.launch(Dispatchers.IO) { requireContext().updatePref(BATTERY_PERCENT, newValue) }
                }
                true
            }
            lifecycleScope.launch {
                requireContext().batteryPercent
                    .flowWithLifecycle(lifecycle)
                    .collect { runOnUIThread { s.value = it } }
            }
        }
    }

    private fun aboutPreferences(genericInfo: GenericInfo) {
        val checker = AtomicBoolean(false)
        fun updateSetter() {
            if (!checker.get()) {
                Single.create<AppUpdate.AppUpdates> {
                    checker.set(true)
                    AppUpdate.getUpdate()?.let { d -> it.onSuccess(d) } ?: it.onError(Exception("Something went wrong"))
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError { }
                    .subscribeBy {
                        appUpdateCheck.onNext(it)
                        checker.set(false)
                        context?.let { c -> Toast.makeText(c, "Done Checking", Toast.LENGTH_SHORT).show() }
                    }
                    .addTo(disposable)
            }
        }

        findPreference<Preference>("about_version")?.let { p ->
            p.title =
                getString(R.string.currentVersion, context?.packageManager?.getPackageInfo(requireContext().packageName, 0)?.versionName.orEmpty())
            p.setOnPreferenceClickListener {
                updateSetter()
                true
            }
        }

        findPreference<Preference>("reset_checker")?.let { p ->
            p.setOnPreferenceClickListener {
                val work = WorkManager.getInstance(this.requireContext())
                work.cancelUniqueWork("updateChecks")
                work.pruneWork()
                OtakuApp.updateSetup(requireContext())
                Toast.makeText(requireContext(), R.string.cleared, Toast.LENGTH_SHORT).show()
                true
            }
        }

        findPreference<Preference>("view_on_github")?.setOnPreferenceClickListener {
            context?.openInCustomChromeBrowser(otakuWorldGithubUrl)
            true
        }

        findPreference<Preference>("updateAvailable")?.let { p ->
            p.isVisible = false
            appUpdateCheck
                .subscribe {
                    p.summary = getString(R.string.currentVersion, it.update_real_version.orEmpty())
                    val appVersion = AppUpdate.checkForUpdate(
                        context?.packageManager?.getPackageInfo(requireContext().packageName, 0)?.versionName.orEmpty(),
                        it.update_real_version.orEmpty()
                    )
                    p.isVisible = appVersion
                }
                .addTo(disposable)

            p.setOnPreferenceClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.updateTo, p.summary))
                    .setMessage(R.string.please_update_for_latest_features)
                    .setPositiveButton(R.string.update) { d, _ ->
                        activity?.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE) {
                            if (it.isGranted) {
                                appUpdateCheck.value
                                    ?.let { a ->
                                        val isApkAlreadyThere = File(
                                            context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/",
                                            a.let(genericInfo.apkString).toString()
                                        )
                                        if (isApkAlreadyThere.exists()) isApkAlreadyThere.delete()
                                        DownloadUpdate(requireContext(), requireContext().packageName).downloadUpdate(a)
                                    }
                            }
                        }
                        d.dismiss()
                    }
                    .setNeutralButton(R.string.gotoBrowser) { d, _ ->
                        context?.openInCustomChromeBrowser("https://github.com/jakepurple13/OtakuWorld/releases/latest")
                        d.dismiss()
                    }
                    .setNegativeButton(R.string.notNow) { d, _ -> d.dismiss() }
                    .show()

                true
            }
        }

        findPreference<Preference>("sync_time")?.let { s ->

            listOfNotNull(
                requireContext().lastUpdateCheck
                    ?.let { "Start: ${requireContext().getSystemDateTimeFormat().format(it)}" },
                requireContext().lastUpdateCheckEnd
                    ?.let { "End: ${requireContext().getSystemDateTimeFormat().format(it)}" }
            )
                .joinToString("\n")
                .let { s.summary = it }

            Observables.combineLatest(
                updateCheckPublish.map { "Start: ${requireContext().getSystemDateTimeFormat().format(it)}" },
                updateCheckPublishEnd.map { "End: ${requireContext().getSystemDateTimeFormat().format(it)}" }
            )
                .map { "${it.first}\n${it.second}" }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { s.summary = it }
                .addTo(disposable)

            s.setOnPreferenceClickListener {
                WorkManager.getInstance(this.requireContext())
                    .enqueueUniqueWork(
                        "oneTimeUpdate",
                        ExistingWorkPolicy.KEEP,
                        OneTimeWorkRequestBuilder<UpdateWorker>()
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                    .setRequiresBatteryNotLow(false)
                                    .setRequiresCharging(false)
                                    .setRequiresDeviceIdle(false)
                                    .setRequiresStorageNotLow(false)
                                    .build()
                            )
                            .build()
                    )
                true
            }
        }

        findPreference<SwitchPreferenceCompat>("sync")?.let { s ->
            s.setDefaultValue(requireContext().shouldCheck)
            s.setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Boolean) {
                    requireContext().shouldCheck = newValue
                    OtakuApp.updateSetup(requireContext())
                }
                true
            }
        }

        findPreference<Preference>("used_libraries")?.setOnPreferenceClickListener {
            findNavController()
                .navigate(
                    SettingsFragmentDirections.actionXToAboutLibs(
                        LibsBuilder()
                            .withSortEnabled(true)
                            .customUtils("loggingutils", "LoggingUtils")
                            //.customUtils("flowutils", "FlowUtils")
                            .customUtils("gsonutils", "GsonUtils")
                            .customUtils("helpfulutils", "HelpfulUtils")
                            .customUtils("dragswipe", "DragSwipe")
                            .customUtils("funutils", "FunUtils")
                            .customUtils("rxutils", "RxUtils")
                            //.customUtils("thirdpartyutils", "ThirdPartyUtils")
                            .withShowLoadingProgress(true)
                    )
                )
            true
        }

    }

    private fun LibsBuilder.customUtils(libraryName: String, newName: String) =
        withLibraryModification(
            libraryName,
            Libs.LibraryFields.LIBRARY_REPOSITORY_LINK,
            "https://www.github.com/jakepurple13/HelpfulTools"
        )
            .withLibraryModification(
                libraryName,
                Libs.LibraryFields.LIBRARY_NAME,
                newName
            )

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}

class SettingsDsl {
    internal var generalSettings: (SettingsFragment1, PreferenceCategory) -> Unit = { _, _ -> }

    fun generalSettings(block: (SettingsFragment1, PreferenceCategory) -> Unit) {
        generalSettings = block
    }

    internal var viewSettings: (SettingsFragment1, PreferenceCategory) -> Unit = { _, _ -> }

    fun viewSettings(block: (SettingsFragment1, PreferenceCategory) -> Unit) {
        viewSettings = block
    }

    internal var navigationSetup: (SettingsFragment1) -> Unit = {}

    fun navigationSetup(block: (SettingsFragment1) -> Unit) {
        navigationSetup = block
    }

    internal var playSettings: (SettingsFragment1, PreferenceCategory) -> Unit = { _, _ -> }

    fun playSettings(block: (SettingsFragment1, PreferenceCategory) -> Unit) {
        playSettings = block
    }

    companion object {
        val customAnimationOptions = navOptions {
            anim {
                enter = R.anim.slide_in
                exit = R.anim.fade_out
                popEnter = R.anim.fade_in
                popExit = R.anim.slide_out
            }
        }
    }
}

class SettingsFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }

    private val genericInfo: GenericInfo by inject()
    private val logo: MainLogo by inject()

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalComposeUiApi::class
    )
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(currentColorScheme) {
                    SettingScreen(
                        navController = findNavController(),
                        logo = logo,
                        genericInfo = genericInfo,
                        fragment = this@SettingsFragment,
                        activity = requireActivity(),
                        usedLibraryClick = {
                            findNavController()
                                .navigate(
                                    SettingsFragmentDirections.actionXToAboutLibs(
                                        LibsBuilder()
                                            .withSortEnabled(true)
                                            .customUtils("loggingutils", "LoggingUtils")
                                            //.customUtils("flowutils", "FlowUtils")
                                            .customUtils("gsonutils", "GsonUtils")
                                            .customUtils("helpfulutils", "HelpfulUtils")
                                            .customUtils("dragswipe", "DragSwipe")
                                            .customUtils("funutils", "FunUtils")
                                            .customUtils("rxutils", "RxUtils")
                                            //.customUtils("thirdpartyutils", "ThirdPartyUtils")
                                            .withShowLoadingProgress(true)
                                    )
                                )
                        },
                        debugMenuClick = {
                            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToDebugFragment())
                        },
                        notificationClick = {
                            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToNotificationFragment())
                        },
                        favoritesClick = {
                            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToFavoriteFragment())
                        },
                        historyClick = {
                            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToHistoryFragment())
                        },
                        globalSearchClick = {
                            findNavController().navigate(GlobalNavDirections.showGlobalSearch())
                        }
                    )
                }
            }
        }

    private fun LibsBuilder.customUtils(libraryName: String, newName: String) =
        withLibraryModification(
            libraryName,
            Libs.LibraryFields.LIBRARY_REPOSITORY_LINK,
            "https://www.github.com/jakepurple13/HelpfulTools"
        )
            .withLibraryModification(
                libraryName,
                Libs.LibraryFields.LIBRARY_NAME,
                newName
            )
}

class ComposeSettingsDsl {

    internal var generalSettings: (@Composable () -> Unit)? = null
    internal var viewSettings: (@Composable () -> Unit)? = null
    internal var playerSettings: (@Composable () -> Unit)? = null

    fun generalSettings(block: @Composable () -> Unit) {
        generalSettings = block
    }

    fun viewSettings(block: @Composable () -> Unit) {
        viewSettings = block
    }

    fun playerSettings(block: @Composable () -> Unit) {
        playerSettings = block
    }

}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@Composable
fun SettingScreen(
    navController: NavController,
    logo: MainLogo,
    genericInfo: GenericInfo,
    fragment: Fragment,
    activity: ComponentActivity,
    usedLibraryClick: () -> Unit = {},
    debugMenuClick: () -> Unit = {},
    notificationClick: () -> Unit = {},
    favoritesClick: () -> Unit = {},
    historyClick: () -> Unit = {},
    globalSearchClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val customPreferences = remember { ComposeSettingsDsl().apply(genericInfo.composeCustomPreferences(navController)) }

    val scrollBehavior = remember { TopAppBarDefaults.pinnedScrollBehavior() }
    val listState = rememberScrollState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SmallTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { p ->
        Column(
            modifier = Modifier
                .verticalScroll(listState)
                .padding(p)
                .padding(bottom = 10.dp)
        ) {

            /*Account*/
            AccountSettings(
                context = context,
                activity = activity,
                logo = logo
            )

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

            /*About*/
            AboutSettings(
                context = context,
                scope = scope,
                activity = activity,
                genericInfo = genericInfo,
                logo = logo,
                usedLibraryClick = usedLibraryClick
            )

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

            /*Notifications*/
            NotificationSettings(
                context = context,
                scope = scope,
                notificationClick = notificationClick
            )

            /*View*/
            ViewSettings(
                customSettings = customPreferences.viewSettings,
                debugMenuClick = debugMenuClick,
                favoritesClick = favoritesClick,
                historyClick = historyClick
            )

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

            /*General*/
            GeneralSettings(
                context = context,
                scope = scope,
                activity = activity,
                genericInfo = genericInfo,
                customSettings = customPreferences.generalSettings,
                globalSearchClick = globalSearchClick
            )

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

            /*Player*/
            PlaySettings(
                context = context,
                scope = scope,
                customSettings = customPreferences.playerSettings
            )

        }
    }

}

class AccountViewModel : ViewModel() {

    var accountInfo by mutableStateOf<FirebaseUser?>(null)

    private val listener: FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener { p0 -> accountInfo = p0.currentUser }

    init {
        FirebaseAuthentication.auth.addAuthStateListener(listener)
    }

    fun signInOrOut(context: Context, activity: ComponentActivity) {
        FirebaseAuthentication.currentUser?.let {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.logOut)
                .setMessage(R.string.areYouSureLogOut)
                .setPositiveButton(R.string.yes) { d, _ ->
                    FirebaseAuthentication.signOut()
                    d.dismiss()
                }
                .setNegativeButton(R.string.no) { d, _ -> d.dismiss() }
                .show()
        } ?: FirebaseAuthentication.signIn(activity)
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuthentication.auth.removeAuthStateListener(listener)
    }
}

@ExperimentalMaterialApi
@Composable
private fun AccountSettings(context: Context, activity: ComponentActivity, logo: MainLogo) {
    CategorySetting { Text(stringResource(R.string.account_category_title)) }

    val viewModel: AccountViewModel = viewModel()
    val accountInfo = viewModel.accountInfo

    PreferenceSetting(
        settingTitle = { Text(accountInfo?.displayName ?: "User") },
        settingIcon = {
            Image(
                painter = rememberImagePainter(data = accountInfo?.photoUrl) {
                    error(logo.logoId)
                    crossfade(true)
                    lifecycle(LocalLifecycleOwner.current)
                    transformations(CircleCropTransformation())
                },
                contentDescription = null
            )
        },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() }
        ) { viewModel.signInOrOut(context, activity) }
    )
}

class AboutViewModel : ViewModel() {

    var canCheck by mutableStateOf(false)

    fun init(context: Context) {
        viewModelScope.launch { context.shouldCheckFlow.collect { canCheck = it } }
    }

}

@ExperimentalMaterialApi
@Composable
private fun AboutSettings(
    context: Context,
    scope: CoroutineScope,
    activity: ComponentActivity,
    genericInfo: GenericInfo,
    logo: MainLogo,
    usedLibraryClick: () -> Unit
) {
    CategorySetting(
        settingTitle = { Text(stringResource(R.string.about)) },
        settingIcon = {
            Image(
                bitmap = AppCompatResources.getDrawable(context, logo.logoId)!!.toBitmap().asImageBitmap(),
                null,
                modifier = Modifier.fillMaxSize()
            )
        }
    )

    val appUpdate by appUpdateCheck.subscribeAsState(null)

    PreferenceSetting(
        settingTitle = {
            Text(
                stringResource(
                    R.string.currentVersion,
                    context.packageManager.getPackageInfo(context.packageName, 0)?.versionName.orEmpty()
                )
            )
        }
    )

    ShowWhen(
        visibility = AppUpdate.checkForUpdate(
            context.packageManager.getPackageInfo(context.packageName, 0)?.versionName.orEmpty(),
            appUpdate?.update_real_version.orEmpty()
        )
    ) {
        var showDialog by remember { mutableStateOf(false) }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(R.string.updateTo, appUpdate?.update_real_version.orEmpty())) },
                text = { Text(stringResource(R.string.please_update_for_latest_features)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            activity.requestPermissions(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            ) {
                                if (it.isGranted) {
                                    appUpdateCheck.value
                                        ?.let { a ->
                                            val isApkAlreadyThere = File(
                                                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/",
                                                a.let(genericInfo.apkString).toString()
                                            )
                                            if (isApkAlreadyThere.exists()) isApkAlreadyThere.delete()
                                            DownloadUpdate(context, context.packageName).downloadUpdate(a)
                                        }
                                }
                            }
                            showDialog = false
                        }
                    ) { Text(stringResource(R.string.update)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.notNow)) }
                    TextButton(
                        onClick = {
                            context.openInCustomChromeBrowser("https://github.com/jakepurple13/OtakuWorld/releases/latest")
                            showDialog = false
                        }
                    ) { Text(stringResource(R.string.gotoBrowser)) }
                }
            )
        }

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.update_available)) },
            summaryValue = { Text(stringResource(R.string.updateTo, appUpdate?.update_real_version.orEmpty())) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { showDialog = true },
            settingIcon = {
                Icon(
                    Icons.Default.SystemUpdateAlt,
                    null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }

    val time by Observables.combineLatest(
        updateCheckPublish.map { "Start: ${context.getSystemDateTimeFormat().format(it)}" },
        updateCheckPublishEnd.map { "End: ${context.getSystemDateTimeFormat().format(it)}" }
    )
        .map { "${it.first}\n${it.second}" }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeAsState(
            listOfNotNull(
                context.lastUpdateCheck?.let { "Start: ${context.getSystemDateTimeFormat().format(it)}" },
                context.lastUpdateCheckEnd?.let { "End: ${context.getSystemDateTimeFormat().format(it)}" }
            ).joinToString("\n")
        )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.last_update_check_time)) },
        summaryValue = { Text(time) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() }
        ) {
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "oneTimeUpdate",
                    ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<UpdateWorker>()
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                .setRequiresBatteryNotLow(false)
                                .setRequiresCharging(false)
                                .setRequiresDeviceIdle(false)
                                .setRequiresStorageNotLow(false)
                                .build()
                        )
                        .build()
                )
        }
    )

    val aboutViewModel: AboutViewModel = viewModel()
    LaunchedEffect(Unit) { aboutViewModel.init(context) }

    SwitchSetting(
        settingTitle = { Text(stringResource(R.string.check_for_periodic_updates)) },
        value = aboutViewModel.canCheck,
        updateValue = {
            scope.launch { context.updatePref(SHOULD_CHECK, it) }
            OtakuApp.updateSetup(context)
        }
    )

    ShowWhen(aboutViewModel.canCheck) {
        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.clear_update_queue)) },
            summaryValue = { Text(stringResource(R.string.clear_update_queue_summary)) },
            modifier = Modifier
                .alpha(if (aboutViewModel.canCheck) 1f else .38f)
                .clickable(
                    enabled = aboutViewModel.canCheck,
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    val work = WorkManager.getInstance(context)
                    work.cancelUniqueWork("updateChecks")
                    work.pruneWork()
                    OtakuApp.updateSetup(context)
                    Toast
                        .makeText(context, R.string.cleared, Toast.LENGTH_SHORT)
                        .show()
                }
        )
    }

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.view_libraries_used)) },
        settingIcon = { Icon(Icons.Default.LibraryBooks, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() },
            onClick = usedLibraryClick
        )
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.view_on_github)) },
        settingIcon = { Icon(painterResource(R.drawable.github_icon), null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() }
        ) { context.openInCustomChromeBrowser("https://github.com/jakepurple13/OtakuWorld/releases/latest") }
    )

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.join_discord)) },
        settingIcon = { Icon(painterResource(R.drawable.ic_baseline_discord_24), null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() }
        ) { context.openInCustomChromeBrowser("https://discord.gg/MhhHMWqryg") }
    )
}

class NotificationViewModel(dao: ItemDao) : ViewModel() {

    var savedNotifications by mutableStateOf(0)
        private set

    private val sub = dao.getAllNotificationCount()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeBy { savedNotifications = it }

    override fun onCleared() {
        super.onCleared()
        sub.dispose()
    }

}

@ExperimentalMaterialApi
@Composable
private fun NotificationSettings(
    context: Context,
    scope: CoroutineScope,
    notificationClick: () -> Unit
) {
    val dao = remember { ItemDatabase.getInstance(context).itemDao() }
    val notiViewModel: NotificationViewModel = viewModel(factory = factoryCreate { NotificationViewModel(dao = dao) })

    ShowWhen(notiViewModel.savedNotifications > 0) {
        CategorySetting { Text(stringResource(R.string.notifications_category_title)) }

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.view_notifications_title)) },
            summaryValue = { Text(stringResource(R.string.pending_saved_notifications, notiViewModel.savedNotifications)) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() },
                onClick = notificationClick
            )
        )

        var showDialog by remember { mutableStateOf(false) }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(R.string.are_you_sure_delete_notifications)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val number = dao.deleteAllNotificationsFlow()
                                launch(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.deleted_notifications, number),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    context.notificationManager.cancel(42)
                                }
                            }
                            showDialog = false
                        }
                    ) { Text(stringResource(R.string.yes)) }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.no)) } }
            )
        }

        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.delete_saved_notifications_title)) },
            summaryValue = { Text(stringResource(R.string.delete_notifications_summary)) },
            modifier = Modifier
                .clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) { showDialog = true }
                .padding(bottom = 16.dp, top = 8.dp)
        )

        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    }
}

@ExperimentalMaterialApi
@Composable
private fun ViewSettings(
    customSettings: (@Composable () -> Unit)?,
    debugMenuClick: () -> Unit,
    favoritesClick: () -> Unit,
    historyClick: () -> Unit
) {
    CategorySetting { Text(stringResource(R.string.view_menu_category_title)) }

    if (BuildConfig.DEBUG) {
        PreferenceSetting(
            settingTitle = { Text("Debug Menu") },
            settingIcon = { Icon(Icons.Default.Android, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() },
                onClick = debugMenuClick
            )
        )
    }

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.viewFavoritesMenu)) },
        settingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() },
            onClick = favoritesClick
        )
    )

    val context = LocalContext.current
    val dao = remember { HistoryDatabase.getInstance(context).historyDao() }
    val historyCount by dao.getAllRecentHistoryCount().collectAsState(initial = 0)

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.history)) },
        summaryValue = { Text(historyCount.toString()) },
        settingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() },
            onClick = historyClick
        )
    )

    customSettings?.invoke()
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
private fun GeneralSettings(
    context: Context,
    scope: CoroutineScope,
    activity: ComponentActivity,
    genericInfo: GenericInfo,
    customSettings: (@Composable () -> Unit)?,
    globalSearchClick: () -> Unit
) {
    CategorySetting { Text(stringResource(R.string.general_menu_title)) }

    val source: ApiService? by sourcePublish.subscribeAsState(initial = null)

    DynamicListSetting(
        settingTitle = { Text(stringResource(R.string.currentSource, source?.serviceName.orEmpty())) },
        dialogTitle = { Text(stringResource(R.string.chooseASource)) },
        settingIcon = { Icon(Icons.Default.Source, null, modifier = Modifier.fillMaxSize()) },
        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(R.string.done)) } },
        value = source,
        options = { genericInfo.sourceList() },
        updateValue = { service, d ->
            service?.let {
                sourcePublish.onNext(it)
                context.currentService = it.serviceName
            }
            d.value = false
        }
    )

    ShowWhen(source != null) {
        PreferenceSetting(
            settingTitle = { Text(stringResource(R.string.view_source_in_browser)) },
            settingIcon = { Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.fillMaxSize()) },
            modifier = Modifier.clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                source?.baseUrl?.let {
                    context.openInCustomChromeBrowser(it) {
                        setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left)
                    }
                }
            }
        )
    }

    PreferenceSetting(
        settingTitle = { Text(stringResource(R.string.global_search)) },
        settingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.fillMaxSize()) },
        modifier = Modifier.clickable(
            indication = rememberRipple(),
            interactionSource = remember { MutableInteractionSource() },
            onClick = globalSearchClick
        )
    )

    val theme by activity.themeSetting.collectAsState(initial = "System")

    ListSetting(
        settingTitle = { Text(stringResource(R.string.theme_choice_title)) },
        dialogIcon = { Icon(Icons.Default.SettingsBrightness, null) },
        settingIcon = { Icon(Icons.Default.SettingsBrightness, null, modifier = Modifier.fillMaxSize()) },
        dialogTitle = { Text(stringResource(R.string.choose_a_theme)) },
        summaryValue = { Text(theme) },
        confirmText = { TextButton(onClick = { it.value = false }) { Text(stringResource(R.string.cancel)) } },
        value = theme,
        options = listOf("System", "Light", "Dark"),
        updateValue = { it, d ->
            d.value = false
            scope.launch { context.updatePref(THEME_SETTING, it) }
            when (it) {
                "System" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                "Light" -> AppCompatDelegate.MODE_NIGHT_NO
                "Dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> null
            }?.let(AppCompatDelegate::setDefaultNightMode)
        }
    )

    val shareChapter by context.shareChapter.collectAsState(initial = true)

    SwitchSetting(
        settingTitle = { Text(stringResource(R.string.share_chapters)) },
        settingIcon = { Icon(Icons.Default.Share, null, modifier = Modifier.fillMaxSize()) },
        value = shareChapter,
        updateValue = { scope.launch { context.updatePref(SHARE_CHAPTER, it) } }
    )

    val showAllScreen by context.showAll.collectAsState(initial = true)

    SwitchSetting(
        settingTitle = { Text(stringResource(R.string.show_all_screen)) },
        settingIcon = { Icon(Icons.Default.Menu, null, modifier = Modifier.fillMaxSize()) },
        value = showAllScreen,
        updateValue = { scope.launch { context.updatePref(SHOW_ALL, it) } }
    )

    customSettings?.invoke()
}

@ExperimentalMaterialApi
@Composable
private fun PlaySettings(context: Context, scope: CoroutineScope, customSettings: (@Composable () -> Unit)?) {
    CategorySetting { Text(stringResource(R.string.playSettings)) }

    var sliderValue by remember { mutableStateOf(runBlocking { context.batteryPercent.first().toFloat() }) }

    SliderSetting(
        sliderValue = sliderValue,
        settingTitle = { Text(stringResource(R.string.battery_alert_percentage)) },
        settingSummary = { Text(stringResource(R.string.battery_default)) },
        settingIcon = { Icon(Icons.Default.BatteryAlert, null) },
        range = 1f..100f,
        updateValue = {
            sliderValue = it
            scope.launch { context.updatePref(BATTERY_PERCENT, sliderValue.toInt()) }
        }
    )

    customSettings?.invoke()
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@Composable
fun <T> DynamicListSetting(
    modifier: Modifier = Modifier,
    settingIcon: (@Composable BoxScope.() -> Unit)? = null,
    summaryValue: (@Composable () -> Unit)? = null,
    settingTitle: @Composable () -> Unit,
    dialogTitle: @Composable () -> Unit,
    dialogIcon: (@Composable () -> Unit)? = null,
    cancelText: (@Composable (MutableState<Boolean>) -> Unit)? = null,
    confirmText: @Composable (MutableState<Boolean>) -> Unit,
    radioButtonColors: RadioButtonColors = RadioButtonDefaults.colors(),
    value: T,
    options: () -> List<T>,
    viewText: (T) -> String = { it.toString() },
    updateValue: (T, MutableState<Boolean>) -> Unit
) {
    val dialogPopup = remember { mutableStateOf(false) }

    if (dialogPopup.value) {

        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { dialogPopup.value = false },
            title = dialogTitle,
            icon = dialogIcon,
            text = {
                LazyColumn {
                    items(options()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = rememberRipple(),
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { updateValue(it, dialogPopup) }
                                .border(0.dp, Color.Transparent, RoundedCornerShape(20.dp))
                        ) {
                            RadioButton(
                                selected = it == value,
                                onClick = { updateValue(it, dialogPopup) },
                                modifier = Modifier.padding(8.dp),
                                colors = radioButtonColors
                            )
                            Text(
                                viewText(it),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = { confirmText(dialogPopup) },
            dismissButton = cancelText?.let { { it(dialogPopup) } }
        )

    }

    PreferenceSetting(
        settingTitle = settingTitle,
        summaryValue = summaryValue,
        settingIcon = settingIcon,
        modifier = Modifier
            .clickable(
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            ) { dialogPopup.value = true }
            .then(modifier)
    )
}