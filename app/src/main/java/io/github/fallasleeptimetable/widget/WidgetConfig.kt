package io.github.fallasleeptimetable.widget

import android.content.Context

/** 每个桌面小组件实例独立的显示设置。 */
data class WidgetConfig(
    val timetableId: Long? = null,
    val showTeacher: Boolean = true,
    val showRoom: Boolean = true,
    val showUpcomingDays: Boolean = true,
)

/** Niagara 透明今日课程组件：全局纵向显示行数与文字颜色。 */
object NiagaraWidgetSetting {
    const val MIN_ROWS = 1
    const val MAX_ROWS = 12
    const val DEFAULT_ROWS = 4

    const val TEXT_AUTO = "auto"
    const val TEXT_BLACK = "black"
    const val TEXT_WHITE = "white"

    const val FONT_AUTO = 0
    const val MIN_FONT_SP = 10
    const val MAX_FONT_SP = 32

    private const val PREFS = "settings"
    private const val KEY_ROWS = "niagara_display_count"
    private const val KEY_TEXT_COLOR = "niagara_text_color"
    private const val KEY_FONT_SIZE = "niagara_font_size"

    fun rows(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_ROWS, DEFAULT_ROWS)
            .coerceIn(MIN_ROWS, MAX_ROWS)

    fun setRows(context: Context, rows: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_ROWS, rows.coerceIn(MIN_ROWS, MAX_ROWS))
            .apply()
    }

    fun textColor(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TEXT_COLOR, TEXT_AUTO)
            ?.takeIf { it in setOf(TEXT_AUTO, TEXT_BLACK, TEXT_WHITE) }
            ?: TEXT_AUTO

    fun setTextColor(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TEXT_COLOR, value)
            .apply()
    }

    fun textColorLabel(value: String): String = when (value) {
        TEXT_BLACK -> "黑色"
        TEXT_WHITE -> "白色"
        else -> "自动（跟随壁纸）"
    }

    /** 0 表示自动按组件大小排版，其余为课程名字号（sp）。 */
    fun fontSize(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_FONT_SIZE, FONT_AUTO)
            .coerceIn(FONT_AUTO, MAX_FONT_SP)

    fun setFontSize(context: Context, sp: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_FONT_SIZE, sp.coerceIn(FONT_AUTO, MAX_FONT_SP))
            .apply()
    }

    fun fontSizeLabel(sp: Int): String =
        if (sp <= FONT_AUTO) "自动" else "${sp}sp"
}

object WidgetConfigStore {
    private const val PREFS = "settings"

    private fun key(appWidgetId: Int, name: String) = "widget_${appWidgetId}_$name"

    fun load(context: Context, appWidgetId: Int): WidgetConfig {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val timetableId = prefs.getLong(key(appWidgetId, "timetable"), -1L)
        return WidgetConfig(
            timetableId = timetableId.takeIf { it > 0 },
            showTeacher = prefs.getBoolean(key(appWidgetId, "show_teacher"), true),
            showRoom = prefs.getBoolean(key(appWidgetId, "show_room"), true),
            showUpcomingDays = prefs.getBoolean(
                key(appWidgetId, "show_upcoming"),
                true,
            ),
        )
    }

    fun save(context: Context, appWidgetId: Int, config: WidgetConfig) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(key(appWidgetId, "timetable"), config.timetableId ?: -1L)
            .putBoolean(key(appWidgetId, "show_teacher"), config.showTeacher)
            .putBoolean(key(appWidgetId, "show_room"), config.showRoom)
            .putBoolean(key(appWidgetId, "show_upcoming"), config.showUpcomingDays)
            .apply()
    }

    fun delete(context: Context, appWidgetId: Int) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        editor.remove(key(appWidgetId, "timetable"))
        editor.remove(key(appWidgetId, "show_teacher"))
        editor.remove(key(appWidgetId, "show_room"))
        editor.remove(key(appWidgetId, "show_upcoming"))
        editor.apply()
    }
}
