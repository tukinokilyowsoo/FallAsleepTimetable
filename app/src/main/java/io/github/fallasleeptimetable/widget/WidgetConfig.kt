package io.github.fallasleeptimetable.widget

import android.content.Context

/** 每个桌面小组件实例独立的显示设置。 */
data class WidgetConfig(
    val timetableId: Long? = null,
    val showTeacher: Boolean = true,
    val showRoom: Boolean = true,
    val showUpcomingDays: Boolean = true,
)

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
