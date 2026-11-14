package io.github.fallasleeptimetable.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TermEntity::class,
        TimetableEntity::class,
        CourseEntity::class,
        CourseSessionEntity::class,
        TimeSlotEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun termDao(): TermDao
    abstract fun timetableDao(): TimetableDao
    abstract fun courseDao(): CourseDao
    abstract fun timeSlotDao(): TimeSlotDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * v1 的节次时间按学期保存；v2 起改由每张课表各自保存。
         * 迁移时把学期时间复制给该学期下的每一张课表，并把每天节数一起带过去。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `timetable` ADD COLUMN `periodsPerDay` " +
                        "INTEGER NOT NULL DEFAULT 12",
                )
                db.execSQL(
                    "UPDATE `timetable` SET `periodsPerDay` = " +
                        "(SELECT `periodsPerDay` FROM `term` " +
                        "WHERE `term`.`id` = `timetable`.`termId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `time_slot_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timetableId` INTEGER NOT NULL, " +
                        "`period` INTEGER NOT NULL, " +
                        "`startMinute` INTEGER NOT NULL, " +
                        "`endMinute` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`timetableId`) REFERENCES `timetable`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL(
                    "INSERT INTO `time_slot_new` " +
                        "(`timetableId`, `period`, `startMinute`, `endMinute`) " +
                        "SELECT `timetable`.`id`, `time_slot`.`period`, " +
                        "`time_slot`.`startMinute`, `time_slot`.`endMinute` " +
                        "FROM `time_slot` " +
                        "JOIN `timetable` ON `timetable`.`termId` = `time_slot`.`termId`",
                )
                db.execSQL("DROP TABLE `time_slot`")
                db.execSQL("ALTER TABLE `time_slot_new` RENAME TO `time_slot`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_time_slot_timetableId` " +
                        "ON `time_slot` (`timetableId`)",
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fallasleep.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
