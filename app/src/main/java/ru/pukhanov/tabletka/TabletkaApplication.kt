package ru.pukhanov.tabletka

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.pukhanov.tabletka.data.local.database.AppDatabase
import ru.pukhanov.tabletka.data.repository.MedicationRepository
import ru.pukhanov.tabletka.notification.NotificationScheduler

class TabletkaApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "tabletka_database"
        )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5
        )
        .build()
    }

    val repository: MedicationRepository by lazy {
        MedicationRepository(database.medicationDao())
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Reactively observe database changes and reschedule alarms accordingly
        applicationScope.launch {
            repository.allMedicationsWithSchedules.collect { medicationsWithSchedules ->
                NotificationScheduler(this@TabletkaApplication).scheduleAlarms(medicationsWithSchedules)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.medication_notifications_channel_name)
            val descriptionText = getString(R.string.medication_notifications_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("medication_reminders", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
