package com.danielsalas.despertador.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.danielsalas.despertador.R
import com.danielsalas.despertador.ui.AlarmRingActivity

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val CHANNEL_ID = "alarm_service_channel"

    companion object {
        const val ACTION_STOP = "com.danielsalas.despertador.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Despertador:ServiceWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        wakeLock?.acquire(30 * 60 * 1000L /*30 minutes*/)
        if (intent?.action == ACTION_STOP) {
            Log.d("AlarmService", "Recibida acción de detener alarma")
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d("AlarmService", "AlarmService iniciado")

        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        val melodyPath = intent?.getStringExtra("ALARM_MELODY_PATH") ?: ""
        val melodyName = intent?.getStringExtra("ALARM_MELODY_NAME") ?: "Predeterminado"
        val vibrate = intent?.getBooleanExtra("ALARM_VIBRATE", true) ?: true
        val label = intent?.getStringExtra("ALARM_LABEL") ?: "Alarma"

        // Intent para apagar desde la notificación
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para abrir la actividad de pantalla completa
        val fullScreenIntent = Intent(this, AlarmRingActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_MELODY_NAME", melodyName)
            putExtra("ALARM_LABEL", label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, alarmId, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mostrar notificación de Foreground Service con acción y fullScreenIntent
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(label)
            .setContentText(stringResource(R.string.app_name)) // Fallback text
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Apagar", stopPendingIntent)
            .build()

        startForeground(alarmId.coerceAtLeast(1), notification)

        // Reproducir sonido
        playAlarmSound(melodyPath)

        // Vibración
        if (vibrate) {
            startVibration()
        }

        return START_STICKY
    }

    private fun stringResource(id: Int): String {
        return try { applicationContext.getString(id) } catch (e: Exception) { "Alarma" }
    }

    private fun playAlarmSound(melodyPath: String) {
        stopAlarmSound()

        try {
            val uri = if (melodyPath.isNotEmpty()) {
                Uri.parse(melodyPath)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }

            mediaPlayer = MediaPlayer().apply {
                setWakeMode(this@AlarmService, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(this@AlarmService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            Log.d("AlarmService", "Reproduciendo sonido: $melodyPath")
        } catch (e: Exception) {
            Log.e("AlarmService", "Error reproduciendo sonido seleccionado, usando fallback", e)
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setWakeMode(this@AlarmService, PowerManager.PARTIAL_WAKE_LOCK)
                    setDataSource(this@AlarmService, fallbackUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (ex: Exception) {
                Log.e("AlarmService", "Error crítico en fallback de audio", ex)
            }
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        Log.d("AlarmService", "AlarmService destruído, deteniendo sonido y vibración")
        stopAlarmSound()
        stopVibration()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Canal del Despertador",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Se usa para reproducir la alarma sonora en segundo plano."
                setSound(null, null) 
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
