package io.github.fallasleeptimetable.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WidgetUpdater {
    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { TodayCoursesWidget().updateAll(appContext) }
            runCatching { WeekScheduleWidget().updateAll(appContext) }
        }
    }
}
