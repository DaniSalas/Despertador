package com.danielsalas.despertador.domain

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.danielsalas.despertador.data.local.AlarmWithExceptions
import com.danielsalas.despertador.receiver.AlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(alarmWithExceptions: AlarmWithExceptions) {
        val alarm = alarmWithExceptions.alarm
        if (!alarm.isEnabled) {
            cancel(alarm.id)
            return
        }

        val exceptions = alarmWithExceptions.exceptions.map { it.exceptionDate }
        val triggerTimeMs = calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.getDaysList(), exceptions)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_HOUR", alarm.hour)
            putExtra("ALARM_MINUTE", alarm.minute)
            putExtra("ALARM_MELODY_PATH", alarm.melodyPath)
            putExtra("ALARM_MELODY_NAME", alarm.melodyName)
            putExtra("ALARM_VIBRATE", alarm.isVibrate)
            putExtra("ALARM_LABEL", alarm.label)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        }
        Log.d("AlarmScheduler", "Alarma ${alarm.id} programada para milisegundos: $triggerTimeMs")
    }

    fun cancel(alarmId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Alarma $alarmId cancelada")
        }
    }

    fun calculateNextTriggerTime(
        hour: Int,
        minute: Int,
        activeDays: List<Int>,
        exceptions: List<String>
    ): Long {
        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }

        if (activeDays.isEmpty()) {
            while (exceptions.contains(target.toLocalDate().toString())) {
                target = target.plusDays(1)
            }
            return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        for (i in 0..365) {
            val dayVal = target.dayOfWeek.value // 1 = Lunes, 7 = Domingo
            if (activeDays.contains(dayVal)) {
                val dateStr = target.toLocalDate().toString()
                if (!exceptions.contains(dateStr)) {
                    return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }
            target = target.plusDays(1)
        }

        return System.currentTimeMillis() + 60000
    }
}
