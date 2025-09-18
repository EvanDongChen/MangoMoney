package com.example.financetest

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat

const val REMINDER_CHANNEL_ID = "finance_reminders"
const val REMINDER_CHANNEL_NAME = "Finance Reminders"

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            REMINDER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for scheduled finance notifications"
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}

fun scheduleReminderNotification(context: Context, reminderId: Long, title: String, amount: Double?, triggerAtMillis: Long) {
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("reminder_id", reminderId)
        putExtra("reminder_title", title)
        amount?.let { putExtra("reminder_amount", it) }
    }
    val pending = PendingIntent.getBroadcast(
        context,
        reminderId.hashCode(),
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    )
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    try {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
    } catch (se: SecurityException) {
        // Some devices / API levels require the SCHEDULE_EXACT_ALARM permission for exact alarms.
        // Fall back to a non-exact alarm to avoid crashing; notify via log and (optionally) a toast.
        Log.w("NotificationUtils", "Exact alarm scheduling denied; falling back to inexact alarm", se)
        try {
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        } catch (e: Exception) {
            Log.e("NotificationUtils", "Failed to schedule fallback alarm", e)
            Toast.makeText(context, "Unable to schedule reminder alarm: ${e.message}", Toast.LENGTH_LONG).show()
            throw e
        }
    }
}

fun cancelReminderNotification(context: Context, reminderId: Long) {
    val intent = Intent(context, ReminderReceiver::class.java)
    val pending = PendingIntent.getBroadcast(
        context,
        reminderId.hashCode(),
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    )
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    am.cancel(pending)
}

fun buildNotification(context: Context, reminderId: Long, title: String, amount: Double?): androidx.core.app.NotificationCompat.Builder {
    val nf = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
    amount?.let {
        nf.setContentText("Amount: ${formatCurrency(-kotlin.math.abs(it))}")
    }
    return nf
}
