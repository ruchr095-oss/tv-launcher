package com.example.appletvlauncher

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            super.onCreate(savedInstanceState)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            setContent { 
                AppleTvScreen() 
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppleTvScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("apple_launcher", Context.MODE_PRIVATE) }
    
    val toneGen = remember { 
        try { android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 50) } catch (e: Exception) { null } 
    }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }

    var weatherText by remember { mutableStateOf("☁️ --°") }
    LaunchedEffect(Unit) {
        while (true) {
            try { weatherText = getWeather() } catch (_: Exception) {}
            delay(30 * 60 * 1000L)
        }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { toneGen?.release() } }
    }

    var settings by remember {
        mutableStateOf(
            LauncherSettings(
                showClock = prefs.getBoolean("show_clock", true),
                showGreeting = prefs.getBoolean("show_greeting", true),
                clock24h = prefs.getBoolean("clock24h", true),
                showLabels = prefs.getBoolean("show_labels", true),
                hideSystemApps = prefs.getBoolean("hide_system", false),
                sortOrder = prefs.getString("sort_order", "name") ?: "name",
                theme = prefs.getString("theme", "cosmos") ?: "cosmos",
                enableGlow = prefs.getBoolean("enable_glow", true),
                enableSounds = prefs.getBoolean("enable_sounds", true),
                pinCode = prefs.getString("pin_code", "1234") ?: "1234",
                wallpaperUri = prefs.getString("wallpaper_uri", null),
                blurWallpaper = prefs.getBoolean("blur_wallpaper", false),
                accentColor = prefs.getString("accent_color", "blue") ?: "blue",
                cardSize = prefs.getString("card_size", "medium") ?: "medium",
                iconShape = prefs.getString("icon_shape", "rounded") ?: "rounded",
                dockOpacity = prefs.getInt("dock_opacity", 33),
                showRecentApps = prefs.getBoolean("show_recent", true),
                autoLaunchPackage = prefs.getString("auto_launch_pkg", null),
                hideUiAfterMinutes = prefs.getInt("hide_ui_minutes", 0),
                dockPosition = prefs.getString("dock_position", "bottom") ?: "bottom"
            )
        )
    }

    var isScreenOff by remember { mutableStateOf(false) }
    var isUiHidden by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(settings.hideUiAfterMinutes) {
        if (settings.hideUiAfterMinutes > 0) {
            while (true) {
                delay(5000)
                if (System.currentTimeMillis() - lastInteractionTime > settings.hideUiAfterMinutes * 60 * 1000) {
                    isUiHidden = true
                }
            }
        }
    }

    fun updateSettings(new: LauncherSettings) {
        settings = new
        prefs.edit()
            .putBoolean("show_clock", new.showClock)
            .putBoolean("show_greeting", new.showGreeting)
            .putBoolean("clock24h", new.clock24h)
            .putBoolean("show_labels", new.showLabels)
            .putBoolean("hide_system", new.hideSystemApps)
            .putString("sort_order", new.sortOrder)
            .putString("theme", new.theme)
            .putBoolean("enable_glow", new.enableGlow)
            .putBoolean("enable_sounds", new.enableSounds)
            .putString("pin_code", new.pinCode)
            .putString("wallpaper_uri", new.wallpaperUri)
            .putBoolean("blur_wallpaper", new.blurWallpaper)
            .putString("accent_color", new.accentColor)
            .putString("card_size", new.cardSize)
            .putString("icon_shape", new.iconShape)
            .putInt("dock_opacity", new.dockOpacity)
            .putBoolean("show_recent", new.showRecentApps)
            .putString("auto_launch_pkg", new.autoLaunchPackage)
            .putInt("hide_ui_minutes", new.hideUiAfterMinutes)
            .putString("dock_position", new.dockPosition)
            .apply()
    }

    fun saveDockItems(items: List<DockItem>) {
        try {
            val jsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("isFolder", item.isFolder)
                if (item.isFolder) {
                    obj.put("folderName", item.folderName)
                    val appsArray = JSONArray()
                    item.folderApps.forEach { appsArray.put(it.packageName) }
                    obj.put("folderApps", appsArray)
                } else {
                    item.app?.let { obj.put("pkg", it.packageName) }
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("dock_items_json", jsonArray.toString()).apply()
            
            val allPkgs = items.flatMap { if (it.isFolder) it.folderApps.map { a -> a.packageName } else listOfNotNull(it.app?.packageName) }
            prefs.edit().putStringSet("saved_apps", allPkgs.toSet()).apply()
        } catch (e: Exception) { }
    }

    fun loadDockItems(installedApps: List<AppInfo>): List<DockItem> {
        val savedJson = prefs.getString("dock_items_json", null)
        if (savedJson != null) {
            try {
                val arr = JSONArray(savedJson)
                val items = mutableListOf<DockItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val isFolder = obj.getBoolean("isFolder")
                    val id = obj.getString("id")
                    if (isFolder) {
                        val name = obj.getString("folderName")
                        val appsArr = obj.getJSONArray("folderApps")
                        val fApps = mutableListOf<AppInfo>()
                        for (j in 0 until appsArr.length()) {
                            val pkg = appsArr.getString(j)
                            installedApps.find { it.packageName == pkg }?.let { fApps.add(it) }
                        }
                        if (fApps.isNotEmpty()) items.add(DockItem(id, true, folderName = name, folderApps = fApps))
                    } else {
                        val pkg = obj.getString("pkg")
                        installedApps.find { it.packageName == pkg }?.let { items.add(DockItem(id, false, app = it)) }
                    }
                }
                return items
            } catch (e: Exception) { }
        }
        
        val savedSet = prefs.getStringSet("saved_apps", emptySet()) ?: emptySet()
        val savedOrder = prefs.getString("saved_order", "") ?: ""
        val orderedPkgs = if (savedOrder.isEmpty()) savedSet.toList() else savedOrder.split(",").filter { it.isNotEmpty() }
        val items = mutableListOf<DockItem>()
        for (pkg in orderedPkgs) {
            installedApps.find { it.packageName == pkg }?.let { items.add(DockItem(it.packageName, false, app = it)) }
        }
        return items
    }

    fun loadAppPins(): Map<String, String> {
        val json = prefs.getString("app_pins_json", "{}") ?: "{}"
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getString(key)
            }
            map
        } catch (e: Exception) { emptyMap() }
    }

    var appPins by remember { mutableStateOf(loadAppPins()) }

    fun saveAppPins(newPins: Map<String, String>) {
        appPins = newPins
        try {
            val obj = JSONObject()
            newPins.forEach { (k, v) -> obj.put(k, v) }
            prefs.edit().putString("app_pins_json", obj.toString()).apply()
        } catch (e: Exception) { }
    }

    fun loadAppCovers(): Map<String, String> {
        val json = prefs.getString("app_covers_json", "{}") ?: "{}"
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getString(key)
            }
            map
        } catch (e: Exception) { emptyMap() }
    }

    var appCovers by remember { mutableStateOf(loadAppCovers()) }

    fun saveAppCovers(newCovers: Map<String, String>) {
        appCovers = newCovers
        try {
            val obj = JSONObject()
            newCovers.forEach { (k, v) -> obj.put(k, v) }
            prefs.edit().putString("app_covers_json", obj.toString()).apply()
        } catch (e: Exception) { }
    }

    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var selectedDockItems by remember { mutableStateOf<List<DockItem>>(emptyList()) }
    var recentApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var hiddenApps by remember { mutableStateOf<Set<String>>(prefs.getStringSet("hidden_apps", emptySet()) ?: emptySet()) }

    var isDrawerOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    var showHiddenAppsDialog by remember { mutableStateOf(false) }
    var showAutoLaunchDialog by remember { mutableStateOf(false) }

    var openedFolder by remember { mutableStateOf<DockItem?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var appToMoveToFolder by remember { mutableStateOf<AppInfo?>(null) }
    var newFolderName by remember { mutableStateOf("") }
    var folderNameError by remember { mutableStateOf("") }

    var isEditMode by remember { mutableStateOf(false) } 
    var grabbedItem by remember { mutableStateOf<DockItem?>(null) }
    var lastMenuApp by remember { mutableStateOf<AppInfo?>(null) } 
    
    var pendingFocusTarget by remember { mutableStateOf<String?>(null) }
    var lastLaunchedAppPkg by remember { mutableStateOf<String?>(null) }

    var rightPressCount by remember { mutableStateOf(0) }

    val iconCache = remember { mutableStateMapOf<String, ImageBitmap>() }
    val coverCache = remember { mutableStateMapOf<String, ImageBitmap>() }
    val dominantColorCache = remember { mutableMapOf<String, Color>() }
    var wallpaperBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    val itemFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val folderAppFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }

    var refreshTrigger by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.getBooleanExtra(Intent.EXTRA_REPLACING, false) == true) return
                when (intent?.action) {
                    Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REMOVED, Intent.ACTION_PACKAGE_CHANGED -> {
                        refreshTrigger++
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            try { context.registerReceiver(receiver, filter) } catch (_: Exception) {}
        }
        
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    LaunchedEffect(settings.wallpaperUri) {
        try {
            settings.wallpaperUri?.let { uri ->
                wallpaperBitmap = null 
                val bmp = withContext(Dispatchers.IO) { loadBitmapFromUri(context, uri, 1920, 1080) }
                wallpaperBitmap = bmp
            }
        } catch (_: Exception) {}
    }

    var contextMenuApp by remember { mutableStateOf<AppInfo?>(null) }
    var isContextMenuFromDrawer by remember { mutableStateOf(false) }
    var appToSetCover by remember { mutableStateOf<AppInfo?>(null) }

    val targetAccentColor = when (settings.accentColor) {
        "green" -> Color(0xFF34C759)
        "purple" -> Color(0xFFAF52DE)
        "orange" -> Color(0xFFFF9500)
        "red" -> Color(0xFFFF3B30)
        else -> Color(0xFF6E8BFF)
    }

    val accentColor by animateColorAsState(targetAccentColor, animationSpec = androidx.compose.animation.core.tween(500), label = "accent")
    val glowColor by animateColorAsState(targetAccentColor, animationSpec = androidx.compose.animation.core.tween(800), label = "glow")
    
    val glowBrush = remember(glowColor) {
        Brush.radialGradient(
            colors = listOf(glowColor.copy(alpha = 0.6f), Color.Transparent),
            center = Offset(1000f, 2000f),
            radius = 2200f
        )
    }

    val cardWidth = when (settings.cardSize) {
        "small" -> 110.dp
        "large" -> 180.dp
        else -> 135.dp
    }
    val cardHeight = when (settings.cardSize) {
        "small" -> 65.dp
        "large" -> 100.dp
        else -> 80.dp
    }
    
    val cardCorner = when (settings.iconShape) {
        "square" -> 0.dp
        "circle" -> 40.dp
        else -> 16.dp
    }
    
    val dockColor = Color(0x33FFFFFF).copy(alpha = settings.dockOpacity / 100f)

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var appToUnlock by remember { mutableStateOf<AppInfo?>(null) }
    var pendingUnlockOnly by remember { mutableStateOf(false) }

    var showSetPinDialog by remember { mutableStateOf(false) }
    var appToSetPin by remember { mutableStateOf<AppInfo?>(null) }
    var newAppPinInput by remember { mutableStateOf("") }
    var setPinError by remember { mutableStateOf("") }

    var showChangePinDialog by remember { mutableStateOf(false) }
    val oldPinInput = remember { mutableStateOf("") }
    val newPinInput = remember { mutableStateOf("") }
    var pinChangeError by remember { mutableStateOf("") }

    // ФОКУС-РЕКВЕСТЕРЫ
    val firstAppFocus = remember { FocusRequester() }
    val lastAppFocus = remember { FocusRequester() }
    val addButtonFocus = remember { FocusRequester() }
    val settingsButtonFocus = remember { FocusRequester() }
    val editButtonFocus = remember { FocusRequester() }
    val clearRamButtonFocus = remember { FocusRequester() }
    val screenOffButtonFocus = remember { FocusRequester() }
    
    val drawerFirstItemFocus = remember { FocusRequester() }
    val searchFieldFocus = remember { FocusRequester() }
    val closeDrawerFocus = remember { FocusRequester() }
    val hiddenDrawerButtonFocus = remember { FocusRequester() }
    val emptySearchFocus = remember { FocusRequester() } 
    
    val settingsFirstFocus = remember { FocusRequester() }
    val settingsCloseFocus = remember { FocusRequester() }
    
    val contextMenuFolderFocus = remember { FocusRequester() }
    val contextMenuCoverFocus = remember { FocusRequester() }
    val contextMenuPinFocus = remember { FocusRequester() }
    val contextMenuHideFocus = remember { FocusRequester() }
    val contextMenuCancelFocus = remember { FocusRequester() }
    val contextMenuInfoFocus = remember { FocusRequester() }
    
    val pinFieldFocus = remember { FocusRequester() }
    val pinField2Focus = remember { FocusRequester() }
    val pinCancelFocus = remember { FocusRequester() }
    val pinConfirmFocus = remember { FocusRequester() }

    val hiddenDialogCloseFocus = remember { FocusRequester() }
    val autoLaunchFirstFocus = remember { FocusRequester() }

    val isAnyDialogOpen = contextMenuApp != null || showPinDialog || showChangePinDialog || showSetPinDialog || isSettingsOpen || showHiddenAppsDialog || showAutoLaunchDialog || openedFolder != null || showCreateFolderDialog || isScreenOff

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val app = appToSetCover
                if (app != null) {
                    val bmp = loadBitmapFromUri(context, it.toString(), 400, 400)
                    if (bmp != null) {
                        if (coverCache.size >= 60) coverCache.keys.firstOrNull()?.let { coverCache.remove(it) }
                        coverCache[app.packageName] = bmp
                    }
                    saveAppCovers(appCovers + (app.packageName to it.toString()))
                    Toast.makeText(context, "Обложка установлена", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { Toast.makeText(context, "Ошибка загрузки", Toast.LENGTH_SHORT).show() }
        }
        appToSetCover = null
    }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                updateSettings(settings.copy(wallpaperUri = it.toString()))
                Toast.makeText(context, "Обои установлены", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(context, "Ошибка загрузки", Toast.LENGTH_SHORT).show() }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val json = JSONObject()
                    prefs.all.forEach { (key, value) ->
                        when (value) {
                            is Boolean, is String, is Int -> json.put(key, value)
                            is Set<*> -> {
                                val arr = JSONArray()
                                value.forEach { item -> arr.put(item) }
                                json.put(key, arr)
                            }
                        }
                    }
                    outputStream.write(json.toString().toByteArray())
                    Toast.makeText(context, "Бэкап сохранен!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { Toast.makeText(context, "Ошибка сохранения", Toast.LENGTH_SHORT).show() }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val text = inputStream.bufferedReader().use { reader -> reader.readText() }
                    val json = JSONObject(text)
                    val editor = prefs.edit()
                    editor.clear()
                    json.keys().forEach { key ->
                        val value = json.get(key)
                        when (value) {
                            is Boolean -> editor.putBoolean(key, value)
                            is String -> editor.putString(key, value)
                            is Int -> editor.putInt(key, value)
                            is JSONArray -> {
                                val set = mutableSetOf<String>()
                                for (i in 0 until value.length()) set.add(value.getString(i))
                                editor.putStringSet(key, set)
                            }
                        }
                    }
                    editor.apply()
                    Toast.makeText(context, "Бэкап восстановлен!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            } catch (e: Exception) { Toast.makeText(context, "Ошибка восстановления", Toast.LENGTH_SHORT).show() }
        }
    }

    fun openAppDirectly(app: AppInfo) {
        if (settings.enableSounds) runCatching { toneGen?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150) }
        prefs.edit().putInt("freq_${app.packageName}", prefs.getInt("freq_${app.packageName}", 0) + 1).apply()
        lastLaunchedAppPkg = app.packageName 

        val newRecent = recentApps.toMutableList()
        newRecent.removeAll { it.packageName == app.packageName }
        newRecent.add(0, app)
        recentApps = newRecent.take(4)

        try {
            context.startActivity(Intent().apply {
                setClassName(app.packageName, app.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e1: Exception) {
            try {
                val fb = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (fb != null) {
                    fb.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fb)
                    return
                }
            } catch (_: Exception) {}
            Toast.makeText(context, "Не удалось запустить", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearMemory() {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val myPid = android.os.Process.myPid()
            val processes = activityManager.runningAppProcesses ?: return
            processes.forEach { process ->
                if (process.pid != myPid) {
                    try { activityManager.killBackgroundProcesses(process.processName) } catch (_: Exception) {}
                }
            }
            Toast.makeText(context, "Память очищена! 🚀", Toast.LENGTH_SHORT).show()
            if (settings.enableSounds) runCatching { toneGen?.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 150) }
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось очистить память", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleDockItemClick(item: DockItem) {
        if (isAnyDialogOpen || isDrawerOpen) return

        if (isEditMode) {
            if (grabbedItem?.id == item.id) {
                grabbedItem = null 
                saveDockItems(selectedDockItems) 
            } else grabbedItem = item 
        } else {
            if (item.isFolder) openedFolder = item
            else {
                val app = item.app ?: return
                if (appPins.containsKey(app.packageName)) {
                    appToUnlock = app; pendingUnlockOnly = false; pinInput = ""; pinError = false; showPinDialog = true
                } else openAppDirectly(app)
            }
        }
    }

    fun handleDockItemLongClick(item: DockItem) {
        if (isAnyDialogOpen || isDrawerOpen || isEditMode) return
        if (!item.isFolder && item.app != null) {
            contextMenuApp = item.app
            isContextMenuFromDrawer = false
            if (settings.enableSounds) runCatching { toneGen?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 100) }
        }
    }

    fun moveDockItem(item: DockItem, direction: Int) {
        val index = selectedDockItems.indexOf(item)
        if (index == -1) return
        val newIndex = index + direction
        if (newIndex < 0 || newIndex >= selectedDockItems.size) return
        
        val mutable = selectedDockItems.toMutableList()
        mutable.removeAt(index)
        mutable.add(newIndex, item)
        selectedDockItems = mutable
    }

    var searchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = isAnyDialogOpen || isDrawerOpen || isEditMode) {
        when {
            isScreenOff -> isScreenOff = false
            showCreateFolderDialog -> showCreateFolderDialog = false
            openedFolder != null -> {
                val closedId = openedFolder!!.id
                openedFolder = null
                scope.launch { delay(100); itemFocusRequesters[closedId]?.requestFocus() }
            }
            showHiddenAppsDialog -> showHiddenAppsDialog = false
            showAutoLaunchDialog -> showAutoLaunchDialog = false
            contextMenuApp != null -> contextMenuApp = null
            showPinDialog -> { showPinDialog = false; appToUnlock = null }
            showSetPinDialog -> showSetPinDialog = false
            showChangePinDialog -> showChangePinDialog = false
            isDrawerOpen -> {
                if (searchQuery.isNotEmpty()) searchQuery = "" else { isDrawerOpen = false; keyboardController?.hide() }
            }
            isSettingsOpen -> isSettingsOpen = false
            isEditMode -> {
                if (grabbedItem != null) grabbedItem = null
                else { isEditMode = false; lastMenuApp = null }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isDrawerOpen = false
                    isSettingsOpen = false
                    isEditMode = false
                    contextMenuApp = null
                    openedFolder = null
                    showHiddenAppsDialog = false
                    showAutoLaunchDialog = false
                    showCreateFolderDialog = false
                    isScreenOff = false
                    keyboardController?.hide()
                    runCatching { firstAppFocus.requestFocus() }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(pendingFocusTarget, selectedDockItems) {
        pendingFocusTarget?.let { target ->
            if (target == "EDIT_BUTTON") {
                delay(150) 
                runCatching { editButtonFocus.requestFocus() }
                pendingFocusTarget = null
            } else if (selectedDockItems.any { it.id == target }) {
                delay(150)
                runCatching { itemFocusRequesters[target]?.requestFocus() }
                pendingFocusTarget = null
            }
        }
    }

    LaunchedEffect(settings.hideSystemApps, settings.sortOrder, refreshTrigger) {
        isLoading = true
        try {
            val loaded = withContext(Dispatchers.IO) { getAllInstalledApps(context) }
            val installedPkgs = loaded.map { it.packageName }.toSet()
            
            iconCache.keys.retainAll(installedPkgs)
            coverCache.keys.retainAll(installedPkgs)
            dominantColorCache.keys.retainAll(installedPkgs)
            
            val cleanedHidden = hiddenApps.filter { installedPkgs.contains(it) }.toSet()
            if (cleanedHidden.size != hiddenApps.size) {
                hiddenApps = cleanedHidden
                prefs.edit().putStringSet("hidden_apps", hiddenApps).apply()
            }
            
            val cleanedPins = appPins.filter { installedPkgs.contains(it.key) }
            if (cleanedPins.size != appPins.size) saveAppPins(cleanedPins)

            val filtered = if (settings.hideSystemApps) loaded.filter { app -> !app.isSystem } else loaded
            
            val sorted = when (settings.sortOrder) {
                "frequency" -> filtered.sortedWith(compareByDescending<AppInfo> { prefs.getInt("freq_${it.packageName}", 0) }.thenBy { it.name })
                "time" -> filtered.sortedWith(compareByDescending<AppInfo> { it.installTime }.thenBy { it.name })
                else -> filtered.sortedBy { it.name }
            }
            allApps = sorted

            val restoredDockItems = loadDockItems(loaded)
            selectedDockItems = restoredDockItems

            appCovers.forEach { (pkg, uriString) ->
                if (!coverCache.containsKey(pkg) && installedPkgs.contains(pkg) && coverCache.size < 60) {
                    val bmp = loadBitmapFromUri(context, uriString, 400, 400)
                    if (bmp != null) coverCache[pkg] = bmp
                }
            }
        } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
    }

    val effectiveTheme = settings.theme

    LaunchedEffect(isLoading, isSettingsOpen, isDrawerOpen, contextMenuApp, showPinDialog, selectedDockItems.size, showSetPinDialog, showChangePinDialog, isEditMode, grabbedItem, showHiddenAppsDialog, showAutoLaunchDialog, openedFolder, showCreateFolderDialog, isScreenOff) {
        if (!isLoading) {
            delay(200)
            runCatching {
                manageFocus(
                    isScreenOff = isScreenOff,
                    showCreateFolderDialog = showCreateFolderDialog,
                    openedFolder = openedFolder,
                    showHiddenAppsDialog = showHiddenAppsDialog,
                    showAutoLaunchDialog = showAutoLaunchDialog,
                    showPinDialog = showPinDialog,
                    showSetPinDialog = showSetPinDialog,
                    showChangePinDialog = showChangePinDialog,
                    contextMenuApp = contextMenuApp,
                    isSettingsOpen = isSettingsOpen,
                    isDrawerOpen = isDrawerOpen,
                    isEditMode = isEditMode,
                    grabbedItem = grabbedItem,
                    lastMenuApp = lastMenuApp,
                    selectedDockItems = selectedDockItems,
                    isAnyDialogOpen = isAnyDialogOpen,
                    allApps = allApps,
                    hiddenApps = hiddenApps,
                    screenOffButtonFocus = screenOffButtonFocus,
                    pinFieldFocus = pinFieldFocus,
                    folderAppFocusRequesters = folderAppFocusRequesters,
                    itemFocusRequesters = itemFocusRequesters,
                    hiddenDialogCloseFocus = hiddenDialogCloseFocus,
                    autoLaunchFirstFocus = autoLaunchFirstFocus,
                    contextMenuFolderFocus = contextMenuFolderFocus,
                    settingsFirstFocus = settingsFirstFocus,
                    searchFieldFocus = searchFieldFocus,
                    firstAppFocus = firstAppFocus,
                    addButtonFocus = addButtonFocus,
                    drawerFirstItemFocus = drawerFirstItemFocus
                )
            }
        }
    }

    LaunchedEffect(showCreateFolderDialog) {
        if (showCreateFolderDialog) {
            contextMenuApp = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        
        if (isScreenOff) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black)
                    .focusRequester(screenOffButtonFocus).focusable()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            isScreenOff = false
                            true
                        } else false
                    }.tvClickable { isScreenOff = false },
                contentAlignment = Alignment.Center
            ) {
                Text("Нажмите любую кнопку для выхода", color = Color(0xFF333333), fontSize = 16.sp)
            }
        } else {
            if (wallpaperBitmap != null) {
                Image(
                    bitmap = wallpaperBitmap!!, 
                    contentDescription = "Wallpaper", 
                    modifier = Modifier.fillMaxSize().then(if (settings.blurWallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(25.dp) else Modifier), 
                    contentScale = ContentScale.Crop
                )
                if (settings.blurWallpaper && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    BackgroundFor(effectiveTheme)
                }
            }

            if (settings.enableGlow) {
                Box(modifier = Modifier.fillMaxSize().drawBehind { drawRect(brush = glowBrush) })
            }

            if (isAnyDialogOpen) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) { 
                    if (isSettingsOpen) isSettingsOpen = false
                    if (contextMenuApp != null) contextMenuApp = null
                    if (showCreateFolderDialog) showCreateFolderDialog = false
                })
            }

            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 300f)
                )
            )

            if (settings.showClock && !isUiHidden) {
                Column(modifier = Modifier.align(Alignment.TopEnd).padding(48.dp), horizontalAlignment = Alignment.End) {
                    Text(getCurrentTime(settings.clock24h, now), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(weatherText, color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                    if (settings.showGreeting) Text(getGreeting(now), color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
                }
            }

            if (!isUiHidden) {
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 48.dp, top = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsButton(
                        color = accentColor,
                        modifier = Modifier
                            .focusRequester(settingsButtonFocus)
                            .focusProperties {
                                canFocus = !isEditMode && !isDrawerOpen && !isAnyDialogOpen
                                down = if (selectedDockItems.isNotEmpty()) firstAppFocus else addButtonFocus
                                right = clearRamButtonFocus
                            },
                        onClick = { isSettingsOpen = true }
                    )
                    
                    ActionCircleButton(
                        icon = "🧹",
                        modifier = Modifier
                            .focusRequester(clearRamButtonFocus)
                            .focusProperties {
                                canFocus = !isEditMode && !isDrawerOpen && !isAnyDialogOpen
                                down = if (selectedDockItems.isNotEmpty()) firstAppFocus else addButtonFocus
                                left = settingsButtonFocus
                                right = screenOffButtonFocus
                            },
                        onClick = { clearMemory() }
                    )

                    ActionCircleButton(
                        icon = "🌙",
                        modifier = Modifier
                            .focusRequester(screenOffButtonFocus)
                            .focusProperties {
                                canFocus = !isEditMode && !isDrawerOpen && !isAnyDialogOpen
                                down = if (selectedDockItems.isNotEmpty()) firstAppFocus else addButtonFocus
                                left = clearRamButtonFocus
                            },
                        onClick = { isScreenOff = true }
                    )
                }
            }

            if (!isLoading && !isUiHidden) {
                val focusScale = 1.15f
                var isEditBtnFocused by remember { mutableStateOf(false) }
                val editBtnAlpha by animateFloatAsState(if (isEditBtnFocused || isEditMode) 1f else 0f, label = "alpha")

                val dockAlignment = when (settings.dockPosition) {
                    "top" -> Alignment.TopCenter
                    "left" -> Alignment.CenterStart
                    "right" -> Alignment.CenterEnd
                    else -> Alignment.BottomCenter
                }
                
                val dockPadding = when (settings.dockPosition) {
                    "top" -> PaddingValues(top = 180.dp)
                    "left" -> PaddingValues(start = 20.dp)
                    "right" -> PaddingValues(end = 20.dp)
                    else -> PaddingValues(bottom = 50.dp)
                }

                Box(
                    modifier = Modifier.align(dockAlignment).padding(dockPadding)
                        .fillMaxWidth(if (settings.dockPosition == "left" || settings.dockPosition == "right") 0.6f else 0.85f)
                ) {
                    if (recentApps.isNotEmpty() && !isEditMode && settings.showRecentApps && settings.dockPosition == "bottom") {
                        Row(
                            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-110).dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            recentApps.forEachIndexed { index, app ->
                                RecentAppCard(
                                    app = app, index = index, total = recentApps.size, iconCache = iconCache,
                                    accentColor = accentColor, isAnyDialogOpen = isAnyDialogOpen, modifier = Modifier,
                                    onClick = { 
                                        if (appPins.containsKey(app.packageName)) {
                                            appToUnlock = app; pendingUnlockOnly = false; pinInput = ""; pinError = false; showPinDialog = true
                                        } else openAppDirectly(app)
                                    }
                                )
                            }
                        }
                    }

                    if (isEditMode) {
                        Text(
                            text = if (grabbedItem == null) "Выберите элемент (ОК)" else "Стрелки — двигать. Вверх — меню. Вниз — удалить. Назад — выход",
                            color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-50).dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                            .background(color = dockColor, shape = RoundedCornerShape(32.dp))
                            .border(
                                width = if (isEditMode) 2.dp else 1.dp, 
                                color = if (isEditMode) accentColor else Color(0x22FFFFFF), 
                                shape = RoundedCornerShape(32.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp) 
                            .focusProperties {
                                canFocus = !isDrawerOpen && !isAnyDialogOpen && !isSettingsOpen
                                up = if (isEditMode) FocusRequester.Default else settingsButtonFocus
                            },
                        horizontalArrangement = Arrangement.Center, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        selectedDockItems.forEachIndexed { index, item ->
                            val isFirst = index == 0
                            val isLastItem = index == selectedDockItems.lastIndex
                            val isGrabbed = grabbedItem?.id == item.id
                            
                            DockItemCard(
                                item = item, index = index, isFirst = isFirst, isLast = isLastItem && selectedDockItems.size >= 5,
                                settings = settings, iconCache = iconCache, coverCache = coverCache,
                                dominantColorCache = dominantColorCache, appCovers = appCovers,
                                cardWidth = cardWidth, cardHeight = cardHeight, corner = cardCorner,
                                focusScale = focusScale, isEditMode = isEditMode, isGrabbed = isGrabbed,
                                isAnyDialogOpen = isAnyDialogOpen, accentColor = accentColor,
                                focusRequester = itemFocusRequesters.getOrPut(item.id) { FocusRequester() },
                                settingsButtonFocus = settingsButtonFocus, editButtonFocus = editButtonFocus,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .then(if (isFirst) Modifier.focusRequester(firstAppFocus) else Modifier)
                                    .then(if (isLastItem) Modifier.focusRequester(lastAppFocus) else Modifier)
                                    .then(
                                        if (isEditMode && !isAnyDialogOpen) {
                                            Modifier.onPreviewKeyEvent { event ->
                                                if (event.type == KeyEventType.KeyDown) {
                                                    when (event.key) {
                                                        Key.DirectionUp -> {
                                                            if (!item.isFolder) { contextMenuApp = item.app; lastMenuApp = item.app }
                                                            true 
                                                        }
                                                        Key.DirectionDown -> {
                                                            val currentIndex = selectedDockItems.indexOf(item)
                                                            val newList = selectedDockItems - item
                                                            selectedDockItems = newList
                                                            saveDockItems(newList)
                                                            grabbedItem = null
                                                            
                                                            if (newList.isNotEmpty()) {
                                                                val nextFocusIndex = if (currentIndex >= newList.size) newList.size - 1 else currentIndex
                                                                pendingFocusTarget = newList[nextFocusIndex].id
                                                            } else {
                                                                isEditMode = false
                                                                pendingFocusTarget = "EDIT_BUTTON"
                                                            }
                                                            true 
                                                        }
                                                        Key.DirectionLeft -> {
                                                            if (isGrabbed) { moveDockItem(item, -1); true } else false
                                                        }
                                                        Key.DirectionRight -> {
                                                            if (isGrabbed) { moveDockItem(item, 1); true } else false
                                                        }
                                                        else -> false
                                                    }
                                                } else false
                                            }
                                        } else Modifier
                                    ),
                                onClick = { handleDockItemClick(item) },
                                onLongClick = { handleDockItemLongClick(item) }
                            )
                        }
                        
                        if (selectedDockItems.size < 5 && !isEditMode) {
                            AppleAddButton(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp) 
                                    .focusRequester(addButtonFocus)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                                            if (selectedDockItems.isEmpty()) {
                                                true 
                                            } else {
                                                false 
                                            }
                                        } else false
                                    }
                                    .focusProperties {
                                        left = if (selectedDockItems.isNotEmpty()) lastAppFocus else settingsButtonFocus
                                        right = if (selectedDockItems.isNotEmpty()) editButtonFocus else addButtonFocus
                                        up = settingsButtonFocus 
                                    },
                                width = cardWidth, height = cardHeight, corner = cardCorner,
                                onClick = { isDrawerOpen = true; keyboardController?.hide() }
                            )
                        }
                    }

                    if (!isEditMode && selectedDockItems.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(if (settings.dockPosition == "right") Alignment.BottomCenter else Alignment.CenterEnd)
                                .offset(x = if (settings.dockPosition == "right") 0.dp else 30.dp, y = if (settings.dockPosition == "right") 40.dp else 0.dp)
                                .width(60.dp).height(cardHeight + 24.dp).alpha(editBtnAlpha)
                                .shadow(if (isEditBtnFocused) 20.dp else 0.dp, RoundedCornerShape(30.dp))
                                .clip(RoundedCornerShape(30.dp)).background(Color(0xFF1C1C1E))
                                .border(if (isEditBtnFocused) 2.dp else 1.dp, if (isEditBtnFocused) Color.White else Color(0x33FFFFFF), RoundedCornerShape(30.dp))
                                .focusRequester(editButtonFocus).onFocusChanged { isEditBtnFocused = it.isFocused }
                                .focusProperties {
                                    canFocus = !isAnyDialogOpen && !isDrawerOpen
                                    left = if (selectedDockItems.size < 5) addButtonFocus else lastAppFocus
                                    up = settingsButtonFocus 
                                }
                                .tvClickable { isEditMode = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⋮", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Загрузка...", color = Color.White, fontSize = 24.sp)
                }
            }

            if (isDrawerOpen) {
                LaunchedEffect(Unit) {
                    delay(100)
                    keyboardController?.hide()
                }
                
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFA0A0A0A)).padding(48.dp)) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Все приложения (${allApps.size})", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            
                            BasicTextField(
                                value = searchQuery, onValueChange = { query -> searchQuery = query },
                                textStyle = TextStyle(color = Color.White, fontSize = 20.sp), cursorBrush = SolidColor(Color.White),
                                modifier = Modifier.width(200.dp).focusRequester(searchFieldFocus)
                                    .focusProperties { 
                                        down = if (allApps.isEmpty()) emptySearchFocus else drawerFirstItemFocus
                                        right = if (hiddenApps.isNotEmpty()) hiddenDrawerButtonFocus else closeDrawerFocus
                                    }
                                    .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp)).padding(12.dp)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                                            keyboardController?.hide()
                                            false 
                                        } else if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace && searchQuery.isEmpty()) {
                                            isDrawerOpen = false; keyboardController?.hide(); true 
                                        } else false
                                    },
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) Text("🔍 Поиск...", color = Color.Gray, fontSize = 20.sp)
                                    innerTextField()
                                }
                            )
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (hiddenApps.isNotEmpty()) {
                                    Text("Скрытые (${hiddenApps.size})", color = accentColor, fontSize = 20.sp, modifier = Modifier
                                        .focusRequester(hiddenDrawerButtonFocus)
                                        .focusProperties { left = searchFieldFocus; right = closeDrawerFocus; down = if (allApps.isEmpty()) emptySearchFocus else drawerFirstItemFocus }
                                        .tvClickable { isDrawerOpen = false; keyboardController?.hide(); showHiddenAppsDialog = true }
                                    )
                                }
                                Text("Закрыть", color = Color.White, fontSize = 20.sp, modifier = Modifier
                                    .focusRequester(closeDrawerFocus)
                                    .focusProperties { left = if (hiddenApps.isNotEmpty()) hiddenDrawerButtonFocus else searchFieldFocus; down = if (allApps.isEmpty()) emptySearchFocus else drawerFirstItemFocus }
                                    .tvClickable { isDrawerOpen = false; keyboardController?.hide() }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))

                        val filteredApps = allApps.filter { app ->
                            !hiddenApps.contains(app.packageName) && (searchQuery.isEmpty() || app.name.contains(searchQuery, ignoreCase = true))
                        }
                        
                        if (filteredApps.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().focusRequester(emptySearchFocus).focusable(), contentAlignment = Alignment.Center) {
                                Text("Ничего не найдено", color = Color.Gray, fontSize = 24.sp)
                            }
                        } else {
                            LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                                    GridAppCard(
                                        app = app, index = index, settings = settings, iconCache = iconCache,
                                        coverCache = coverCache, appCovers = appCovers, cardWidth = 160.dp, cardHeight = 100.dp, corner = 20.dp,
                                        modifier = Modifier
                                            .then(if (index == 0) Modifier.focusRequester(drawerFirstItemFocus) else Modifier)
                                            .then(if (index < 4) Modifier.focusProperties { up = searchFieldFocus } else Modifier), 
                                        onClick = {
                                            if (selectedDockItems.none { it.app?.packageName == app.packageName }) {
                                                val newList = selectedDockItems + DockItem(app.packageName, false, app = app)
                                                selectedDockItems = newList
                                                saveDockItems(newList)
                                            }
                                            isDrawerOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isSettingsOpen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.45f).padding(vertical = 48.dp, horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF15181E), RoundedCornerShape(32.dp)) 
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp)).padding(32.dp).focusGroup() 
                        .onPreviewKeyEvent { event -> if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) true else false }
                ) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Настройки", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x22FFFFFF))
                                    .focusRequester(settingsCloseFocus).tvClickable(onClick = { isSettingsOpen = false }),
                                contentAlignment = Alignment.Center
                            ) { Text("✕", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium) }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        SettingsSection("Отображение", first = true) {
                            SettingRow("Часы", settings.showClock, Modifier.focusRequester(settingsFirstFocus)) { updateSettings(settings.copy(showClock = it)) }
                            SettingRow("Приветствие", settings.showGreeting) { updateSettings(settings.copy(showGreeting = it)) }
                            SettingRow("Свечение", settings.enableGlow) { updateSettings(settings.copy(enableGlow = it)) }
                            SettingRow("Недавние приложения", settings.showRecentApps) { updateSettings(settings.copy(showRecentApps = it)) }
                            SettingRow("Размытие обоев", settings.blurWallpaper) { updateSettings(settings.copy(blurWallpaper = it)) }
                            
                            SettingAction("Выбрать свои обои") { wallpaperPickerLauncher.launch(arrayOf("image/*")) }
                            if (settings.wallpaperUri != null) {
                                SettingRow("Сбросить обои", false) { updateSettings(settings.copy(wallpaperUri = null)) }
                            }
                            
                            SettingCycle("Цвет акцента", settings.accentColor, listOf("blue", "green", "purple", "orange", "red")) { updateSettings(settings.copy(accentColor = it)) }
                            SettingCycle("Размер иконок", settings.cardSize, listOf("small", "medium", "large")) { updateSettings(settings.copy(cardSize = it)) }
                            SettingCycle("Форма иконок", settings.iconShape, listOf("square", "rounded", "circle")) { updateSettings(settings.copy(iconShape = it)) }
                            SettingCycle("Прозрачность дока", settings.dockOpacity.toString(), listOf("10", "25", "33", "50", "75", "100")) { updateSettings(settings.copy(dockOpacity = it.toIntOrNull() ?: 33)) }
                            SettingCycle("Положение дока", settings.dockPosition, listOf("bottom", "top", "left", "right")) { updateSettings(settings.copy(dockPosition = it)) }
                            SettingCycle("Скрыть интерфейс (мин)", settings.hideUiAfterMinutes.toString(), listOf("0", "1", "5", "10", "30")) { updateSettings(settings.copy(hideUiAfterMinutes = it.toIntOrNull() ?: 0)) }
                        }
                        SettingsSection("Звук") {
                            SettingRow("Звуки кликов", settings.enableSounds) { updateSettings(settings.copy(enableSounds = it)) }
                        }
                        SettingsSection("Приложения") {
                            SettingRow("Скрыть системные", settings.hideSystemApps) { updateSettings(settings.copy(hideSystemApps = it)) }
                            SettingCycle("Сортировка", settings.sortOrder, listOf("name", "frequency", "time")) { updateSettings(settings.copy(sortOrder = it)) }
                            
                            SettingAction("Очистить память (Kill Apps)") { clearMemory() }

                            if (hiddenApps.isNotEmpty()) SettingAction("Управление скрытыми (${hiddenApps.size})") { showHiddenAppsDialog = true }
                            SettingAction("Автозапуск приложения") { showAutoLaunchDialog = true }
                            SettingAction("Экспорт настроек") { exportBackupLauncher.launch("launcher_backup.json") }
                            SettingAction("Импорт настроек") { importBackupLauncher.launch(arrayOf("application/json")) }
                        }
                        SettingsSection("Оформление (Темы)") {
                            SettingCycle("Тема", settings.theme, listOf("cosmos", "gradient", "midnight", "retro")) { updateSettings(settings.copy(theme = it)) }
                        }
                        SettingsSection("Безопасность") {
                            SettingAction("Сменить общий PIN-код") { oldPinInput.value = ""; newPinInput.value = ""; pinChangeError = ""; showChangePinDialog = true }
                        }
                    }
                }
            }

            if (showCreateFolderDialog && appToMoveToFolder != null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null
                    ) { showCreateFolderDialog = false }, contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.background(Color(0xFF1C1C1E), RoundedCornerShape(24.dp)).padding(32.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                            .focusGroup().onPreviewKeyEvent { event -> if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)) true else false }, 
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Создать папку", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Приложение: ${appToMoveToFolder!!.name}", color = Color.Gray, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        BasicTextField(
                            value = newFolderName, onValueChange = { newFolderName = it; folderNameError = "" },
                            textStyle = TextStyle(color = Color.White, fontSize = 24.sp, textAlign = TextAlign.Center), cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF2C2C2E), RoundedCornerShape(12.dp)).padding(16.dp)
                                .focusRequester(pinFieldFocus).focusProperties { down = pinCancelFocus },
                            decorationBox = { innerTextField -> 
                                if (newFolderName.isEmpty()) Text("Название папки", color = Color.Gray, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                innerTextField() 
                            }
                        )
                        
                        if (folderNameError.isNotEmpty()) { Spacer(modifier = Modifier.height(8.dp)); Text(folderNameError, color = Color.Red, fontSize = 14.sp) }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Text("Отмена", color = Color.Gray, fontSize = 18.sp, modifier = Modifier
                                .focusRequester(pinCancelFocus).focusProperties { up = pinFieldFocus; right = pinConfirmFocus }
                                .tvClickable { showCreateFolderDialog = false; keyboardController?.hide() }
                            )
                            Text("Создать", color = Color(0xFF6E8BFF), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier
                                .focusRequester(pinConfirmFocus).focusProperties { up = pinFieldFocus; left = pinCancelFocus }
                                .tvClickable {
                                    if (newFolderName.isNotBlank()) {
                                        val app = appToMoveToFolder!!
                                        val newFolder = DockItem(UUID.randomUUID().toString(), true, folderName = newFolderName, folderApps = listOf(app))
                                        val currentList = selectedDockItems.toMutableList()
                                        val appIndex = currentList.indexOfFirst { !it.isFolder && it.app?.packageName == app.packageName }
                                        
                                        if (appIndex != -1) { currentList.removeAt(appIndex); currentList.add(appIndex, newFolder) } else { currentList.add(newFolder) }
                                        
                                        selectedDockItems = currentList
                                        saveDockItems(currentList)
                                        showCreateFolderDialog = false
                                        contextMenuApp = null
                                        keyboardController?.hide()
                                        Toast.makeText(context, "Папка создана", Toast.LENGTH_SHORT).show()
                                    } else folderNameError = "Введите имя"
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showHiddenAppsDialog,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                val hiddenList = allApps.filter { hiddenApps.contains(it.packageName) }
                
                Box(
                    modifier = Modifier.width(600.dp).heightIn(max = 600.dp).background(Color(0xEE1A1A1C), RoundedCornerShape(28.dp))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(28.dp)).padding(32.dp).focusGroup()
                        .onPreviewKeyEvent { event -> if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)) true else false }
                ) {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Скрытые приложения", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (hiddenList.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("Нет скрытых приложений", color = Color.Gray, fontSize = 18.sp) }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(hiddenList, key = { it.packageName }) { app ->
                                    var isFocused by remember { mutableStateOf(false) }
                                    val bgColor by animateFloatAsState(if (isFocused) 1f else 0f, label = "bg") // dummy
                                    val bgColorC by androidx.compose.animation.animateColorAsState(if (isFocused) Color.White else Color(0x15FFFFFF), label = "bg")
                                    val textColor by androidx.compose.animation.animateColorAsState(if (isFocused) Color.Black else Color.White, label = "text")
                                    val imageBitmap = iconCache[app.packageName]

                                    Row(
                                        modifier = Modifier.fillMaxWidth().focusRequester(itemFocusRequesters.getOrPut(app.packageName) { FocusRequester() })
                                            .background(bgColorC, RoundedCornerShape(16.dp)).onFocusChanged { isFocused = it.isFocused }
                                            .tvClickable {
                                                val currentIndex = hiddenList.indexOf(app)
                                                hiddenApps = hiddenApps - app.packageName
                                                prefs.edit().putStringSet("hidden_apps", hiddenApps).apply()
                                                
                                                if (selectedDockItems.none { it.app?.packageName == app.packageName }) {
                                                    val newList = selectedDockItems + DockItem(app.packageName, false, app = app)
                                                    selectedDockItems = newList
                                                    saveDockItems(newList)
                                                }
                                                
                                                val newHiddenList = hiddenList.filter { it.packageName != app.packageName }
                                                scope.launch {
                                                    delay(100)
                                                    if (newHiddenList.isNotEmpty()) {
                                                        val nextIndex = if (currentIndex >= newHiddenList.size) newHiddenList.size - 1 else currentIndex
                                                        itemFocusRequesters[newHiddenList[nextIndex].packageName]?.requestFocus()
                                                    } else hiddenDialogCloseFocus.requestFocus()
                                                }
                                            }.padding(horizontal = 24.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (imageBitmap != null) { Image(bitmap = imageBitmap, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))); Spacer(modifier = Modifier.width(16.dp)) }
                                        Text(app.name, color = textColor, fontSize = 18.sp, fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                                        Text("Вернуть", color = if (isFocused) accentColor else Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val isRestoreAllEnabled = hiddenList.isNotEmpty()
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                    .background(if (isRestoreAllEnabled) accentColor else accentColor.copy(alpha = 0.2f))
                                    .then(if (isRestoreAllEnabled) Modifier.tvClickable { 
                                        val appsToRestore = allApps.filter { hiddenApps.contains(it.packageName) }
                                        val newList = selectedDockItems.toMutableList()
                                        appsToRestore.forEach { app -> if (newList.none { it.app?.packageName == app.packageName }) newList.add(DockItem(app.packageName, false, app = app)) }
                                        selectedDockItems = newList
                                        saveDockItems(newList)
                                        hiddenApps = emptySet()
                                        prefs.edit().putStringSet("hidden_apps", emptySet()).apply()
                                        hiddenDialogCloseFocus.requestFocus()
                                    } else Modifier).padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("Вернуть ВСЕ", color = if (isRestoreAllEnabled) Color.White else Color.White.copy(alpha = 0.5f), fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            
                            Box(
                                modifier = Modifier.weight(1f).focusRequester(hiddenDialogCloseFocus).clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x33FFFFFF)).tvClickable { showHiddenAppsDialog = false }.padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("Закрыть", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showAutoLaunchDialog,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier.width(600.dp).heightIn(max = 600.dp).background(Color(0xEE1A1A1C), RoundedCornerShape(28.dp))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(28.dp)).padding(32.dp).focusGroup()
                        .onPreviewKeyEvent { event -> if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)) true else false }
                ) {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Автозапуск приложения", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(allApps, key = { it.packageName }) { app ->
                                var isFocused by remember { mutableStateOf(false) }
                                val isSelected = settings.autoLaunchPackage == app.packageName
                                val bgColorC by androidx.compose.animation.animateColorAsState(when { isFocused -> Color.White; isSelected -> accentColor.copy(alpha = 0.3f); else -> Color(0x15FFFFFF) }, label = "bg")
                                val textColor by androidx.compose.animation.animateColorAsState(if (isFocused) Color.Black else Color.White, label = "text")
                                val imageBitmap = iconCache[app.packageName]

                                Row(
                                    modifier = Modifier.fillMaxWidth().focusRequester(itemFocusRequesters.getOrPut(app.packageName) { FocusRequester() })
                                        .background(bgColorC, RoundedCornerShape(16.dp)).onFocusChanged { isFocused = it.isFocused }
                                        .tvClickable { updateSettings(settings.copy(autoLaunchPackage = app.packageName)); showAutoLaunchDialog = false }
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (imageBitmap != null) { Image(bitmap = imageBitmap, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))); Spacer(modifier = Modifier.width(16.dp)) }
                                    Text(app.name, color = textColor, fontSize = 18.sp, fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                                    if (isSelected) Text("✓", color = if (isFocused) Color.Black else accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x33FFFFFF))
                                .tvClickable { updateSettings(settings.copy(autoLaunchPackage = null)); showAutoLaunchDialog = false }.padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("Отключить автозапуск", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
                    }
                }
            }

            AnimatedVisibility(
                visible = contextMenuApp != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 48.dp, horizontal = 48.dp)
            ) {
                if (contextMenuApp != null) {
                    val app = contextMenuApp!!
                    val imageBitmap = iconCache[app.packageName]
                    
                    Box(
                        modifier = Modifier.width(380.dp).wrapContentHeight().background(Color(0xEE1A1A1C), RoundedCornerShape(28.dp))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(28.dp)).padding(24.dp).focusGroup()
                            .onPreviewKeyEvent { event -> if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)) true else false }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                if (imageBitmap != null) Image(bitmap = imageBitmap, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(app.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Управление приложением", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                            
                            HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp, modifier = Modifier.padding(bottom = 16.dp))

                            ContextMenuItem("Создать папку", Modifier.focusRequester(contextMenuFolderFocus).focusProperties { up = contextMenuCancelFocus }, contextMenuFolderFocus) {
                                appToMoveToFolder = app
                                newFolderName = ""; folderNameError = ""
                                showCreateFolderDialog = true
                                contextMenuApp = null
                            }
                            ContextMenuItem("Установить обложку", Modifier.focusRequester(contextMenuCoverFocus), contextMenuCoverFocus) {
                                appToSetCover = app; coverPickerLauncher.launch(arrayOf("image/*")); contextMenuApp = null
                            }
                            ContextMenuItem("Информация о приложении", Modifier.focusRequester(contextMenuInfoFocus), contextMenuInfoFocus) {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    intent.data = Uri.parse("package:${app.packageName}"); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent)
                                } catch (e: Exception) { Toast.makeText(context, "Не удалось открыть", Toast.LENGTH_SHORT).show() }
                                contextMenuApp = null
                            }
                            val isLocked = appPins.containsKey(app.packageName)
                            ContextMenuItem(if (isLocked) "Разблокировать" else "Заблокировать PIN", Modifier.focusRequester(contextMenuPinFocus), contextMenuPinFocus) {
                                if (isLocked) { appToUnlock = app; pendingUnlockOnly = true; pinInput = ""; pinError = false; showPinDialog = true }
                                else { appToSetPin = app; newAppPinInput = ""; setPinError = ""; showSetPinDialog = true }
                                contextMenuApp = null
                            }
                            ContextMenuItem("Скрыть приложение", Modifier.focusRequester(contextMenuHideFocus), contextMenuHideFocus, isDestructive = true) {
                                hiddenApps = hiddenApps + app.packageName
                                prefs.edit().putStringSet("hidden_apps", hiddenApps).apply()
                                if (!isContextMenuFromDrawer) { val newList = selectedDockItems.filter { it.app?.packageName != app.packageName }; selectedDockItems = newList; saveDockItems(newList) }
                                contextMenuApp = null
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0x11FFFFFF), thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))
                            
                            Box(
                                modifier = Modifier.fillMaxWidth().focusRequester(contextMenuCancelFocus).focusProperties { down = contextMenuFolderFocus } 
                                    .clip(RoundedCornerShape(12.dp)).tvClickable(onClick = { contextMenuApp = null }).padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("Отмена", color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = openedFolder != null,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (openedFolder != null) {
                    val folder = openedFolder!!
                    Box(
                        modifier = Modifier.width(700.dp).heightIn(max = 600.dp).background(Color(0xEE1A1A1C), RoundedCornerShape(28.dp))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(28.dp)).padding(32.dp).focusGroup()
                            .onPreviewKeyEvent { event -> if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)) true else false }
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(folder.folderName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x22FFFFFF)).tvClickable { openedFolder = null },
                                    contentAlignment = Alignment.Center
                                ) { Text("✕", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium) }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0x22FFFFFF), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyVerticalGrid(columns = GridCells.Adaptive(120.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                itemsIndexed(folder.folderApps, key = { _, app -> app.packageName }) { index, app ->
                                    val imageBitmap = iconCache[app.packageName]
                                    LaunchedEffect(app.packageName) { 
                                        if (imageBitmap == null) {
                                            val bmp = withContext(Dispatchers.IO) { app.icon.toBitmap().asImageBitmap() }
                                            iconCache[app.packageName] = bmp
                                        }
                                    }
                                    var isFocused by remember { mutableStateOf(false) }
                                    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "scale")
                                    
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.scale(scale).focusRequester(folderAppFocusRequesters.getOrPut(app.packageName) { FocusRequester() })
                                            .background(if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(16.dp))
                                            .onFocusChanged { isFocused = it.isFocused }
                                            .tvClickable {
                                                openedFolder = null
                                                if (appPins.containsKey(app.packageName)) {
                                                    appToUnlock = app; pendingUnlockOnly = false; pinInput = ""; pinError = false; showPinDialog = true
                                                } else openAppDirectly(app)
                                            }.padding(8.dp)
                                    ) {
                                        if (imageBitmap != null) Image(bitmap = imageBitmap, contentDescription = app.name, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(app.name, color = if (isFocused) Color.Black else Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showSetPinDialog && appToSetPin != null) {
                LauncherPinDialog(
                    title = "Установить PIN", subtitle = appToSetPin!!.name, pinValue = newAppPinInput,
                    onPinChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) { newAppPinInput = it; setPinError = "" } },
                    errorText = setPinError, onDismiss = { showSetPinDialog = false },
                    onConfirm = {
                        if (newAppPinInput.isNotEmpty()) { saveAppPins(appPins + (appToSetPin!!.packageName to newAppPinInput)); showSetPinDialog = false } 
                        else setPinError = "PIN не может быть пустым"
                    },
                    focusRequester = pinFieldFocus, cancelFocus = pinCancelFocus, confirmFocus = pinConfirmFocus
                )
            }

            if (showPinDialog && appToUnlock != null) {
                val app = appToUnlock!!
                LauncherPinDialog(
                    title = if (pendingUnlockOnly) "Снять блокировку" else "Введите PIN для", subtitle = app.name, pinValue = pinInput,
                    onPinChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) { pinInput = it; pinError = false } },
                    errorText = if (pinError) "Неверный PIN" else "", onDismiss = { showPinDialog = false; appToUnlock = null },
                    onConfirm = {
                        if (pinInput == appPins[app.packageName] || pinInput == settings.pinCode) {
                            val target = appToUnlock; val mode = pendingUnlockOnly
                            showPinDialog = false; appToUnlock = null
                            if (mode && target != null) saveAppPins(appPins - target.packageName) else if (target != null) openAppDirectly(target)
                        } else { pinError = true; pinInput = "" }
                    },
                    focusRequester = pinFieldFocus, cancelFocus = pinCancelFocus, confirmFocus = pinConfirmFocus
                )
            }

            if (showChangePinDialog) {
                LauncherPinDialog(
                    title = "Смена общего PIN", subtitle = "Введите текущий и новый PIN", pinValue = oldPinInput.value,
                    onPinChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) { oldPinInput.value = it; pinChangeError = "" } },
                    errorText = pinChangeError, onDismiss = { showChangePinDialog = false },
                    onConfirm = {
                        if (oldPinInput.value == settings.pinCode) {
                            if (newPinInput.value.isNotEmpty()) { updateSettings(settings.copy(pinCode = newPinInput.value)); showChangePinDialog = false } 
                            else pinChangeError = "Новый PIN не может быть пустым"
                        } else pinChangeError = "Неверный текущий PIN"
                    },
                    focusRequester = pinFieldFocus, pinField2Focus = pinField2Focus, cancelFocus = pinCancelFocus, confirmFocus = pinConfirmFocus,
                    isChangePin = true, newPinValue = newPinInput.value,
                    onNewPinChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) { newPinInput.value = it; pinChangeError = "" } }
                )
            }
        }
    }
}

// -------------------- ВСПОМОГАТЕЛЬНАЯ ФУНКЦИЯ ДЛЯ ФОКУСА --------------------

private fun manageFocus(
    isScreenOff: Boolean,
    showCreateFolderDialog: Boolean,
    openedFolder: DockItem?,
    showHiddenAppsDialog: Boolean,
    showAutoLaunchDialog: Boolean,
    showPinDialog: Boolean,
    showSetPinDialog: Boolean,
    showChangePinDialog: Boolean,
    contextMenuApp: AppInfo?,
    isSettingsOpen: Boolean,
    isDrawerOpen: Boolean,
    isEditMode: Boolean,
    grabbedItem: DockItem?,
    lastMenuApp: AppInfo?,
    selectedDockItems: List<DockItem>,
    isAnyDialogOpen: Boolean,
    allApps: List<AppInfo>,
    hiddenApps: Set<String>,
    screenOffButtonFocus: FocusRequester,
    pinFieldFocus: FocusRequester,
    folderAppFocusRequesters: MutableMap<String, FocusRequester>,
    itemFocusRequesters: MutableMap<String, FocusRequester>,
    hiddenDialogCloseFocus: FocusRequester,
    autoLaunchFirstFocus: FocusRequester,
    contextMenuFolderFocus: FocusRequester,
    settingsFirstFocus: FocusRequester,
    searchFieldFocus: FocusRequester,
    firstAppFocus: FocusRequester,
    addButtonFocus: FocusRequester,
    drawerFirstItemFocus: FocusRequester
) {
    try {
        when {
            isScreenOff -> screenOffButtonFocus.requestFocus()
            showCreateFolderDialog -> pinFieldFocus.requestFocus()
            openedFolder != null -> {
                val firstApp = openedFolder.folderApps.firstOrNull()
                if (firstApp != null) folderAppFocusRequesters.getOrPut(firstApp.packageName) { FocusRequester() }.requestFocus()
            }
            showHiddenAppsDialog -> {
                val hiddenList = allApps.filter { hiddenApps.contains(it.packageName) }
                if (hiddenList.isNotEmpty()) {
                    itemFocusRequesters.getOrPut(hiddenList.first().packageName) { FocusRequester() }.requestFocus()
                } else {
                    hiddenDialogCloseFocus.requestFocus()
                }
            }
            showAutoLaunchDialog -> autoLaunchFirstFocus.requestFocus()
            showPinDialog -> pinFieldFocus.requestFocus()
            showSetPinDialog -> pinFieldFocus.requestFocus()
            showChangePinDialog -> pinFieldFocus.requestFocus()
            contextMenuApp != null -> contextMenuFolderFocus.requestFocus()
            isSettingsOpen -> settingsFirstFocus.requestFocus()
            isDrawerOpen -> {
                if (allApps.isNotEmpty()) {
                    drawerFirstItemFocus.requestFocus()
                }
            }
            isEditMode && grabbedItem != null -> itemFocusRequesters[grabbedItem.id]?.requestFocus()
            isEditMode && lastMenuApp != null -> {
                val id = selectedDockItems.find { it.app?.packageName == lastMenuApp.packageName || it.folderApps.any { a -> a.packageName == lastMenuApp.packageName } }?.id
                if (id != null) itemFocusRequesters[id]?.requestFocus()
            }
            isEditMode && selectedDockItems.isNotEmpty() -> firstAppFocus.requestFocus()
            !isEditMode && selectedDockItems.isNotEmpty() && !isAnyDialogOpen && !isDrawerOpen -> firstAppFocus.requestFocus()
            !isEditMode && selectedDockItems.isEmpty() && !isAnyDialogOpen && !isDrawerOpen -> addButtonFocus.requestFocus()
            else -> {}
        }
    } catch (_: Exception) { }
}
