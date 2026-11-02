package io.github.fallasleeptimetable.widget

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils

data class WidgetTextColors(
    val primary: Color,
    val secondary: Color,
)

/**
 * 透明小组件直接叠在壁纸上，文字颜色按壁纸亮度取黑或白。
 *
 * 优先使用系统提供的壁纸主色；取不到时把壁纸缩成小图求平均亮度；
 * 仍然失败（权限/服务不可用）时退回小组件的浅色/深色设置。
 */
fun adaptiveWidgetTextColors(context: Context): WidgetTextColors {
    when (NiagaraWidgetSetting.textColor(context)) {
        NiagaraWidgetSetting.TEXT_BLACK -> return WidgetTextColors(
            primary = Color(0xFF111111),
            secondary = Color(0xCC111111),
        )

        NiagaraWidgetSetting.TEXT_WHITE -> return WidgetTextColors(
            primary = Color.White,
            secondary = Color(0xCCFFFFFF),
        )
    }

    val luminance = runCatching { wallpaperLuminance(context) }.getOrNull()
        ?: if (WidgetThemeSetting.isDark(context)) 0f else 1f

    return if (luminance > 0.55f) {
        WidgetTextColors(
            primary = Color(0xFF111111),
            secondary = Color(0xCC111111),
        )
    } else {
        WidgetTextColors(
            primary = Color.White,
            secondary = Color(0xCCFFFFFF),
        )
    }
}

private fun wallpaperLuminance(context: Context): Float? {
    val manager = WallpaperManager.getInstance(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        val colors = manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        val primary = colors?.primaryColor
        if (primary != null) {
            return ColorUtils.calculateLuminance(primary.toArgb()).toFloat()
        }
    }

    val drawable = manager.drawable ?: return null
    val size = 32
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        var sum = 0.0
        pixels.forEach { pixel ->
            sum += ColorUtils.calculateLuminance(pixel)
        }
        return (sum / pixels.size).toFloat()
    } finally {
        bitmap.recycle()
    }
}
