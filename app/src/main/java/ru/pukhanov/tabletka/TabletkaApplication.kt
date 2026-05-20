package ru.pukhanov.tabletka

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.pukhanov.tabletka.data.repository.MedicationRepository
import ru.pukhanov.tabletka.notification.NotificationScheduler
import javax.inject.Inject

@HiltAndroidApp
class TabletkaApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var repository: MedicationRepository

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
