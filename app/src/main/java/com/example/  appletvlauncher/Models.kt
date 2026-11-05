package com.example.appletvlauncher

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
    val isSystem: Boolean = false,
    val installTime: Long = 0L
)

data class DockItem(
    val id: String, 
    val isFolder: Boolean,
    val app: AppInfo? = null,
    val folderName: String = "",
    val folderApps: List<AppInfo> = emptyList()
)

data class LauncherSettings(
    val showClock: Boolean = true,
    val showGreeting: Boolean = true,
    val clock24h: Boolean = true,
    val showLabels: Boolean = true,
    val hideSystemApps: Boolean = false,
    val sortOrder: String = "name",
    val theme: String = "cosmos",
    val enableGlow: Boolean = true,
    val enableSounds: Boolean = true,
    val pinCode: String = "1234",
    val wallpaperUri: String? = null,
    val blurWallpaper: Boolean = false,
    val accentColor: String = "blue",
    val cardSize: String = "medium",
    val iconShape: String = "rounded",
    val dockOpacity: Int = 33,
    val showRecentApps: Boolean = true,
    val autoLaunchPackage: String? = null,
    val hideUiAfterMinutes: Int = 0,
    val dockPosition: String = "bottom"
)
