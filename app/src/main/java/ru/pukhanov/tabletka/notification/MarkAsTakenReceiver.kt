package ru.pukhanov.tabletka.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.pukhanov.tabletka.TabletkaApplication
import ru.pukhanov.tabletka.data.model.MedicationTake
import java.time.LocalDate

class MarkAsTakenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val hour = intent.getIntExtra("EXTRA_HOUR", -1)
        val minute = intent.getIntExtra("EXTRA_MINUTE", -1)
        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", -1)

        if (hour == -1 || minute == -1 || notificationId == -1) {
            Log.e("MarkAsTakenReceiver", "Missing extras: hour=$hour, minute=$minute, notificationId=$notificationId")
            pendingResult.finish()
            return
        }

        val app = context.applicationContext as TabletkaApplication
        val repository = app.repository

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Insert the take record
                val todayDate = LocalDate.now().toString()
                repository.insertTake(MedicationTake(date = todayDate, hour = hour, minute = minute))

                // 2. Dismiss the notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
            } catch (e: Exception) {
                Log.e("MarkAsTakenReceiver", "Error marking medication as taken", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
