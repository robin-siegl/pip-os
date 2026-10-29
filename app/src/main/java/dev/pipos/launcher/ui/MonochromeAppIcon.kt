package dev.pipos.launcher.ui

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val iconCache = android.util.LruCache<String, ImageBitmap>(96)

/** Converts real launcher icons to phosphor silhouettes off the main thread. */
@Composable
fun MonochromeAppIcon(packageName: String, activityName: String, tint: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tintArgb = tint.toArgb()
    val key = "$packageName/$activityName:$tintArgb"
    val bitmap by produceState<ImageBitmap?>(initialValue = iconCache.get(key), key1 = key) {
        value = withContext(Dispatchers.Default) {
            iconCache.get(key) ?: runCatching {
                val drawable = context.packageManager.getActivityIcon(ComponentName(packageName, activityName)).mutate()
                val size = 80
                val source = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(source)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
                val pixels = IntArray(size * size)
                source.getPixels(pixels, 0, size, 0, 0, size, size)
                val rgb = tintArgb and 0x00FFFFFF
                for (index in pixels.indices) {
                    val pixel = pixels[index]
                    val alpha = android.graphics.Color.alpha(pixel)
                    val luminance = (android.graphics.Color.red(pixel) * .21f +
                        android.graphics.Color.green(pixel) * .72f +
                        android.graphics.Color.blue(pixel) * .07f) / 255f
                    pixels[index] = rgb or ((alpha * (.32f + .68f * luminance)).toInt().coerceIn(0, 255) shl 24)
                }
                source.recycle()
                Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
            }.getOrNull()?.also { iconCache.put(key, it) }
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap = bitmap!!, contentDescription = null, modifier = Modifier.size(28.dp))
        else Text("▣", color = tint)
    }
}
