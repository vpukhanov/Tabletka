package ru.pukhanov.tabletka.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.model.MedicationSchedule
import ru.pukhanov.tabletka.data.model.MedicationTake
import ru.pukhanov.tabletka.data.local.dao.MedicationDao

@Database(entities = [Medication::class, MedicationSchedule::class, MedicationTake::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN dosage TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `medication_schedules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `medicationId` INTEGER NOT NULL, 
                        `hour` INTEGER NOT NULL, 
                        `minute` INTEGER NOT NULL, 
                        `daysOfWeek` TEXT NOT NULL, 
                        FOREIGN KEY(`medicationId`) REFERENCES `medications`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_medication_schedules_medicationId` ON `medication_schedules` (`medicationId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medication_schedules ADD COLUMN doses REAL NOT NULL DEFAULT 1.0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `medication_takes` (
                        `date` TEXT NOT NULL, 
                        `hour` INTEGER NOT NULL, 
                        `minute` INTEGER NOT NULL, 
                        PRIMARY KEY(`date`, `hour`, `minute`)
                    )
                """.trimIndent())
            }
        }
    }
}
