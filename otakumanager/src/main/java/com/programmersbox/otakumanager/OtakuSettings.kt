package com.programmersbox.otakumanager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alorma.settings.composables.SettingsGroup
import com.alorma.settings.composables.SettingsMenuLink
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseUser
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.loggingutils.Loged
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.FirebaseAuthentication
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.sharedutils.appUpdateCheck
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.openInCustomChromeBrowser
import com.skydoves.landscapist.glide.GlideImage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.coroutines.CoroutineContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore("otakusettings")
val THEME_SETTING = stringPreferencesKey("theme")

@ExperimentalAnimationApi
@Composable
fun OtakuSettings(activity: ComponentActivity, genericInfo: GenericInfo) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .padding(5.dp)
            .verticalScroll(rememberScrollState())
    ) {


        //add scaffold and the title will be settings
        val scope = rememberCoroutineScope()
        val disposable = remember { CompositeDisposable() }

        var accountInfo by remember { mutableStateOf<FirebaseUser?>(null) }
        val packageManager = LocalContext.current.packageManager

        LaunchedEffect(Unit) { FirebaseAuthentication.auth.addAuthStateListener { accountInfo = it.currentUser } }

        DisposableEffect(accountInfo) {
            onDispose { disposable.dispose() }
        }

        SettingsGroup(
            title = { Text(text = stringResource(id = R.string.account_category_title), modifier = Modifier.padding(start = 5.dp)) }
        ) {

            SettingsMenuLink(
                icon = {
                    GlideImage(
                        imageModel = accountInfo?.photoUrl ?: R.drawable.otakumanager_logo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        requestBuilder = { Glide.with(LocalView.current).asDrawable().circleCrop() },
                    )
                },
                title = { Text(text = accountInfo?.displayName ?: "User", modifier = Modifier.padding(start = 5.dp)) },
                onClick = {
                    FirebaseAuthentication.currentUser?.let {
                        MaterialAlertDialogBuilder(activity)
                            .setTitle(R.string.logOut)
                            .setMessage(R.string.areYouSureLogOut)
                            .setPositiveButton(R.string.yes) { d, _ ->
                                FirebaseAuthentication.signOut()
                                d.dismiss()
                            }
                            .setNegativeButton(R.string.no) { d, _ -> d.dismiss() }
                            .show()
                    } ?: FirebaseAuthentication.signIn(activity)
                },
            )

        }

        SettingsGroup(
            title = { Text(text = stringResource(id = R.string.about), modifier = Modifier.padding(start = 5.dp)) }
        ) {

            val appUpdate by appUpdateCheck
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeAsState(initial = null)

            val currentAppInfo = rememberSaveable { packageManager?.getPackageInfo(activity.packageName, 0)?.versionName.orEmpty() }

            SettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.currentVersion, currentAppInfo), modifier = Modifier.padding(start = 5.dp)) },
                subtitle = { Text(text = stringResource(id = R.string.press_to_check_for_updates), modifier = Modifier.padding(start = 5.dp)) },
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        AppUpdate.getUpdate()?.let(appUpdateCheck::onNext)
                        launch(Dispatchers.Main) { Toast.makeText(activity, "Done Checking", Toast.LENGTH_SHORT).show() }
                    }
                }
            )

            AnimatedVisibility(visible = AppUpdate.checkForUpdate(currentAppInfo, appUpdate?.update_real_version.orEmpty())) {

                val update: () -> Unit = {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle(
                            activity.getString(
                                R.string.updateTo,
                                activity.getString(R.string.currentVersion, appUpdate?.update_real_version.orEmpty())
                            )
                        )
                        .setMessage(R.string.please_update_for_latest_features)
                        .setPositiveButton(R.string.update) { d, _ ->
                            activity.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE) {
                                if (it.isGranted) {
                                    appUpdateCheck.value
                                        ?.let { a ->
                                            val isApkAlreadyThere = File(
                                                activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/",
                                                a.let(genericInfo.apkString).toString()
                                            )
                                            if (isApkAlreadyThere.exists()) isApkAlreadyThere.delete()
                                            DownloadApk(
                                                activity,
                                                a.downloadUrl(genericInfo.apkString),
                                                a.let(genericInfo.apkString).toString()
                                            ).startDownloadingApk()
                                        }
                                }
                            }
                            d.dismiss()
                        }
                        .setNeutralButton(R.string.gotoBrowser) { d, _ ->
                            activity.openInCustomChromeBrowser("https://github.com/jakepurple13/OtakuWorld/releases/latest")
                            d.dismiss()
                        }
                        .setNegativeButton(R.string.notNow) { d, _ -> d.dismiss() }
                        .show()
                }

                SettingsMenuLink(
                    icon = { Image(painter = painterResource(id = R.drawable.ic_baseline_system_update_alt_24), contentDescription = null) },
                    title = { Text(text = stringResource(id = R.string.update_available), modifier = Modifier.padding(start = 5.dp)) },
                    subtitle = {
                        Text(
                            text = stringResource(
                                id = R.string.currentVersion,
                                appUpdate?.update_real_version.orEmpty()
                            ),
                            modifier = Modifier.padding(
                                start = 5.dp
                            )
                        )
                    },
                    onClick = update,
                    action = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.clickable(onClick = update)
                        )
                    }
                )

            }

            SettingsMenuLink(
                icon = { Image(painter = painterResource(id = R.drawable.github_icon), contentDescription = null) },
                title = { Text(text = stringResource(id = R.string.view_on_github), modifier = Modifier.padding(start = 5.dp)) },
                //subtitle = { Text(text = "", modifier = Modifier.padding(start = 5.dp)) },
                onClick = { activity.openInCustomChromeBrowser("https://github.com/jakepurple13/OtakuWorld/") },
                action = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.clickable(onClick = { activity.openInCustomChromeBrowser("https://github.com/jakepurple13/OtakuWorld/") })
                    )
                }
            )

        }

        SettingsGroup(
            title = { Text(text = "Otaku Apps", modifier = Modifier.padding(start = 5.dp)) }
        ) {

            SettingItem(
                "MangaWorld",
                R.drawable.mangaworld_logo,
                "com.programmersbox.mangaworld",
                packageManager, activity
            )

            SettingItem(
                "AnimeWorld",
                R.drawable.animeworld_logo,
                "com.programmersbox.animeworld",
                packageManager, activity
            )

            SettingItem(
                "NovelWorld",
                R.drawable.novelworld_logo,
                "com.programmersbox.novelworld",
                packageManager, activity
            )

        }

        /*
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
             */

        SettingsGroup(
            title = { Text(text = stringResource(id = R.string.general_menu_title), modifier = Modifier.padding(start = 5.dp)) }
        ) {

            val theme by activity.dataStore.data
                .map { it[THEME_SETTING] ?: "System" }
                .collectAsState(initial = "System")

            val themeList = arrayOf("System", "Light", "Dark")

            SettingsMenuLink(
                icon = { Image(painter = painterResource(id = R.drawable.ic_baseline_settings_brightness_24), contentDescription = null) },
                title = { Text(text = stringResource(id = R.string.theme_choice_title), modifier = Modifier.padding(start = 5.dp)) },
                subtitle = { Text(text = theme, modifier = Modifier.padding(start = 5.dp)) },
                onClick = {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.choose_a_theme)
                        .setIcon(R.drawable.ic_baseline_settings_brightness_24)
                        .setSingleChoiceItems(themeList, themeList.indexOf(theme)) { d, i ->
                            scope.launch(Dispatchers.IO) { activity.dataStore.edit { it[THEME_SETTING] = themeList[i] } }
                            when (themeList[i]) {
                                "System" -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                "Light" -> AppCompatDelegate.MODE_NIGHT_NO
                                "Dark" -> AppCompatDelegate.MODE_NIGHT_YES
                                else -> null
                            }?.let(AppCompatDelegate::setDefaultNightMode)
                            d.dismiss()
                        }
                        .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
                        .show()
                }
            )

        }

    }
}

@Composable
fun SettingItem(text: String, icon: Int, packageName: String, packageManager: PackageManager, activity: ComponentActivity) {
    val world = remember { packageManager.getLaunchIntentForPackage(packageName) }

    val version = try {
        packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).versionName
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    val onClick = {
        if (world != null) activity.startActivity(world)
        else activity.openInCustomChromeBrowser("https://github.com/jakepurple13/OtakuWorld/")
    }

    SettingsMenuLink(
        icon = { Image(painter = painterResource(id = icon), contentDescription = text) },
        title = { Text(text = text, modifier = Modifier.padding(start = 5.dp)) },
        subtitle = {
            Text(
                text = if (world != null) "Installed - Version: $version" else "Not Installed",
                modifier = Modifier.padding(start = 5.dp)
            )
        },
        onClick = onClick,
        action = { Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.clickable(onClick = onClick)) }
    )
}

class DownloadApk(val context: Context, private val downloadUrl: String, private val outputName: String) : CoroutineScope, KoinComponent {
    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job // to run code in Main(UI) Thread

    private val logo: MainLogo by inject()

    // call this method to cancel a coroutine when you don't need it anymore,
    // e.g. when user closes the screen
    fun cancel() {
        job.cancel()
    }

    fun startDownloadingApk() {
        if (URLUtil.isValidUrl(downloadUrl)) execute()
    }

    private lateinit var bar: AlertDialog

    private fun execute() = launch {
        onPreExecute()
        val result = doInBackground() // runs in background thread without blocking the Main Thread
        onPostExecute(result)
    }

    private suspend fun onProgressUpdate(vararg values: Int?) = withContext(Dispatchers.Main) {
        values[0]?.let {
            bar.setMessage(if (it > 99) context.getString(R.string.finishing_dots) else context.getString(R.string.downloading_dots, it))
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun doInBackground(): Boolean = withContext(Dispatchers.IO) { // to run code in Background Thread
        // do async work
        var flag = false

        try {
            val url = URL(downloadUrl)
            val c = url.openConnection() as HttpURLConnection
            c.requestMethod = "GET"
            c.connect()
            val path = Environment.getExternalStorageDirectory().toString() + "/Download/"
            val file = File(path)
            file.mkdirs()
            val outputFile = File(file, outputName)

            if (outputFile.exists()) outputFile.delete()

            val fos = FileOutputStream(outputFile)
            val inputStream = c.inputStream
            val totalSize = c.contentLength.toFloat() //size of apk

            val buffer = ByteArray(1024)
            var len1: Int
            var downloaded = 0f
            while (inputStream.read(buffer).also { len1 = it } != -1) {
                fos.write(buffer, 0, len1)
                downloaded += len1
                onProgressUpdate((downloaded * 100 / totalSize).toInt())
            }
            fos.close()
            inputStream.close()
            if (BuildConfig.DEBUG) outputFile.delete() else openNewVersion(path)
            flag = true
        } catch (e: MalformedURLException) {
            Loged.e("Update Error: " + e.message, "DownloadApk")
            flag = false
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext flag
    }

    // Runs on the Main(UI) Thread
    private fun onPreExecute() {
        // show progress
        bar = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.updating_dots)
            .setMessage(R.string.downloading_dots_no_percent)
            .setCancelable(false)
            .setIcon(logo.logoId)
            .show()
    }

    // Runs on the Main(UI) Thread
    private fun onPostExecute(result: Boolean?) {
        // hide progress
        bar.dismiss()
        Toast.makeText(context, if (result == true) R.string.finishedDownloading else R.string.errorTryAgain, Toast.LENGTH_SHORT).show()
    }

    private fun openNewVersion(location: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(getUriFromFile(location), "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    private fun getUriFromFile(location: String): Uri {
        return if (Build.VERSION.SDK_INT < 24) {
            Uri.fromFile(File(location + outputName))
        } else {
            FileProvider.getUriForFile(context, context.packageName + ".provider", File(location + outputName))
        }
    }
}