package io.github.fallasleeptimetable.data

import androidx.room.withTransaction
import io.github.fallasleeptimetable.data.importer.ImportedTimetable
import io.github.fallasleeptimetable.data.importer.TimetableFileImporter
import io.github.fallasleeptimetable.domain.CourseDraft
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class ImportResult(
    val timetableId: Long,
    val courseCount: Int,
    val timetableName: String,
)

class ScheduleRepository(private val db: AppDatabase) {
    private val termDao = db.termDao()
    private val timetableDao = db.timetableDao()
    private val courseDao = db.courseDao()
    private val timeSlotDao = db.timeSlotDao()

    val term: Flow<TermEntity?> = termDao.observeFirst()
    val timetables: Flow<List<TimetableEntity>> = timetableDao.observeAll()

    fun courses(timetableId: Long): Flow<List<CourseWithSessions>> =
        courseDao.observeWithSessions(timetableId)

    fun timeSlots(timetableId: Long) = timeSlotDao.observeByTimetable(timetableId)

    suspend fun loadTimeSlots(timetableId: Long) = timeSlotDao.getByTimetable(timetableId)

    suspend fun ensureSeed(debugSample: Boolean) {
        if (termDao.count() > 0) return
        db.withTransaction {
            val today = LocalDate.now()
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val start = if (debugSample) monday.minusWeeks(7) else monday
            val termId = termDao.insert(
                TermEntity(
                    name = "本学期",
                    startDateEpochDay = start.toEpochDay(),
                    totalWeeks = 20,
                    periodsPerDay = 12,
                    showSaturday = true,
                    showSunday = false,
                ),
            )
            val tableId = timetableDao.insert(
                TimetableEntity(termId = termId, name = "我的课表", periodsPerDay = 12),
            )
            timeSlotDao.insertAll(defaultTimeSlots(tableId, 1..12))
            if (debugSample) insertSampleCourses(tableId)
        }
    }

    suspend fun saveCourse(
        timetableId: Long,
        courseId: Long?,
        sessionId: Long?,
        draft: CourseDraft,
    ) {
        db.withTransaction {
            val id = if (courseId == null) {
                courseDao.insertCourse(
                    CourseEntity(
                        timetableId = timetableId,
                        name = draft.name.trim(),
                        teacher = draft.teacher?.trim()?.ifBlank { null },
                        colorIndex = draft.colorIndex,
                    ),
                )
            } else {
                val existing = courseDao.getCourse(courseId) ?: return@withTransaction
                courseDao.updateCourse(
                    existing.copy(
                        name = draft.name.trim(),
                        teacher = draft.teacher?.trim()?.ifBlank { null },
                        colorIndex = draft.colorIndex,
                    ),
                )
                courseId
            }

            val session = CourseSessionEntity(
                id = sessionId ?: 0,
                courseId = id,
                dayOfWeek = draft.dayOfWeek,
                startPeriod = draft.startPeriod,
                periodCount = draft.periodCount,
                startWeek = draft.startWeek,
                endWeek = draft.endWeek,
                parity = draft.parity,
                room = draft.room?.trim()?.ifBlank { null },
            )
            if (sessionId == null) {
                courseDao.insertSession(session)
            } else {
                courseDao.updateSession(session)
            }
        }
    }

    suspend fun deleteCourse(courseId: Long) {
        courseDao.deleteCourse(courseId)
    }

    suspend fun searchCourses(query: String): List<CourseSearchRow> {
        val keyword = query.trim()
        if (keyword.isEmpty()) return emptyList()
        return courseDao.search(keyword)
    }

    suspend fun updateTerm(updated: TermEntity) {
        termDao.update(updated)
    }

    /**
     * 写入某张课表的上课时间。applyGlobally 为 true 时同步覆盖其他所有课表。
     */
    suspend fun replaceTimeSlots(
        timetableId: Long,
        periodsPerDay: Int,
        slots: List<Pair<Int, Int>>,
        applyGlobally: Boolean = false,
    ) {
        require(slots.isNotEmpty()) { "至少需要一节课" }
        db.withTransaction {
            val table = timetableDao.get(timetableId) ?: return@withTransaction
            writeTimeSlots(table, periodsPerDay, slots)
            if (applyGlobally) {
                timetableDao.getAll()
                    .filter { it.id != table.id }
                    .forEach { other -> writeTimeSlots(other, periodsPerDay, slots) }
            }
        }
    }

    private suspend fun writeTimeSlots(
        table: TimetableEntity,
        periodsPerDay: Int,
        slots: List<Pair<Int, Int>>,
    ) {
        timetableDao.update(table.copy(periodsPerDay = periodsPerDay))
        timeSlotDao.deleteByTimetable(table.id)
        timeSlotDao.insertAll(
            slots.mapIndexed { index, (start, end) ->
                TimeSlotEntity(
                    timetableId = table.id,
                    period = index + 1,
                    startMinute = start,
                    endMinute = end,
                )
            },
        )
    }

    /**
     * 新建课表默认沿用当前课表的上课时间配置。
     */
    suspend fun createTimetable(
        termId: Long,
        name: String,
        sourceTimetableId: Long? = null,
    ): Long = db.withTransaction {
        val order = timetableDao.getAll().size
        val source = sourceTimetableId?.let { timetableDao.get(it) }
        val sourceSlots = source?.let { timeSlotDao.getByTimetable(it.id) }
            .orEmpty()
            .map { it.startMinute to it.endMinute }
        val slots = sourceSlots.ifEmpty {
            defaultTimeSlots(0, 1..12).map { it.startMinute to it.endMinute }
        }
        val periods = source?.periodsPerDay?.takeIf { it > 0 } ?: slots.size
        val table = TimetableEntity(
            termId = termId,
            name = name.trim().ifBlank { "新课程表" },
            sortOrder = order,
            periodsPerDay = periods,
        )
        val tableId = timetableDao.insert(table)
        writeTimeSlots(table.copy(id = tableId), periods, slots)
        tableId
    }

    suspend fun deleteTimetable(timetableId: Long) {
        timetableDao.deleteById(timetableId)
    }

    suspend fun importTimetableFromText(
        termId: Long,
        sourceTimetableId: Long?,
        text: String,
        applySettings: Boolean = false,
    ): ImportResult {
        val term = termDao.get(termId) ?: error("还没有学期，无法导入")
        val slots = sourceTimetableId
            ?.let { timeSlotDao.getByTimetable(it) }
            .orEmpty()
        val parsed = TimetableFileImporter.parse(text, term, slots)
        return importParsedTimetable(termId, sourceTimetableId, parsed, applySettings)
    }

    suspend fun importParsedTimetable(
        termId: Long,
        sourceTimetableId: Long?,
        parsed: ImportedTimetable,
        applySettings: Boolean = false,
    ): ImportResult {
        require(parsed.courses.isNotEmpty()) { "没有可导入的课程" }
        return db.withTransaction {
            if (applySettings) {
                applyImportedTermSettings(termId, parsed)
            }
            val source = sourceTimetableId?.let { timetableDao.get(it) }
            val existing = timetableDao.getAll()
            val existingNames = existing.map { it.name }.toSet()
            val baseName = parsed.name.trim().ifBlank { "导入的课表" }
            val name = if (baseName in existingNames) "$baseName（导入）" else baseName

            val importedSlots = parsed.timeSlots
                ?.takeIf { applySettings && it.isNotEmpty() }
            val sourceSlots = source?.let { timeSlotDao.getByTimetable(it.id) }
                .orEmpty()
                .map { it.startMinute to it.endMinute }
            val slots = importedSlots
                ?.map { it.startMinute to it.endMinute }
                ?: sourceSlots.ifEmpty {
                    defaultTimeSlots(0, 1..12).map { it.startMinute to it.endMinute }
                }
            val maxImportedPeriod = importedSlots?.maxOfOrNull { it.period } ?: 0
            val periods = maxOf(
                maxImportedPeriod,
                parsed.term?.periodsPerDay ?: 0,
                source?.periodsPerDay ?: 0,
                slots.size,
            ).coerceIn(1, 30)

            val table = TimetableEntity(
                termId = termId,
                name = name,
                sortOrder = existing.size,
                periodsPerDay = periods,
            )
            val tableId = timetableDao.insert(table)
            writeTimeSlots(table.copy(id = tableId), periods, slots)
            insertImportedCourses(tableId, parsed)
            ImportResult(
                timetableId = tableId,
                courseCount = parsed.courses.size,
                timetableName = name,
            )
        }
    }

    /**
     * 覆盖导入：清空目标课表的课程后写入新课程，保留原有上课时间配置与课表名称。
     */
    suspend fun importParsedTimetableInto(
        timetableId: Long,
        parsed: ImportedTimetable,
    ): ImportResult {
        require(parsed.courses.isNotEmpty()) { "没有可导入的课程" }
        return db.withTransaction {
            val table = timetableDao.get(timetableId) ?: error("目标课表不存在")
            courseDao.deleteCoursesByTimetable(timetableId = table.id)
            insertImportedCourses(table.id, parsed)
            ImportResult(
                timetableId = table.id,
                courseCount = parsed.courses.size,
                timetableName = table.name,
            )
        }
    }

    /**
     * 把 JSON 里携带的学期设置写回当前学期。
     * 节次时间不再随学期共享，而是写到新导入的课表上。
     */
    private suspend fun applyImportedTermSettings(termId: Long, parsed: ImportedTimetable) {
        val term = termDao.get(termId) ?: return
        val imported = parsed.term
        val startDate = (imported?.startDateEpochDay ?: term.startDateEpochDay)
            .coerceIn(MIN_EPOCH_DAY, MAX_EPOCH_DAY)
        termDao.update(
            term.copy(
                startDateEpochDay = startDate,
                totalWeeks = (imported?.totalWeeks ?: term.totalWeeks).coerceIn(1, 60),
                showSaturday = imported?.showSaturday ?: term.showSaturday,
                showSunday = imported?.showSunday ?: term.showSunday,
            ),
        )
    }

    private suspend fun insertImportedCourses(tableId: Long, parsed: ImportedTimetable) {
        parsed.courses.forEach { course ->
            val courseId = courseDao.insertCourse(
                CourseEntity(
                    timetableId = tableId,
                    name = course.name,
                    teacher = course.teacher,
                    colorIndex = course.colorIndex,
                ),
            )
            course.sessions.forEach { session ->
                courseDao.insertSession(
                    CourseSessionEntity(
                        courseId = courseId,
                        dayOfWeek = session.dayOfWeek,
                        startPeriod = session.startPeriod,
                        periodCount = session.periodCount,
                        startWeek = session.startWeek,
                        endWeek = session.endWeek,
                        parity = session.parity,
                        room = session.room,
                    ),
                )
            }
        }
    }

    suspend fun renameTimetable(timetableId: Long, name: String) {
        val table = timetableDao.get(timetableId) ?: return
        timetableDao.update(table.copy(name = name.trim().ifBlank { table.name }))
    }

    private suspend fun insertSampleCourses(tableId: Long) {
        val samples = listOf(
            Sample("高等数学", "张老师", "教三-201", 1, 1, 2, 1, 16, 0, 0),
            Sample("大学英语", "李老师", "外语楼-305", 2, 3, 2, 1, 16, 1, 0),
            Sample("数据结构", "王老师", "计算机楼-401", 3, 1, 2, 1, 16, 2, 0),
            Sample("体育", "赵老师", "东操场", 4, 5, 2, 1, 16, 3, 0),
            Sample("操作系统", "刘老师", "计算机楼-302", 5, 3, 2, 1, 12, 4, 0),
            Sample("程序设计实验", "王老师", "实验楼-2", 1, 5, 2, 9, 16, 5, 0),
        )
        samples.forEach { sample ->
            val courseId = courseDao.insertCourse(
                CourseEntity(
                    timetableId = tableId,
                    name = sample.name,
                    teacher = sample.teacher,
                    colorIndex = sample.color,
                ),
            )
            courseDao.insertSession(
                CourseSessionEntity(
                    courseId = courseId,
                    dayOfWeek = sample.day,
                    startPeriod = sample.start,
                    periodCount = sample.count,
                    startWeek = sample.startWeek,
                    endWeek = sample.endWeek,
                    parity = sample.parity,
                    room = sample.room,
                ),
            )
        }
    }

    private data class Sample(
        val name: String,
        val teacher: String,
        val room: String,
        val day: Int,
        val start: Int,
        val count: Int,
        val startWeek: Int,
        val endWeek: Int,
        val color: Int,
        val parity: Int,
    )

    private companion object {
        // 2000-01-01 .. 2100-12-31，避免损坏文件写入荒唐日期
        const val MIN_EPOCH_DAY = 10957L
        const val MAX_EPOCH_DAY = 47847L
    }

    private fun defaultTimeSlots(timetableId: Long, periods: IntRange): List<TimeSlotEntity> {
        val times = listOf(
            480 to 525,
            535 to 580,
            600 to 645,
            655 to 700,
            840 to 885,
            895 to 940,
            960 to 1005,
            1015 to 1060,
            1140 to 1185,
            1195 to 1240,
            1250 to 1295,
            1305 to 1350,
        )
        return periods.map { period ->
            val (start, end) = times.getOrElse(period - 1) { 480 to 525 }
            TimeSlotEntity(
                timetableId = timetableId,
                period = period,
                startMinute = start,
                endMinute = end,
            )
        }
    }
}
