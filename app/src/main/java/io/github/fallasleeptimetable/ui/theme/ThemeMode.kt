package io.github.fallasleeptimetable.ui.theme

import android.content.Context
import android.content.res.Configuration

/**
 * 应用与桌面小组件共用的深色模式设置。
 * 旧版本的小组件样式（widget_theme）作为兼容来源，首次读取时迁移到 dark_mode。
 */
object ThemeMode {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    private const val PREFS = "settings"
    private const val KEY = "dark_mode"
    private const val LEGACY_WIDGET_KEY = "widget_theme"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY, null)
        if (stored != null) return stored
        val legacy = prefs.getString(LEGACY_WIDGET_KEY, null)
        if (legacy != null) {
            prefs.edit().putString(KEY, legacy).apply()
            return legacy
        }
        return SYSTEM
    }

    fun set(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, value)
            .apply()
    }

    fun isDark(context: Context): Boolean = when (get(context)) {
        DARK -> true
        LIGHT -> false
        else -> (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    fun label(value: String): String = when (value) {
        DARK -> "深色"
        LIGHT -> "浅色"
        else -> "跟随系统"
    }
}
