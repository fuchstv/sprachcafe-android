package org.sprachcafe.team.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ShiftReminderManager {

    const val CHANNEL_ID = "kassensturz_reminders"
    const val CHANNEL_NAME = "Schichtende & Kassensturz"
    private const val ALARM_REQUEST_CODE = 4242

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Erinnert 15 Minuten vor Schichtende an den Kassensturz"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder15MinBefore(context: Context, endTimeStr: String) {
        createNotificationChannel(context)

        val parts = endTimeStr.split(":")
        if (parts.size < 2) return

        val hour = parts[0].trim().toIntOrNull() ?: return
        val minute = parts[1].trim().toIntOrNull() ?: return

        val shiftEndCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 15 minutes before shift end
        val reminderCal = Calendar.getInstance().apply {
            timeInMillis = shiftEndCal.timeInMillis - (15 * 60 * 1000)
        }

        // Only schedule if reminder is in the future
        if (reminderCal.timeInMillis > System.currentTimeMillis()) {
            val intent = Intent(context, ShiftAlarmReceiver::class.java).apply {
                putExtra("endTime", endTimeStr)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderCal.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderCal.timeInMillis, pendingIntent)
                }
            } catch (e: SecurityException) {
                // Fallback if SCHEDULE_EXACT_ALARM is restricted
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderCal.timeInMillis, pendingIntent)
            }
        }
    }

    fun cancelReminder(context: Context) {
        val intent = Intent(context, ShiftAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
