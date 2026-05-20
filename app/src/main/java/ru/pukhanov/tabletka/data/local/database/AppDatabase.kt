package ru.pukhanov.tabletka.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.local.dao.MedicationDao

@Database(entities = [Medication::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medications ADD COLUMN dosage TEXT")
            }
        }
    }
}
