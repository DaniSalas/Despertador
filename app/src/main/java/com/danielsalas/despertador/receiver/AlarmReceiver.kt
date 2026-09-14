package com.danielsalas.despertador.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.danielsalas.despertador.R
import com.danielsalas.despertador.data.local.AlarmDatabase
import com.danielsalas.despertador.domain.AlarmScheduler
import com.danielsalas.despertador.service.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS_ALARM = "com.danielsalas.despertador.ACTION_DISMISS_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("AlarmReceiver", "Reinicio detectado, reprogramando alarmas...")
            rescheduleAlarms(context)
            return
        }

        val alarmId = intent.getIntExtra("ALARM_ID", -1)

        if (action == ACTION_DISMISS_ALARM) {
            Log.d("AlarmReceiver", "Acción de descartar alarma recibida para ID: $alarmId")
            if (alarmId != -1) {
                val scheduler = AlarmScheduler(context)
                scheduler.cancel(alarmId)
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(alarmId + 20000)
            }
            return
        }

        Log.d("AlarmReceiver", "¡Evento de alarma recibido!")

        val isPreAlarm = intent.getBooleanExtra("IS_PRE_ALARM", false)
        val hour = intent.getIntExtra("ALARM_HOUR", 0)
        val minute = intent.getIntExtra("ALARM_MINUTE", 0)
        val melodyPath = intent.getStringExtra("ALARM_MELODY_PATH") ?: ""
        val melodyName = intent.getStringExtra("ALARM_MELODY_NAME") ?: ""
        val vibrate = intent.getBooleanExtra("ALARM_VIBRATE", true)
        val label = intent.getStringExtra("ALARM_LABEL") ?: ""

        if (alarmId == -1) return

        if (isPreAlarm) {
            showPreAlarmNotification(context, alarmId, hour, minute)
        } else {
            // Iniciar el servicio en primer plano para la alarma real
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra("ALARM_ID", alarmId)
                putExtra("ALARM_MELODY_PATH", melodyPath)
                putExtra("ALARM_MELODY_NAME", melodyName)
                putExtra("ALARM_VIBRATE", vibrate)
                putExtra("ALARM_LABEL", label)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    private fun rescheduleAlarms(context: Context) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val db = AlarmDatabase.getDatabase(context)
            val alarms = db.alarmDao.getAllAlarmsWithExceptionsSync()
            val scheduler = AlarmScheduler(context)
            alarms.forEach {
                if (it.alarm.isEnabled) {
                    scheduler.schedule(it)
                }
            }
        }
    }

    private fun showPreAlarmNotification(context: Context, alarmId: Int, hour: Int, minute: Int) {
        val channelId = "pre_alarm_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones de Próxima Alarma",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra("ALARM_ID", alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 30000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.pre_alarm_notification_title))
            .setContentText(context.getString(R.string.pre_alarm_notification_text, timeStr))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.pre_alarm_dismiss_action), dismissPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alarmId + 20000, notification)
    }
}
