package io.github.fallasleeptimetable.data.calendar

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import io.github.fallasleeptimetable.data.CourseWithSessions
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.data.TimeSlotEntity
import java.time.LocalDate
import java.time.ZoneId

/**
 * 把当前课表写入系统日历的本地日历「FallAsleep课程表」，由系统日历负责提醒。
 *
 * 每次导入会先清空该日历里之前导入的事件，再按课程安排重建。
 */
object CalendarScheduleExporter {
    private const val ACCOUNT_NAME = "FallAsleep"
    private const val ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL
    private const val CALENDAR_NAME = "FallAsleep课程表"
    private const val CALENDAR_DISPLAY_NAME = "FallAsleep课程表"
    private const val CALENDAR_COLOR = 0xFF4A5C92.toInt()
    private const val REMINDER_MINUTES = 10

    data class ExportResult(
        val eventCount: Int,
        val calendarDisplayName: String,
    )

    fun export(
        context: Context,
        term: TermEntity,
        timetableId: Long,
        courses: List<CourseWithSessions>,
        timeSlots: List<TimeSlotEntity>,
        reminderMinutes: Int = REMINDER_MINUTES,
    ): ExportResult {
        require(courses.isNotEmpty()) { "当前课表还没有课程" }

        val resolver = context.contentResolver
        val startDate = LocalDate.ofEpochDay(term.startDateEpochDay)
        val zone = ZoneId.systemDefault()
        val slotByPeriod = timeSlots.associateBy { it.period }

        val calendarId = findOrCreateCalendar(resolver, zone)
        clearCalendar(resolver, calendarId)

        var eventCount = 0

        courses.forEach { entry ->
            entry.sessions.forEach { session ->
                val activeWeeks = activeWeeks(session)
                if (activeWeeks.isEmpty()) return@forEach

                val day = session.dayOfWeek
                if (day !in 1..7) return@forEach
                val startSlot = slotByPeriod[session.startPeriod] ?: return@forEach
                val endPeriod = session.startPeriod + session.periodCount - 1
                val endSlot = slotByPeriod[endPeriod] ?: return@forEach
                if (endSlot.endMinute <= startSlot.startMinute) return@forEach

                val firstWeek = activeWeeks.first()
                val date = startDate
                    .plusWeeks((firstWeek - 1).toLong())
                    .plusDays((day - 1).toLong())

                val startMillis = date
                    .atTime(startSlot.startMinute / 60, startSlot.startMinute % 60)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
                val endMillis = date
                    .atTime(endSlot.endMinute / 60, endSlot.endMinute % 60)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()

                val rrule = buildString {
                    append("FREQ=WEEKLY;")
                    if (session.parity != 0) append("INTERVAL=2;")
                    append("COUNT=").append(activeWeeks.size)
                }

                val description = buildString {
                    append("教师：").append(entry.course.teacher ?: "未填写").append('\n')
                    append("节次：第 ").append(session.startPeriod)
                    if (session.periodCount > 1) {
                        append('-').append(session.startPeriod + session.periodCount - 1)
                    }
                    append(" 节\n")
                    append("周次：")
                    append(
                        weekText(
                            session.startWeek,
                            session.endWeek,
                            session.parity,
                        ),
                    )
                    append("\n由 FallAsleep 课程表导入")
                }

                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.TITLE, entry.course.name)
                    put(CalendarContract.Events.DESCRIPTION, description)
                    put(CalendarContract.Events.EVENT_LOCATION, session.room ?: "")
                    put(CalendarContract.Events.CUSTOM_APP_URI, appUri(timetableId))
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
                    put(CalendarContract.Events.RRULE, rrule)
                    put(CalendarContract.Events.HAS_ALARM, 1)
                    put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                }

                val eventUri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    ?: return@forEach
                val eventId = ContentUris.parseId(eventUri)

                resolver.insert(
                    CalendarContract.Reminders.CONTENT_URI,
                    ContentValues().apply {
                        put(CalendarContract.Reminders.EVENT_ID, eventId)
                        put(CalendarContract.Reminders.MINUTES, reminderMinutes)
                        put(
                            CalendarContract.Reminders.METHOD,
                            CalendarContract.Reminders.METHOD_ALERT,
                        )
                    },
                )
                eventCount++
            }
        }

        return ExportResult(
            eventCount = eventCount,
            calendarDisplayName = CALENDAR_DISPLAY_NAME,
        )
    }

    fun deleteAll(context: Context): Int {
        val resolver = context.contentResolver
        val calendarId = findCalendar(resolver) ?: return 0
        return deleteEvents(
            resolver,
            "${CalendarContract.Events.CALENDAR_ID}=?",
            arrayOf(calendarId.toString()),
        )
    }

    fun deleteForTimetable(context: Context, timetableId: Long): Int {
        val resolver = context.contentResolver
        val calendarId = findCalendar(resolver) ?: return 0
        return deleteEvents(
            resolver,
            "${CalendarContract.Events.CALENDAR_ID}=? AND " +
                "${CalendarContract.Events.CUSTOM_APP_URI}=?",
            arrayOf(calendarId.toString(), appUri(timetableId)),
        )
    }

    private fun appUri(timetableId: Long): String = "fallasleep://timetable/$timetableId"

    private fun findCalendar(resolver: ContentResolver): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME}=? AND " +
            "${CalendarContract.Calendars.ACCOUNT_TYPE}=? AND " +
            "${CalendarContract.Calendars.NAME}=?"
        val args = arrayOf(ACCOUNT_NAME, ACCOUNT_TYPE, CALENDAR_NAME)

        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            args,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    private fun findOrCreateCalendar(resolver: ContentResolver, zone: ZoneId): Long {
        findCalendar(resolver)?.let { return it }

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            put(CalendarContract.Calendars.NAME, CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_DISPLAY_NAME)
            put(CalendarContract.Calendars.CALENDAR_COLOR, CALENDAR_COLOR)
            put(
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.CAL_ACCESS_OWNER,
            )
            put(CalendarContract.Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, zone.id)
        }

        val calendarUri = asSyncAdapter(CalendarContract.Calendars.CONTENT_URI)
        return resolver.insert(calendarUri, values)?.let { ContentUris.parseId(it) }
            ?: error("无法创建本地日历，请检查日历权限或系统日历服务")
    }

    private fun clearCalendar(resolver: ContentResolver, calendarId: Long) {
        deleteEvents(
            resolver,
            "${CalendarContract.Events.CALENDAR_ID}=?",
            arrayOf(calendarId.toString()),
        )
    }

    private fun deleteEvents(
        resolver: ContentResolver,
        selection: String,
        args: Array<String>,
    ): Int {
        val ids = mutableListOf<Long>()
        resolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID),
            selection,
            args,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0))
            }
        }

        ids.forEach { eventId ->
            resolver.delete(
                CalendarContract.Reminders.CONTENT_URI,
                "${CalendarContract.Reminders.EVENT_ID}=?",
                arrayOf(eventId.toString()),
            )
        }

        resolver.delete(
            asSyncAdapter(CalendarContract.Events.CONTENT_URI),
            selection,
            args,
        )
        return ids.size
    }

    private fun asSyncAdapter(uri: Uri): Uri = uri.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
        .build()

    private fun activeWeeks(
        session: io.github.fallasleeptimetable.data.CourseSessionEntity,
    ): List<Int> {
        val range = session.startWeek..session.endWeek
        return when (session.parity) {
            1 -> range.filter { it % 2 == 1 }
            2 -> range.filter { it % 2 == 0 }
            else -> range.toList()
        }
    }

    private fun weekText(startWeek: Int, endWeek: Int, parity: Int): String {
        val base = if (startWeek == endWeek) {
            "第 $startWeek 周"
        } else {
            "第 $startWeek-$endWeek 周"
        }
        return when (parity) {
            1 -> "$base（单周）"
            2 -> "$base（双周）"
            else -> base
        }
    }
}
