package ru.pukhanov.tabletka

import android.app.Application
import androidx.room.Room
import ru.pukhanov.tabletka.data.local.database.AppDatabase
import ru.pukhanov.tabletka.data.repository.MedicationRepository

class TabletkaApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "tabletka_database"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()
    }

    val repository: MedicationRepository by lazy {
        MedicationRepository(database.medicationDao())
    }
}
