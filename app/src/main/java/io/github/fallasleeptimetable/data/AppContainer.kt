package io.github.fallasleeptimetable.data

import android.content.Context

object AppContainer {
    @Volatile
    private var repository: ScheduleRepository? = null

    fun repository(context: Context): ScheduleRepository =
        repository ?: synchronized(this) {
            repository ?: ScheduleRepository(AppDatabase.get(context)).also { repository = it }
        }
}
