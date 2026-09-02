package org.sprachcafe.team.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.sprachcafe.team.MainActivity

class ShiftAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val endTime = intent.getStringExtra("endTime") ?: "bald"

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "CASH_COUNT")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ShiftReminderManager.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ Schichtende steht bevor ($endTime Uhr)")
            .setContentText("Bitte denke daran, rechtzeitig den Kassensturz durchzuführen.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Deine Schicht endet um $endTime Uhr. Bitte führe jetzt den Kassensturz durch, um die Café-Kasse ordnungsgemäß abzuschließen.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(4242, notification)
    }
}
