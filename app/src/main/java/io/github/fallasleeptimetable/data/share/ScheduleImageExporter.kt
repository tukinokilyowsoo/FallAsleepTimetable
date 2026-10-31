package io.github.fallasleeptimetable.data.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import io.github.fallasleeptimetable.data.CourseWithSessions
import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.data.TimeSlotEntity
import io.github.fallasleeptimetable.domain.VisibleSession
import io.github.fallasleeptimetable.domain.assignLanes
import io.github.fallasleeptimetable.domain.visibleSessions
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.floor

/**
 * 用 Android Canvas 离屏绘制课表长图，不经过 WebView。
 *
 * 单周图按 2 倍尺寸输出；整学期图按 1 倍尺寸输出，避免位图过大。
 */
object ScheduleImageExporter {
    private val weekDateFormat = DateTimeFormatter.ofPattern("MM/dd")

    data class Options(
        val includeTeacher: Boolean = true,
        val includeRoom: Boolean = true,
        val allWeeks: Boolean = false,
        val palette: SharePalette,
    )

    fun renderPng(
        term: TermEntity,
        timetableName: String,
        courses: List<CourseWithSessions>,
        timeSlots: List<TimeSlotEntity>,
        currentWeek: Int,
        options: Options,
    ): ByteArray? {
        if (courses.none { it.sessions.isNotEmpty() }) return null

        val weeks = if (options.allWeeks) {
            (1..term.totalWeeks.coerceAtLeast(1)).toList()
        } else {
            listOf(currentWeek.coerceIn(1, term.totalWeeks.coerceAtLeast(1)))
        }
        val days = buildList {
            addAll(1..5)
            if (term.showSaturday) add(6)
            if (term.showSunday) add(7)
        }

        val dense = options.allWeeks && weeks.size * term.periodsPerDay > 320
        val unit = when {
            !options.allWeeks -> 2f
            dense -> 0.8f
            else -> 1f
        }
        fun dp(value: Float): Float = value * unit

        val horizontalPadding = dp(22f)
        val timeColumnWidth = dp(46f)
        val dayColumnWidth = dp(92f)
        val headerHeight = dp(40f)
        val rowHeight = dp(54f)
        val titleHeight = dp(88f)
        val weekLabelHeight = dp(36f)
        val weekGap = dp(18f)
        val footerHeight = dp(26f)

        val width = (horizontalPadding * 2 + timeColumnWidth +
            dayColumnWidth * days.size).toInt()
        val gridHeight = headerHeight + rowHeight * term.periodsPerDay
        val weekHeight = weekLabelHeight + gridHeight
        val height = (
            titleHeight +
                weekHeight * weeks.size +
                weekGap * (weeks.size - 1).coerceAtLeast(0) +
                footerHeight
            ).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(options.palette.background)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val palette = options.palette
            val startDate = LocalDate.ofEpochDay(term.startDateEpochDay)
            val slotByPeriod = timeSlots.associateBy { it.period }

            drawTitle(
                canvas = canvas,
                paint = paint,
                palette = palette,
                x = horizontalPadding,
                top = dp(14f),
                title = timetableName.ifBlank { "我的课表" },
                subtitle = if (options.allWeeks) {
                    "${term.name} · 共 ${term.totalWeeks} 周"
                } else {
                    val week = weeks.first()
                    val start = startDate.plusWeeks((week - 1).toLong())
                    "${term.name} · 第 $week 周 · " +
                        "${start.format(weekDateFormat)} – " +
                        "${start.plusDays(6).format(weekDateFormat)}"
                },
                titleSize = dp(22f),
                subtitleSize = dp(12f),
                unit = unit,
            )

            var weekTop = titleHeight
            weeks.forEachIndexed { index, week ->
                val weekStart = startDate.plusWeeks((week - 1).toLong())
                drawWeek(
                    canvas = canvas,
                    paint = paint,
                    palette = palette,
                    term = term,
                    days = days,
                    week = week,
                    weekStart = weekStart,
                    sessions = visibleSessions(courses, week),
                    slotByPeriod = slotByPeriod,
                    weekTop = weekTop,
                    isCurrentWeek = week == currentWeek,
                    includeTeacher = options.includeTeacher,
                    includeRoom = options.includeRoom,
                    unit = unit,
                    horizontalPadding = horizontalPadding,
                    timeColumnWidth = timeColumnWidth,
                    dayColumnWidth = dayColumnWidth,
                    headerHeight = headerHeight,
                    rowHeight = rowHeight,
                    weekLabelHeight = weekLabelHeight,
                )
                weekTop += weekHeight
                if (index != weeks.lastIndex) weekTop += weekGap
            }

            paint.textSize = dp(10f)
            paint.color = palette.onSurfaceVariant
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                "由 FallAsleep 课程表生成",
                width - horizontalPadding,
                height - dp(8f),
                paint,
            )

            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            return output.toByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    private fun drawTitle(
        canvas: Canvas,
        paint: Paint,
        palette: SharePalette,
        x: Float,
        top: Float,
        title: String,
        subtitle: String,
        titleSize: Float,
        subtitleSize: Float,
        unit: Float,
    ) {
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = titleSize
        paint.color = palette.onSurface
        canvas.drawText(title, x, top + titleSize, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = subtitleSize
        paint.color = palette.onSurfaceVariant
        canvas.drawText(subtitle, x, top + titleSize + subtitleSize * 1.9f, paint)
    }

    private fun drawWeek(
        canvas: Canvas,
        paint: Paint,
        palette: SharePalette,
        term: TermEntity,
        days: List<Int>,
        week: Int,
        weekStart: LocalDate,
        sessions: List<VisibleSession>,
        slotByPeriod: Map<Int, TimeSlotEntity>,
        weekTop: Float,
        isCurrentWeek: Boolean,
        includeTeacher: Boolean,
        includeRoom: Boolean,
        unit: Float,
        horizontalPadding: Float,
        timeColumnWidth: Float,
        dayColumnWidth: Float,
        headerHeight: Float,
        rowHeight: Float,
        weekLabelHeight: Float,
    ) {
        fun dp(value: Float): Float = value * unit

        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = dp(14f)
        paint.color = if (isCurrentWeek) palette.primary else palette.onSurfaceVariant
        val label = buildString {
            append("第 ").append(week).append(" 周 · ")
            append(weekStart.format(weekDateFormat)).append(" – ")
            append(weekStart.plusDays(6).format(weekDateFormat))
            if (isCurrentWeek) append("（本周）")
        }
        canvas.drawText(label, horizontalPadding, weekTop + dp(22f), paint)
        paint.typeface = Typeface.DEFAULT

        val gridTop = weekTop + weekLabelHeight
        val gridLeft = horizontalPadding
        val gridRight = horizontalPadding + timeColumnWidth + dayColumnWidth * days.size
        val gridBottom = gridTop + headerHeight + rowHeight * term.periodsPerDay

        paint.strokeWidth = dp(0.8f)
        paint.color = palette.outlineVariant
        paint.alpha = 0x66
        for (period in 0..term.periodsPerDay) {
            val y = gridTop + headerHeight + rowHeight * period
            canvas.drawLine(gridLeft, y, gridRight, y, paint)
        }
        canvas.drawLine(gridLeft, gridTop, gridLeft, gridBottom, paint)
        canvas.drawLine(gridLeft + timeColumnWidth, gridTop, gridLeft + timeColumnWidth, gridBottom, paint)
        for (index in days.indices) {
            val x = gridLeft + timeColumnWidth + dayColumnWidth * index
            canvas.drawLine(x, gridTop, x, gridBottom, paint)
        }
        canvas.drawLine(gridRight, gridTop, gridRight, gridBottom, paint)
        paint.alpha = 255

        days.forEachIndexed { index, day ->
            val centerX = gridLeft + timeColumnWidth + dayColumnWidth * index + dayColumnWidth / 2
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = dp(13f)
            paint.color = palette.onSurface
            canvas.drawText(dayLabel(day), centerX, gridTop + dp(17f), paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = dp(10f)
            paint.color = palette.onSurfaceVariant
            val date = weekStart.plusDays((day - 1).toLong())
            canvas.drawText(
                date.format(weekDateFormat),
                centerX,
                gridTop + dp(31f),
                paint,
            )
            paint.textAlign = Paint.Align.LEFT
        }

        for (period in 1..term.periodsPerDay) {
            val cellTop = gridTop + headerHeight + rowHeight * (period - 1)
            val centerX = gridLeft + timeColumnWidth / 2
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = dp(12f)
            paint.color = palette.onSurface
            canvas.drawText(period.toString(), centerX, cellTop + rowHeight * 0.42f, paint)

            slotByPeriod[period]?.let { slot ->
                paint.textSize = dp(9f)
                paint.color = palette.onSurfaceVariant
                canvas.drawText(
                    formatMinutes(slot.startMinute),
                    centerX,
                    cellTop + rowHeight * 0.72f,
                    paint,
                )
            }
            paint.textAlign = Paint.Align.LEFT
        }

        sessions.groupBy { it.dayOfWeek }.forEach { (day, daySessions) ->
            val dayIndex = days.indexOf(day)
            if (dayIndex < 0) return@forEach
            val dayLeft = gridLeft + timeColumnWidth + dayColumnWidth * dayIndex
            val (lanes, laneCount) = assignLanes(daySessions)
            daySessions.forEach { session ->
                val lane = lanes[session.sessionId] ?: 0
                val laneWidth = dayColumnWidth / laneCount
                val blockLeft = dayLeft + laneWidth * lane
                val blockTop = gridTop + headerHeight + rowHeight * (session.startPeriod - 1)
                val blockHeight = rowHeight * session.periodCount
                drawSessionBlock(
                    canvas = canvas,
                    paint = paint,
                    palette = palette,
                    session = session,
                    includeTeacher = includeTeacher,
                    includeRoom = includeRoom,
                    left = blockLeft,
                    top = blockTop,
                    width = laneWidth,
                    height = blockHeight,
                    unit = unit,
                )
            }
        }
    }

    private fun drawSessionBlock(
        canvas: Canvas,
        paint: Paint,
        palette: SharePalette,
        session: VisibleSession,
        includeTeacher: Boolean,
        includeRoom: Boolean,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        unit: Float,
    ) {
        fun dp(value: Float): Float = value * unit

        val inset = dp(2f)
        val innerPadding = dp(5f)
        val blockLeft = left + inset
        val blockTop = top + inset
        val blockRight = left + width - inset
        val blockBottom = top + height - inset
        if (blockRight - blockLeft < dp(12f) || blockBottom - blockTop < dp(12f)) return

        paint.style = Paint.Style.FILL
        paint.color = palette.container(session.colorIndex)
        canvas.drawRoundRect(
            blockLeft,
            blockTop,
            blockRight,
            blockBottom,
            dp(9f),
            dp(9f),
            paint,
        )

        val textLeft = blockLeft + innerPadding
        val textWidth = blockRight - blockLeft - innerPadding * 2
        if (textWidth <= 0) return
        val contentColor = palette.content(session.colorIndex)
        val nameSize = dp(12f)
        val detailSize = dp(9.5f)
        val nameLineHeight = nameSize * 1.22f
        val detailLineHeight = detailSize * 1.25f
        val availableHeight = blockBottom - blockTop - innerPadding * 2

        val detailLines = (if (includeTeacher && !session.teacher.isNullOrBlank()) 1 else 0) +
            (if (includeRoom && !session.room.isNullOrBlank()) 1 else 0)
        val maxNameLines = floor(
            (availableHeight - detailLines * detailLineHeight) / nameLineHeight,
        ).toInt().coerceIn(1, 3)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = nameSize
        paint.color = contentColor
        paint.alpha = 255
        var baseline = blockTop + innerPadding + nameSize
        baseline = drawWrappedText(
            canvas = canvas,
            paint = paint,
            text = session.name,
            x = textLeft,
            firstBaseline = baseline,
            maxWidth = textWidth,
            maxLines = maxNameLines,
            lineHeight = nameLineHeight,
        )

        paint.typeface = Typeface.DEFAULT
        paint.textSize = detailSize
        paint.color = contentColor
        paint.alpha = 0xCC
        if (includeTeacher) {
            session.teacher?.takeIf { it.isNotBlank() }?.let { teacher ->
                if (baseline + detailSize <= blockBottom - innerPadding) {
                    canvas.drawText(
                        ellipsize(paint, teacher, textWidth),
                        textLeft,
                        baseline,
                        paint,
                    )
                    baseline += detailLineHeight
                }
            }
        }
        if (includeRoom) {
            session.room?.takeIf { it.isNotBlank() }?.let { room ->
                if (baseline + detailSize <= blockBottom - innerPadding) {
                    canvas.drawText(
                        ellipsize(paint, room, textWidth),
                        textLeft,
                        baseline,
                        paint,
                    )
                }
            }
        }
        paint.alpha = 255
    }

    private fun drawWrappedText(
        canvas: Canvas,
        paint: Paint,
        text: String,
        x: Float,
        firstBaseline: Float,
        maxWidth: Float,
        maxLines: Int,
        lineHeight: Float,
    ): Float {
        if (text.isEmpty() || maxWidth <= 0f) return firstBaseline
        var remaining = text
        var baseline = firstBaseline
        var line = 0
        while (remaining.isNotEmpty() && line < maxLines) {
            val count = paint.breakText(remaining, true, maxWidth, null)
            if (count <= 0) break
            val lastLine = line == maxLines - 1
            val chunk = if (lastLine && count < remaining.length) {
                ellipsize(paint, remaining.substring(0, count), maxWidth).also {
                    remaining = ""
                }
            } else {
                remaining.substring(0, count).also {
                    remaining = remaining.substring(count)
                }
            }
            canvas.drawText(chunk, x, baseline, paint)
            baseline += lineHeight
            line++
        }
        return baseline
    }

    private fun ellipsize(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 0) {
            val candidate = text.substring(0, end).trimEnd() + "…"
            if (paint.measureText(candidate) <= maxWidth) return candidate
            end--
        }
        return "…"
    }

    private fun dayLabel(day: Int): String {
        val names = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        return names.getOrElse(day - 1) { "周一" }
    }

    private fun formatMinutes(minutes: Int): String =
        "%02d:%02d".format(minutes / 60, minutes % 60)
}
