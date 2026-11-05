package com.example.appletvlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private val formatter24h = SimpleDateFormat("HH:mm", Locale.getDefault())
private val formatter12h = SimpleDateFormat("hh:mm a", Locale.getDefault())

fun getCurrentTime(use24h: Boolean, timeMs: Long): String = 
    (if (use24h) formatter24h else formatter12h).format(Date(timeMs))

fun getGreeting(timeMs: Long): String {
    val h = Calendar.getInstance().apply { timeInMillis = timeMs }.get(Calendar.HOUR_OF_DAY)
    return when { 
        h < 6 -> "Доброй ночи"
        h < 12 -> "Доброе утро"
        h < 18 -> "Добрый день"
        else -> "Добрый вечер" 
    }
}

suspend fun getWeather(): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val lat = 55.7558
        val lon = 37.6173
        val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&timezone=auto")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
        
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val current = json.getJSONObject("current")
            val temp = current.getDouble("temperature_2m").roundToInt()
            val code = current.getInt("weather_code")
            val icon = when (code) {
                0 -> "☀️"; 1, 2, 3 -> "🌤️"; 45, 48 -> "🌫️"; 51, 53, 55 -> "🌦️"; 56, 57 -> "🌧️"
                61, 63, 65 -> "🌧️"; 66, 67 -> "🌧️"; 71, 73, 75 -> "❄️"; 77 -> "❄️"; 80, 81, 82 -> "🌧️"
                85, 86 -> "❄️"; 95 -> "⛈️"; 96, 99 -> "⛈️"; else -> "🌡️"
            }
            "$icon ${if (temp > 0) "+$temp" else "$temp"}°"
        } else "☁️ --°" 
    } catch (e: Exception) { "☁️ --°" }
}

fun loadBitmapFromUri(context: Context, uriString: String, reqWidth: Int = 400, reqHeight: Int = 400): ImageBitmap? {
    return try {
        val uri = Uri.parse(uriString)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        
        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight: Int = options.outHeight / 2
            val halfWidth: Int = options.outWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        val options2 = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        context.contentResolver.openInputStream(uri)?.use { 
            BitmapFactory.decodeStream(it, null, options2)?.asImageBitmap() 
        }
    } catch (e: Exception) { null }
}

fun Bitmap.getDominantColor(): Color {
    val scaled = Bitmap.createScaledBitmap(this, 32, 32, false)
    val pixels = IntArray(scaled.width * scaled.height)
    scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
    var r = 0; var g = 0; var b = 0; var count = 0
    for (pixel in pixels) {
        val alpha = android.graphics.Color.alpha(pixel)
        if (alpha > 200) {
            val red = android.graphics.Color.red(pixel)
            val green = android.graphics.Color.green(pixel)
            val blue = android.graphics.Color.blue(pixel)
            if ((red > 240 && green > 240 && blue > 240) || (red < 15 && green < 15 && blue < 15)) continue
            r += red; g += green; b += blue; count++
        }
    }
    return if (count == 0) Color(0xFF6E8BFF) else Color(r / count, g / count, b / count)
}

fun getAllInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val appsMap = mutableMapOf<String, AppInfo>()
    
    fun add(info: android.content.pm.ResolveInfo) {
        try {
            val pkg = info.activityInfo.packageName
            if (appsMap.containsKey(pkg)) return
            val appInfo = pm.getApplicationInfo(pkg, 0)
            var installTime = 0L
            try { installTime = pm.getPackageInfo(pkg, 0).firstInstallTime } catch (_: Exception) {}
            appsMap[pkg] = AppInfo(
                info.loadLabel(pm).toString(), pkg, info.activityInfo.name, 
                info.loadIcon(pm), (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0, installTime
            )
        } catch (_: Exception) {}
    }
    pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) }, 0).forEach { add(it) }
    pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }, 0).forEach { add(it) }
    return appsMap.values.toList()
}

fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) return this.bitmap
    val width = if (intrinsicWidth > 0) intrinsicWidth else 100
    val height = if (intrinsicHeight > 0) intrinsicHeight else 100
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}
