package io.github.fallasleeptimetable.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 下一次需要刷新小组件的时刻：今天最近的课程结束时间；没有则次日零点。
 * 结束时间等于当前分钟时视为已下课。
 */
internal fun nextWidgetRefreshAt(
    now: LocalDateTime,
    endMinutes: List<Int>,
): LocalDateTime {
    val today = now.toLocalDate()
    val nowMinute = now.hour * 60 + now.minute
    return endMinutes.asSequence()
        .filter { it > nowMinute }
        .distinct()
        .sorted()
        .firstOrNull()
        ?.let { minute -> today.atTime(minute / 60, minute % 60) }
        ?.takeIf { it.isAfter(now) }
        ?: today.plusDays(1).atStartOfDay()
}

/**
 * 在下一节课结束或跨天时唤醒小组件，让今日课程在下课后一分钟内消失。
 *
 * - 没有添加任何小组件时不排闹钟，并取消旧闹钟；
 * - 只有周课表组件时只需次日零点刷新；
 * - Android 12+ 需要「闹钟和提醒」权限才能精确唤醒，没有权限时降级为
 *   setAndAllowWhileIdle，同时仍有 30 分钟的系统周期刷新兜底。
 *
 * 由 [WidgetUpdater] 串行调用，不再自建协程。
 */
object WidgetRefreshScheduler {
    private const val REQUEST_CODE = 0x4641

    suspend fun scheduleNext(context: Context) {
        val appContext = context.applicationContext
        val manager = GlanceAppWidgetManager(appContext)

        val todayIds = glanceIds(manager, TodayCoursesWidget::class.java)
        val niagaraIds = glanceIds(manager, NiagaraTodayWidget::class.java)
        val weekIds = runCatching {
            manager.getGlanceIds(WeekScheduleWidget::class.java)
        }.getOrDefault(emptyList())

        if (todayIds.isEmpty() && niagaraIds.isEmpty() && weekIds.isEmpty()) {
            cancelAlarm(appContext)
            return
        }

        val now = LocalDateTime.now()
        val today = now.toLocalDate()

        // 只有周课表组件时，内容只在跨天时变化。
        val needsClassEndRefresh = todayIds.isNotEmpty() || niagaraIds.isNotEmpty()
        val endMinutes = if (needsClassEndRefresh) {
            (todayIds + niagaraIds).flatMap { appWidgetId ->
                val data = runCatching {
                    WidgetDataLoader.load(appContext, appWidgetId)
                }.getOrNull() ?: return@flatMap emptyList()
                val week = data.actualWeek(today)
                if (week !in 1..data.term.totalWeeks) {
                    emptyList()
                } else {
                    data.sessionsForWeek(week)
                        .filter { it.dayOfWeek == today.dayOfWeek.value }
                        .map { it.endMinute }
                }
            }
        } else {
            emptyList()
        }

        val trigger = if (needsClassEndRefresh) {
            nextWidgetRefreshAt(now, endMinutes)
        } else {
            today.plusDays(1).atStartOfDay()
        }

        scheduleAlarm(
            appContext,
            trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(AlarmManager::class.java) ?: return false
        return manager.canScheduleExactAlarms()
    }

    private suspend fun <T : GlanceAppWidget> glanceIds(
        manager: GlanceAppWidgetManager,
        widgetClass: Class<T>,
    ): List<Int> = runCatching {
        manager.getGlanceIds(widgetClass).map { manager.getAppWidgetId(it) }
    }.getOrDefault(emptyList())

    private fun scheduleAlarm(context: Context, atMillis: Long) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            WidgetRefreshReceiver.refreshIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        runCatching {
            if (canScheduleExact(context)) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    atMillis,
                    pendingIntent,
                )
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    atMillis,
                    pendingIntent,
                )
            }
        }.onFailure {
            manager.set(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            WidgetRefreshReceiver.refreshIntent(context),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        manager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
