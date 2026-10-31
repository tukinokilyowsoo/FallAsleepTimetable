package io.github.fallasleeptimetable.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.graphics.Color

data class WidgetPalette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val secondaryText: Color,
    val outline: Color,
    val containers: List<Color>,
    val onContainers: List<Color>,
    val mutedContainer: Color,
    val mutedText: Color,
)

object WidgetThemeSetting {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    private const val PREFS = "settings"
    private const val KEY = "widget_theme"

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, SYSTEM) ?: SYSTEM

    fun set(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, value)
            .commit()
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

fun widgetPalette(context: Context): WidgetPalette {
    val dark = WidgetThemeSetting.isDark(context)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        monetPalette(context, dark)
    } else {
        fallbackPalette(dark)
    }
}

/**
 * 莫奈取色：使用系统动态色资源，按当前小组件样式取浅色/深色变体。
 */
private fun monetPalette(context: Context, dark: Boolean): WidgetPalette {
    val themed = themedContext(context, dark)
    fun c(colorRes: Int): Color = Color(themed.getColor(colorRes))

    return if (!dark) {
        WidgetPalette(
            background = c(android.R.color.system_neutral1_10),
            surface = c(android.R.color.system_neutral1_50),
            text = c(android.R.color.system_neutral1_900),
            secondaryText = c(android.R.color.system_neutral2_700),
            outline = c(android.R.color.system_neutral2_500),
            containers = listOf(
                c(android.R.color.system_accent1_100),
                c(android.R.color.system_accent2_100),
                c(android.R.color.system_accent3_100),
                c(android.R.color.system_neutral1_100),
                c(android.R.color.system_accent1_200),
                c(android.R.color.system_accent2_200),
            ),
            onContainers = listOf(
                c(android.R.color.system_accent1_900),
                c(android.R.color.system_accent2_900),
                c(android.R.color.system_accent3_900),
                c(android.R.color.system_neutral1_900),
                c(android.R.color.system_accent1_900),
                c(android.R.color.system_accent2_900),
            ),
            mutedContainer = c(android.R.color.system_neutral1_100),
            mutedText = c(android.R.color.system_neutral2_700),
        )
    } else {
        WidgetPalette(
            background = c(android.R.color.system_neutral1_900),
            surface = c(android.R.color.system_neutral1_800),
            text = c(android.R.color.system_neutral1_50),
            secondaryText = c(android.R.color.system_neutral2_200),
            outline = c(android.R.color.system_neutral2_600),
            containers = listOf(
                c(android.R.color.system_accent1_800),
                c(android.R.color.system_accent2_800),
                c(android.R.color.system_accent3_800),
                c(android.R.color.system_neutral1_800),
                c(android.R.color.system_accent1_700),
                c(android.R.color.system_accent2_700),
            ),
            onContainers = listOf(
                c(android.R.color.system_accent1_100),
                c(android.R.color.system_accent2_100),
                c(android.R.color.system_accent3_100),
                c(android.R.color.system_neutral1_100),
                c(android.R.color.system_accent1_100),
                c(android.R.color.system_accent2_100),
            ),
            mutedContainer = c(android.R.color.system_neutral1_800),
            mutedText = c(android.R.color.system_neutral2_200),
        )
    }
}

private fun themedContext(context: Context, dark: Boolean): Context {
    val configuration = Configuration(context.resources.configuration)
    configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
        if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    return context.createConfigurationContext(configuration)
}

private fun fallbackPalette(dark: Boolean): WidgetPalette {
    return if (!dark) {
        WidgetPalette(
            background = Color(0xFFFFFBFE),
            surface = Color(0xFFF3EDF7),
            text = Color(0xFF1B1B21),
            secondaryText = Color(0xFF45464F),
            outline = Color(0xFFCAC4D0),
            containers = listOf(
                Color(0xFFEADDFF),
                Color(0xFFD7E3FF),
                Color(0xFFFFD8E4),
                Color(0xFFFFE0B2),
                Color(0xFFD7E8CC),
                Color(0xFFCFE5F5),
            ),
            onContainers = listOf(
                Color(0xFF21005D),
                Color(0xFF001B3D),
                Color(0xFF31111D),
                Color(0xFF2B1700),
                Color(0xFF102010),
                Color(0xFF001E2C),
            ),
            mutedContainer = Color(0xFFE3E3E8),
            mutedText = Color(0xFF6F7078),
        )
    } else {
        WidgetPalette(
            background = Color(0xFF1B1B21),
            surface = Color(0xFF26232B),
            text = Color(0xFFE6E1E9),
            secondaryText = Color(0xFFC9C5D0),
            outline = Color(0xFF49454F),
            containers = listOf(
                Color(0xFF4F378B),
                Color(0xFF2F4677),
                Color(0xFF633B48),
                Color(0xFF5A3F00),
                Color(0xFF3A4E30),
                Color(0xFF27475A),
            ),
            onContainers = listOf(
                Color(0xFFEADDFF),
                Color(0xFFD7E3FF),
                Color(0xFFFFD8E4),
                Color(0xFFFFE0B2),
                Color(0xFFD7E8CC),
                Color(0xFFCFE5F5),
            ),
            mutedContainer = Color(0xFF34323A),
            mutedText = Color(0xFF9A98A2),
        )
    }
}
