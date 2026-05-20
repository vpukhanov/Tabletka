package ru.pukhanov.tabletka.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.local.dao.MedicationDao

@Database(entities = [Medication::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
}
