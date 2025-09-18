package com.example.financetest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val id = intent.getLongExtra("reminder_id", System.currentTimeMillis())
            val title = intent.getStringExtra("reminder_title") ?: "Reminder"
            val amount = if (intent.hasExtra("reminder_amount")) intent.getDoubleExtra("reminder_amount", 0.0) else null

            Log.i("ReminderReceiver", "Received reminder broadcast: id=$id title=$title amount=$amount")
            Toast.makeText(context, "Reminder: $title", Toast.LENGTH_SHORT).show()

            val notif = buildNotification(context, id, title, amount).build()
            // On Android 13+ the app needs POST_NOTIFICATIONS runtime permission; double-check before notifying.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    Log.w("ReminderReceiver", "POST_NOTIFICATIONS permission not granted; cannot show notification")
                    Toast.makeText(context, "Notification permission not granted", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            NotificationManagerCompat.from(context).notify(id.hashCode(), notif)
            Log.i("ReminderReceiver", "Notification posted for reminder id=$id")
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Error handling reminder broadcast", e)
        }
    }
}
