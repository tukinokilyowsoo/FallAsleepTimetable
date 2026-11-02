package io.github.fallasleeptimetable.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 下课闹钟、跨天/改时间/改时区、开机后刷新小组件并安排下一次唤醒。 */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        WidgetUpdater.updateAll(context)
    }

    companion object {
        const val ACTION_REFRESH = "io.github.fallasleeptimetable.action.REFRESH_WIDGETS"

        fun refreshIntent(context: Context): Intent =
            Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_REFRESH)
    }
}
