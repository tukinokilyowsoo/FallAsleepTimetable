package io.github.fallasleeptimetable.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 下课闹钟、跨天/改时间/改时区、开机后刷新小组件并安排下一次唤醒。 */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != ACTION_REFRESH && action !in SYSTEM_ACTIONS) return
        WidgetUpdater.updateAll(context)
    }

    companion object {
        const val ACTION_REFRESH = "io.github.fallasleeptimetable.action.REFRESH_WIDGETS"

        private val SYSTEM_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_WALLPAPER_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )

        fun refreshIntent(context: Context): Intent =
            Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_REFRESH)
    }
}
