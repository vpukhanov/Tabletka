package ru.pukhanov.tabletka.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.pukhanov.tabletka.MainActivity
import ru.pukhanov.tabletka.TabletkaApplication
import ru.pukhanov.tabletka.data.model.MedicationWithSchedules
import java.time.DayOfWeek
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val hour = intent.getIntExtra("EXTRA_HOUR", -1)
        val minute = intent.getIntExtra("EXTRA_MINUTE", -1)
        val dayOfWeekValue = intent.getIntExtra("EXTRA_DAY_OF_WEEK", -1)

        if (hour == -1 || minute == -1 || dayOfWeekValue == -1) {
            pendingResult.finish()
            return
        }

        val app = context.applicationContext as TabletkaApplication
        val repository = app.repository

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Reschedule this specific alarm for next week
                val scheduler = NotificationScheduler(context)
                scheduler.rescheduleAlarm(dayOfWeekValue, hour, minute)

                // 2. Check if medications for this hour and minute have already been taken today
                val todayDate = LocalDate.now().toString()
                val takes = repository.getTakesForDate(todayDate).first()
                val isAlreadyTaken = takes.any { it.hour == hour && it.minute == minute }
                if (isAlreadyTaken) {
                    Log.d("AlarmReceiver", "Medications at $hour:$minute already taken today, skipping notification")
                    return@launch
                }

                // 3. Query all medications with schedules to find those scheduled for today and this time
                val dayOfWeek = DayOfWeek.of(dayOfWeekValue)
                val allMeds = repository.allMedicationsWithSchedules.first()

                val scheduledMeds = allMeds.filter { medWithSchedule ->
                    medWithSchedule.schedules.any { schedule ->
                        schedule.hour == hour &&
                        schedule.minute == minute &&
                        schedule.daysOfWeek.contains(dayOfWeek)
                    }
                }

                if (scheduledMeds.isNotEmpty()) {
                    showNotification(context, scheduledMeds, hour, minute, dayOfWeekValue)
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error in AlarmReceiver onReceive", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        scheduledMeds: List<MedicationWithSchedules>,
        hour: Int,
        minute: Int,
        dayOfWeekValue: Int
    ) {
        // Check for POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("AlarmReceiver", "POST_NOTIFICATIONS permission not granted, skipping notification")
                return
            }
        }

        // Generate deterministic notification ID using the formula from getRequestCode
        val notificationId = (dayOfWeekValue * 10000) + (hour * 100) + minute

        // Tapping the notification takes the user to the Today screen (MainActivity)
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markAsTakenIntent = Intent(context, MarkAsTakenReceiver::class.java).apply {
            putExtra("EXTRA_HOUR", hour)
            putExtra("EXTRA_MINUTE", minute)
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
        }
        val markAsTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            markAsTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (scheduledMeds.size == 1) {
            "Medication Reminder"
        } else {
            "Medications Reminder"
        }

        val medNames = scheduledMeds.joinToString(", ") { medWithSchedule ->
            val med = medWithSchedule.medication
            val dosageText = if (!med.dosage.isNullOrBlank()) " (${med.dosage})" else ""
            "${med.title}$dosageText"
        }
        val text = "It's time to take: $medNames"

        val notification = NotificationCompat.Builder(context, "medication_reminders")
            .setSmallIcon(ru.pukhanov.tabletka.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(
                ru.pukhanov.tabletka.R.drawable.ic_notification_checkmark,
                context.getString(ru.pukhanov.tabletka.R.string.mark_as_taken),
                markAsTakenPendingIntent
            )
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
