package com.angelina.daytask.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.angelina.daytask.MainActivity
import com.angelina.daytask.R

import com.angelina.daytask.data.AppDatabase
import com.angelina.daytask.ui.viewmodel.toModel
import com.angelina.daytask.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            reScheduleAlarms(context)
            return
        }

        val taskName = intent.getStringExtra("TASK_NAME") ?: "Tarea pendiente"
        val taskEmoji = intent.getStringExtra("TASK_EMOJI") ?: "🎯"

        showNotification(context, taskName, taskEmoji)
    }

    private fun reScheduleAlarms(context: Context) {
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val tasks = db.taskDao().getAllTasks().first()
            tasks.forEach { entity ->
                if (!entity.completed) {
                    NotificationHelper.scheduleTaskNotification(context, entity.toModel())
                }
            }
        }
    }

    private fun showNotification(context: Context, title: String, emoji: String) {
        val channelId = "task_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Tareas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para recordarte tus actividades"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setContentTitle("¡Es hora de tu aventura! $emoji")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
