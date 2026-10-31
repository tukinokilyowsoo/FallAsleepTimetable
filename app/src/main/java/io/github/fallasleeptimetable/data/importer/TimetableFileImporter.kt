package io.github.fallasleeptimetable.data.importer

import io.github.fallasleeptimetable.data.TermEntity
import io.github.fallasleeptimetable.data.TimeSlotEntity

/** 按内容自动识别课表文件格式：ICS / CSV / FallAsleep JSON / 旧版 .wakeup_schedule。 */
object TimetableFileImporter {
    fun parse(
        text: String,
        term: TermEntity,
        timeSlots: List<TimeSlotEntity>,
    ): ImportedTimetable {
        val content = text.removePrefix("\uFEFF").trim()
        require(content.isNotEmpty()) { "文件是空的" }

        return when {
            content.startsWith("BEGIN:VCALENDAR", ignoreCase = true) ->
                IcsImporter.parse(content, term, timeSlots)

            CsvImporter.looksLikeCsv(content) -> CsvImporter.parse(content)

            else -> TimetableImporter.parse(content)
        }
    }
}
