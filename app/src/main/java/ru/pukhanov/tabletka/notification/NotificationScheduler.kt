package ru.pukhanov.tabletka.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import ru.pukhanov.tabletka.data.model.MedicationWithSchedules
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import androidx.core.content.edit

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val sharedPrefs = context.getSharedPreferences("notification_scheduler_prefs", Context.MODE_PRIVATE)

    data class UniqueSchedule(
        val dayOfWeek: DayOfWeek,
        val hour: Int,
        val minute: Int
    )

    fun scheduleAlarms(medicationsWithSchedules: List<MedicationWithSchedules>) {
        // 1. Cancel previously scheduled alarms to avoid orphans
        cancelAllScheduledAlarms()

        // 2. Identify all unique (dayOfWeek, hour, minute) schedules
        val uniqueSchedules = mutableSetOf<UniqueSchedule>()
        for (medWithSchedule in medicationsWithSchedules) {
            for (schedule in medWithSchedule.schedules) {
                for (day in schedule.daysOfWeek) {
                    uniqueSchedules.add(UniqueSchedule(day, schedule.hour, schedule.minute))
                }
            }
        }

        // 3. Schedule each unique alarm
        val scheduledCodes = mutableSetOf<String>()
        val now = LocalDateTime.now()

        for (schedule in uniqueSchedules) {
            val triggerTime = getNextTriggerTime(schedule, now)
            val triggerMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val requestCode = getRequestCode(schedule)
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("EXTRA_HOUR", schedule.hour)
                putExtra("EXTRA_MINUTE", schedule.minute)
                putExtra("EXTRA_DAY_OF_WEEK", schedule.dayOfWeek.value)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            try {
                if (canScheduleExact) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
                scheduledCodes.add(requestCode.toString())
            } catch (e: SecurityException) {
                Log.e("NotificationScheduler", "SecurityException scheduling exact alarm", e)
                // Fallback to inexact alarm
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
                scheduledCodes.add(requestCode.toString())
            }
        }

        // 4. Save the scheduled request codes
        sharedPrefs.edit { putStringSet("scheduled_request_codes", scheduledCodes) }
    }

    private fun cancelAllScheduledAlarms() {
        val scheduledCodes = sharedPrefs.getStringSet("scheduled_request_codes", emptySet()) ?: emptySet()
        for (codeStr in scheduledCodes) {
            val requestCode = codeStr.toIntOrNull() ?: continue
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        sharedPrefs.edit { remove("scheduled_request_codes") }
    }

    fun rescheduleAlarm(dayOfWeekValue: Int, hour: Int, minute: Int) {
        val dayOfWeek = DayOfWeek.of(dayOfWeekValue)
        val schedule = UniqueSchedule(dayOfWeek, hour, minute)
        val now = LocalDateTime.now()
        val triggerTime = getNextTriggerTime(schedule, now)
        val triggerMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val requestCode = getRequestCode(schedule)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_HOUR", hour)
            putExtra("EXTRA_MINUTE", minute)
            putExtra("EXTRA_DAY_OF_WEEK", dayOfWeekValue)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("NotificationScheduler", "SecurityException in rescheduleAlarm", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    companion object {
        fun getRequestCode(schedule: UniqueSchedule): Int {
            return (schedule.dayOfWeek.value * 10000) + (schedule.hour * 100) + schedule.minute
        }

        fun getNextTriggerTime(schedule: UniqueSchedule, now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
            var triggerTime = now.withHour(schedule.hour).withMinute(schedule.minute).withSecond(0).withNano(0)
            if (!triggerTime.isAfter(now)) {
                triggerTime = triggerTime.plusDays(1)
            }
            while (triggerTime.dayOfWeek != schedule.dayOfWeek) {
                triggerTime = triggerTime.plusDays(1)
            }
            return triggerTime
        }
    }
}
