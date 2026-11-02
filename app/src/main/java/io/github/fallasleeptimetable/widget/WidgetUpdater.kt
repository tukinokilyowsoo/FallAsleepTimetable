package io.github.fallasleeptimetable.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 小组件刷新的唯一入口。
 *
 * 所有触发（打开应用、改课程、换壁纸、下课闹钟等）都投递到这个合并通道：
 * 短时间内的多次请求只执行一次，避免修改课程时连续重绘三套小组件。
 */
object WidgetUpdater {
    private const val COALESCE_MS = 120L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requests = Channel<Context>(Channel.CONFLATED)

    init {
        scope.launch {
            for (first in requests) {
                delay(COALESCE_MS)
                var context = first
                while (true) {
                    val next = requests.tryReceive().getOrNull() ?: break
                    context = next
                }
                runCatching { updateAllNow(context) }
            }
        }
    }

    fun updateAll(context: Context) {
        requests.trySend(context.applicationContext)
    }

    private suspend fun updateAllNow(context: Context) {
        runCatching { TodayCoursesWidget().updateAll(context) }
        runCatching { WeekScheduleWidget().updateAll(context) }
        runCatching { NiagaraTodayWidget().updateAll(context) }
        runCatching { WidgetRefreshScheduler.scheduleNext(context) }
    }
}
