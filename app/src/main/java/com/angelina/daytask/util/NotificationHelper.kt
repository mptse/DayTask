package com.angelina.daytask.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.angelina.daytask.data.model.Task
import com.angelina.daytask.receiver.TaskNotificationReceiver
import java.text.SimpleDateFormat
import java.util.*

object NotificationHelper {

    fun scheduleTaskNotification(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Check if we can schedule exact alarms on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // If not, we could redirect to settings or use setWindow
                // For now, we'll try to use setExactAndAllowWhileIdle and let system handle it
            }
        }

        val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
            putExtra("TASK_NAME", task.name)
            putExtra("TASK_EMOJI", task.emoji)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = parseDateTime(task.day, task.time)

        // Only schedule if the time is in the future
        if (triggerAtMillis > System.currentTimeMillis()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun parseDateTime(day: String, time: String): Long {
        return try {
            val format = SimpleDateFormat("dd MMM, yyyy HH:mm", Locale.getDefault())
            val date = format.parse("$day $time")
            date?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun cancelTaskNotification(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
