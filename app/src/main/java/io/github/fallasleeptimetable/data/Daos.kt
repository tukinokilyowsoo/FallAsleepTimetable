package io.github.fallasleeptimetable.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CourseWithSessions(
    @Embedded val course: CourseEntity,
    @Relation(parentColumn = "id", entityColumn = "courseId")
    val sessions: List<CourseSessionEntity>,
)

@Dao
interface TermDao {
    @Query("SELECT * FROM term ORDER BY id LIMIT 1")
    fun observeFirst(): Flow<TermEntity?>

    @Query("SELECT * FROM term WHERE id = :id")
    fun observe(id: Long): Flow<TermEntity?>

    @Query("SELECT * FROM term WHERE id = :id")
    suspend fun get(id: Long): TermEntity?

    @Query("SELECT COUNT(*) FROM term")
    suspend fun count(): Int

    @Insert
    suspend fun insert(term: TermEntity): Long

    @Update
    suspend fun update(term: TermEntity)
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<TimetableEntity>>

    @Query("SELECT * FROM timetable ORDER BY sortOrder, id")
    suspend fun getAll(): List<TimetableEntity>

    @Query("SELECT * FROM timetable WHERE id = :id")
    suspend fun get(id: Long): TimetableEntity?

    @Insert
    suspend fun insert(timetable: TimetableEntity): Long

    @Update
    suspend fun update(timetable: TimetableEntity)

    @Delete
    suspend fun delete(timetable: TimetableEntity)

    @Query("DELETE FROM timetable WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CourseDao {
    @Transaction
    @Query("SELECT * FROM course WHERE timetableId = :timetableId ORDER BY name")
    fun observeWithSessions(timetableId: Long): Flow<List<CourseWithSessions>>

    @Query("SELECT * FROM course WHERE id = :id")
    suspend fun getCourse(id: Long): CourseEntity?

    @Query("SELECT * FROM course_session WHERE id = :id")
    suspend fun getSession(id: Long): CourseSessionEntity?

    @Insert
    suspend fun insertCourse(course: CourseEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Query("DELETE FROM course WHERE id = :id")
    suspend fun deleteCourse(id: Long)

    @Insert
    suspend fun insertSession(session: CourseSessionEntity): Long

    @Update
    suspend fun updateSession(session: CourseSessionEntity)
}

@Dao
interface TimeSlotDao {
    @Query("SELECT * FROM time_slot WHERE termId = :termId ORDER BY period")
    fun observeByTerm(termId: Long): Flow<List<TimeSlotEntity>>

    @Query("SELECT * FROM time_slot WHERE termId = :termId ORDER BY period")
    suspend fun getByTerm(termId: Long): List<TimeSlotEntity>

    @Query("SELECT COUNT(*) FROM time_slot WHERE termId = :termId")
    suspend fun countByTerm(termId: Long): Int

    @Query("DELETE FROM time_slot WHERE termId = :termId")
    suspend fun deleteByTerm(termId: Long)

    @Insert
    suspend fun insertAll(slots: List<TimeSlotEntity>)
}
