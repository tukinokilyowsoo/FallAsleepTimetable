package io.github.fallasleeptimetable.data.importer

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

enum class EduTemplate(val label: String) {
    FZ("正方教务（旧版）"),
    NEW_FZ("正方教务（新版）"),
    QZ("强智教务"),
}

data class EduParseResult(
    val template: EduTemplate,
    val timetable: ImportedTimetable,
)

/**
 * 本地解析教务系统课表页面。
 *
 * 只处理三种最常见的模板：
 * - 新版正方 jwglxt（#table1 + .festival/.title）
 * - 旧版正方（#Table1 + “第x节”文本）
 * - 强智（#kbtable + .kbcontent）
 */
object EduScheduleParser {
    fun parse(html: String, timetableName: String = "教务导入"): EduParseResult {
        require(html.isNotBlank()) { "页面内容为空" }
        val document = Jsoup.parse(html, "UTF-8")
        val template = detect(document)
            ?: error("没有识别到课表结构，请确认已经打开个人课表页面")

        val courses = when (template) {
            EduTemplate.NEW_FZ -> parseNewFz(document)
            EduTemplate.FZ -> parseFz(document)
            EduTemplate.QZ -> parseQz(document)
        }
        val merged = mergeCourses(courses)
        require(merged.isNotEmpty()) {
            "识别到${template.label}页面，但没有解析出课程，可能页面结构有改动"
        }
        return EduParseResult(
            template = template,
            timetable = ImportedTimetable(name = timetableName, courses = merged),
        )
    }

    fun detectTemplate(html: String): EduTemplate? =
        runCatching { detect(Jsoup.parse(html, "UTF-8")) }.getOrNull()

    private fun detect(document: Document): EduTemplate? {
        if (document.getElementById("kbtable") != null ||
            document.getElementsByClass("kbcontent").isNotEmpty()
        ) {
            return EduTemplate.QZ
        }
        if (document.getElementById("table1") != null &&
            (document.getElementsByClass("festival").isNotEmpty() ||
                document.getElementsByClass("title").isNotEmpty())
        ) {
            return EduTemplate.NEW_FZ
        }
        if (document.getElementById("Table1") != null) {
            return EduTemplate.FZ
        }
        return null
    }

    // ---------------------------------------------------------------- 新版正方

    private fun parseNewFz(document: Document): List<ImportedCourse> {
        val table = document.getElementById("table1") ?: return emptyList()
        val courses = mutableListOf<ImportedCourse>()

        for (tr in table.getElementsByTag("tr")) {
            val node = parseFestival(tr)
            var dayCounter = 0

            for (td in tr.getElementsByTag("td")) {
                if (td.hasClass("festival")) continue
                dayCounter++
                val day = parseDay(td, dayCounter)

                val courseDivs = td.getElementsByTag("div").filter { div ->
                    div.hasClass("title") || div.getElementsByClass("title").isNotEmpty()
                }

                for (div in courseDivs) {
                    val titleElement = div.getElementsByClass("title").firstOrNull()
                        ?: div.takeIf { it.hasClass("title") }
                        ?: continue
                    val name = titleElement.text().trim()
                    if (name.isEmpty()) continue

                    val teacher = div.getElementsByAttributeValueStarting("title", "教师")
                        .mapNotNull { fieldValue(it) }
                        .firstOrNull()
                    val room = div.getElementsByAttributeValueStarting("title", "上课地点")
                        .mapNotNull { fieldValue(it) }
                        .firstOrNull()

                    val sessions = mutableListOf<ImportedSession>()
                    for (timeElement in div.getElementsByAttributeValueStarting("title", "节/周")) {
                        val timeText = fieldValue(timeElement).orEmpty()
                        if (timeText.isEmpty()) continue

                        val spec = parseNewFzTime(timeText, node)
                        spec.weeks.forEach { (startWeek, endWeek, parity) ->
                            sessions.add(
                                ImportedSession(
                                    dayOfWeek = day.coerceIn(1, 7),
                                    startPeriod = spec.start,
                                    periodCount = (spec.end - spec.start + 1).coerceAtLeast(1),
                                    startWeek = startWeek,
                                    endWeek = endWeek,
                                    parity = parity,
                                    room = room,
                                ),
                            )
                        }
                    }

                    if (sessions.isNotEmpty()) {
                        courses.add(
                            ImportedCourse(
                                name = name,
                                teacher = teacher,
                                colorIndex = colorFor(name),
                                sessions = sessions,
                            ),
                        )
                    }
                }
            }
        }
        return courses
    }

    private fun parseFestival(tr: Element): Int {
        val text = tr.getElementsByClass("festival").text().trim()
        return Regex("""\d+""").find(text)?.value?.toIntOrNull()
            ?: chineseNumber(text)
            ?: 0
    }

    private fun parseDay(td: Element, dayCounter: Int): Int {
        val fromId = Regex("""(\d+)""")
            .find(td.attr("id"))
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
        return (fromId ?: dayCounter).coerceIn(1, 7)
    }

    /**
     * 正方页面常把字段值放在带 title 的 span 后面的 font 里，
     * 例如 `span[title="教师 "]` + `font 张三`；span 内部只有图标。
     */
    private fun fieldValue(element: Element): String? {
        val ownText = element.text().trim()
        if (ownText.isNotBlank()) return ownText

        var sibling = element.nextElementSibling()
        while (sibling != null) {
            if (sibling.tagName() == "font" || sibling.tagName() == "span") {
                val text = sibling.text().trim()
                if (text.isNotBlank()) return text
            }
            sibling = sibling.nextElementSibling()
        }
        val parentText = element.parent()?.text()?.trim().orEmpty()
        if (parentText.isNotBlank()) return parentText
        return element.text().trim().takeIf { it.isNotBlank() }
    }

    private fun extractWeekSource(text: String): String {
        val afterLastParen = text.substringAfterLast(')', "").trim()
        if (afterLastParen.isNotBlank() && afterLastParen.contains('周')) {
            return afterLastParen
        }
        val inParens = Regex("""\(([^)]*)\)""")
            .findAll(text)
            .map { it.groupValues[1] }
            .firstOrNull { it.contains('周') && !it.contains("节") }
        if (!inParens.isNullOrBlank()) return inParens
        return text.substringAfterLast('节', "").trim()
    }

    private data class NewFzTime(
        val start: Int,
        val end: Int,
        val weeks: List<Triple<Int, Int, Int>>,
    )

    private fun parseNewFzTime(text: String, fallbackNode: Int): NewFzTime {
        val range = Regex("""(\d+)\s*-\s*(\d+)\s*节""").find(text)
        val single = Regex("""(\d+)\s*节""").find(text)

        val start = range?.groupValues?.get(1)?.toIntOrNull()
            ?: single?.groupValues?.get(1)?.toIntOrNull()
            ?: fallbackNode
        val end = range?.groupValues?.get(2)?.toIntOrNull() ?: start

        val weeks = parseWeeks(extractWeekSource(text)).ifEmpty { listOf(Triple(1, 20, 0)) }
        return NewFzTime(start.coerceAtLeast(1), end.coerceAtLeast(start), weeks)
    }

    // ---------------------------------------------------------------- 旧版正方

    private val fzCourseProps = setOf(
        "必修课", "选修课", "必修", "选修", "专基", "专选", "公必", "公选",
        "义修", "选", "必", "主干", "专限", "公基", "值班", "通选",
        "思政必", "思政选", "自基必", "自基选", "语技必", "语技选",
        "体育必", "体育选", "专业基础课", "双创必", "双创选", "新生必",
        "新生选", "学科必修", "学科选修", "通识必修", "通识选修",
        "公共基础", "第二课堂", "学科实践", "专业实践", "专业必修",
        "辅修", "专业选修", "外语", "方向",
    )

    private fun parseFz(document: Document): List<ImportedCourse> {
        val table = document.getElementById("Table1") ?: return emptyList()
        val courses = mutableListOf<ImportedCourse>()

        for (tr in table.getElementsByTag("tr")) {
            var node = 0
            for (td in tr.getElementsByTag("td")) {
                val raw = td.text().trim().replace(' ', ' ')
                if (raw.length <= 1) continue

                val nodeMatch = Regex("""^第\s*([一二三四五六七八九十\d]+)\s*节$""").find(raw)
                if (nodeMatch != null) {
                    node = chineseNumber(nodeMatch.groupValues[1]) ?: node
                    continue
                }
                if (!raw.contains('周') || !raw.contains('节')) continue

                parseFzCell(raw, node, courses)
            }
        }
        return courses
    }

    private fun parseFzCell(raw: String, node: Int, out: MutableList<ImportedCourse>) {
        val tokens = raw.split(Regex("""\s+""")).filter { it.isNotBlank() }

        tokens.forEachIndexed { index, token ->
            if (!token.contains('{') || !token.contains('}')) return@forEachIndexed

            var nameIndex = index - 1
            while (nameIndex >= 0 && tokens[nameIndex] in fzCourseProps) {
                nameIndex--
            }
            val name = tokens.getOrNull(nameIndex)?.trim().orEmpty()
            if (name.isEmpty()) return@forEachIndexed

            val day = parseChineseDay(token) ?: return@forEachIndexed
            val teacher = tokens.getOrNull(index + 1)?.trim()?.takeIf { it.isNotBlank() }
            val room = tokens.getOrNull(index + 2)?.trim()?.takeIf { it.isNotBlank() }

            val spec = parseFzTime(token, node)
            val sessions = spec.weeks.map { (startWeek, endWeek, parity) ->
                ImportedSession(
                    dayOfWeek = day,
                    startPeriod = spec.start,
                    periodCount = spec.count,
                    startWeek = startWeek,
                    endWeek = endWeek,
                    parity = parity,
                    room = room,
                )
            }
            out.add(
                ImportedCourse(
                    name = name,
                    teacher = teacher,
                    colorIndex = colorFor(name),
                    sessions = sessions,
                ),
            )
        }
    }

    private data class FzTime(
        val start: Int,
        val count: Int,
        val weeks: List<Triple<Int, Int, Int>>,
    )

    private fun parseFzTime(text: String, fallbackNode: Int): FzTime {
        val section = text.substringBefore('{')
        val numbers = Regex("""\d+""")
            .findAll(section)
            .mapNotNull { it.value.toIntOrNull() }
            .toList()

        val start: Int
        val count: Int
        when {
            numbers.size >= 2 -> {
                start = numbers.first()
                count = (numbers.last() - start + 1).coerceAtLeast(1)
            }

            numbers.size == 1 -> {
                start = numbers.first()
                count = 1
            }

            else -> {
                start = fallbackNode.coerceAtLeast(1)
                count = 1
            }
        }

        val weekSource = text.substringAfter('{', "").substringBefore('}', "")
        val weeks = parseWeeks(weekSource).ifEmpty { listOf(Triple(1, 20, 0)) }
        return FzTime(start, count, weeks)
    }

    private fun parseChineseDay(text: String): Int? {
        val match = Regex("""(?:周|星期)([一二三四五六日天])""").find(text) ?: return null
        return when (match.groupValues[1]) {
            "一" -> 1
            "二" -> 2
            "三" -> 3
            "四" -> 4
            "五" -> 5
            "六" -> 6
            "日", "天" -> 7
            else -> null
        }
    }

    // ---------------------------------------------------------------- 强智

    private fun parseQz(document: Document): List<ImportedCourse> {
        val table = document.getElementById("kbtable") ?: return emptyList()
        val courses = mutableListOf<ImportedCourse>()
        var nodeCount = 0

        for (tr in table.getElementsByTag("tr")) {
            val tds = tr.getElementsByTag("td")
            if (tds.isEmpty()) continue
            nodeCount++

            var day = 0
            for (td in tds) {
                day++
                for (div in td.getElementsByClass("kbcontent")) {
                    if (div.text().isBlank()) continue
                    div.html().split("-----").forEach { segment ->
                        if (segment.isBlank()) return@forEach
                        parseQzSegment(segment, day, nodeCount)?.let(courses::add)
                    }
                }
            }
        }
        return courses
    }

    private fun parseQzSegment(
        segmentHtml: String,
        day: Int,
        nodeCount: Int,
    ): ImportedCourse? {
        val normalizedHtml = segmentHtml.replace(Regex("(?i)<br\\s*/?>"), "<br>")
        val name = Jsoup.parse(normalizedHtml.substringBefore("<br>", normalizedHtml))
            .text()
            .replace(' ', ' ')
            .trim()
        if (name.isEmpty()) return null

        val fragment = Jsoup.parse(segmentHtml)
        fun field(title: String): String? =
            fragment.getElementsByAttributeValue("title", title)
                .firstOrNull()
                ?.text()
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        val teacher = field("老师")
        val room = field("教室")
        val weekText = field("周次(节次)")?.substringBefore("(周)").orEmpty()
        val weeks = parseWeeks(weekText).ifEmpty { listOf(Triple(1, 20, 0)) }

        val node = if (nodeCount <= 3) nodeCount * 2 - 1 else nodeCount * 2 - 2
        val start = node.coerceAtLeast(1)
        val count = if (start == 5 || start == 12) 1 else 2

        val sessions = weeks.map { (startWeek, endWeek, parity) ->
            ImportedSession(
                dayOfWeek = day.coerceIn(1, 7),
                startPeriod = start,
                periodCount = count,
                startWeek = startWeek,
                endWeek = endWeek,
                parity = parity,
                room = room,
            )
        }
        return ImportedCourse(
            name = name,
            teacher = teacher,
            colorIndex = colorFor(name),
            sessions = sessions,
        )
    }

    // ---------------------------------------------------------------- 公共

    private fun parseWeeks(text: String): List<Triple<Int, Int, Int>> {
        if (text.isBlank()) return emptyList()
        val results = mutableListOf<Triple<Int, Int, Int>>()

        text.split(',', '，', ';', '；').forEach { raw ->
            val token = raw.trim()
            if (token.isEmpty()) return@forEach
            val parity = when {
                token.contains('单') -> 1
                token.contains('双') -> 2
                else -> 0
            }
            val range = Regex("""(\d+)\s*-\s*(\d+)""").find(token)
            if (range != null) {
                val start = range.groupValues[1].toIntOrNull() ?: return@forEach
                val end = range.groupValues[2].toIntOrNull() ?: start
                results.add(Triple(start, end, parity))
            } else {
                val single = Regex("""(\d+)""").find(token)?.groupValues?.get(1)?.toIntOrNull()
                if (single != null) {
                    results.add(Triple(single, single, parity))
                } else if (parity != 0) {
                    results.add(Triple(1, 20, parity))
                }
            }
        }
        return results
    }

    private fun chineseNumber(input: String): Int? {
        input.toIntOrNull()?.let { return it }
        val digits = mapOf(
            '零' to 0, '一' to 1, '二' to 2, '三' to 3, '四' to 4,
            '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9,
        )
        if (input == "十") return 10
        if (input.length == 2 && input[0] == '十') {
            return 10 + (digits[input[1]] ?: return null)
        }
        if (input.length == 2 && input[1] == '十') {
            return (digits[input[0]] ?: return null) * 10
        }
        if (input.length == 1) {
            return digits[input[0]]
        }
        return null
    }

    private fun mergeCourses(courses: List<ImportedCourse>): List<ImportedCourse> {
        val merged = LinkedHashMap<String, ImportedCourse>()
        courses.forEach { course ->
            val existing = merged[course.name]
            if (existing == null) {
                merged[course.name] = course.copy(
                    sessions = course.sessions.distinctBy { it.key() }
                        .sortedWith(compareBy({ it.dayOfWeek }, { it.startPeriod })),
                )
            } else {
                val sessions = (existing.sessions + course.sessions)
                    .distinctBy { it.key() }
                    .sortedWith(
                        compareBy({ it.dayOfWeek }, { it.startPeriod }, { it.startWeek }),
                    )
                merged[course.name] = existing.copy(
                    teacher = existing.teacher ?: course.teacher,
                    sessions = sessions,
                )
            }
        }
        return merged.values.toList()
    }

    private fun ImportedSession.key(): List<Any> = listOf(
        dayOfWeek,
        startPeriod,
        periodCount,
        startWeek,
        endWeek,
        parity,
        room ?: "",
    )

    private fun colorFor(name: String): Int = (name.hashCode() and 0x7fffffff) % 6
}
