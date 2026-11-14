package io.github.fallasleeptimetable.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "term")
data class TermEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startDateEpochDay: Long,
    val totalWeeks: Int = 20,
    val periodsPerDay: Int = 12,
    val showSaturday: Boolean = true,
    val showSunday: Boolean = false,
)

@Entity(
    tableName = "timetable",
    foreignKeys = [
        ForeignKey(
            entity = TermEntity::class,
            parentColumns = ["id"],
            childColumns = ["termId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("termId")],
)
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val termId: Long,
    val name: String,
    val sortOrder: Int = 0,
    /** 这张课表每天的节数；上课时间配置随课表独立保存。 */
    @ColumnInfo(defaultValue = "12")
    val periodsPerDay: Int = 12,
)

@Entity(
    tableName = "course",
    foreignKeys = [
        ForeignKey(
            entity = TimetableEntity::class,
            parentColumns = ["id"],
            childColumns = ["timetableId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("timetableId")],
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timetableId: Long,
    val name: String,
    val teacher: String? = null,
    val colorIndex: Int = 0,
    val note: String? = null,
)

@Entity(
    tableName = "course_session",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("courseId")],
)
data class CourseSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val periodCount: Int,
    val startWeek: Int,
    val endWeek: Int,
    val parity: Int = 0,
    val room: String? = null,
)

@Entity(
    tableName = "time_slot",
    foreignKeys = [
        ForeignKey(
            entity = TimetableEntity::class,
            parentColumns = ["id"],
            childColumns = ["timetableId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("timetableId")],
)
data class TimeSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timetableId: Long,
    val period: Int,
    val startMinute: Int,
    val endMinute: Int,
)
