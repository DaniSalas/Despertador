package com.danielsalas.despertador.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.danielsalas.despertador.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "¡Alarma recibida por BroadcastReceiver!")

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val melodyPath = intent.getStringExtra("ALARM_MELODY_PATH") ?: ""
        val melodyName = intent.getStringExtra("ALARM_MELODY_NAME") ?: ""
        val vibrate = intent.getBooleanExtra("ALARM_VIBRATE", true)
        val label = intent.getStringExtra("ALARM_LABEL") ?: ""

        if (alarmId == -1) return

        // Iniciar el servicio en primer plano. 
        // El servicio se encargará de mostrar la notificación con FullScreenIntent.
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
