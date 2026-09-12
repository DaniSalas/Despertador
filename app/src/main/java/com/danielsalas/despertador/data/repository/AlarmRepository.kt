package com.danielsalas.despertador.data.repository

import com.danielsalas.despertador.data.local.AlarmDao
import com.danielsalas.despertador.data.local.AlarmEntity
import com.danielsalas.despertador.data.local.AlarmExceptionEntity
import com.danielsalas.despertador.data.local.AlarmWithExceptions
import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val alarmDao: AlarmDao) {

    val allAlarms: Flow<List<AlarmWithExceptions>> = alarmDao.getAllAlarmsWithExceptions()

    suspend fun getAlarmById(id: Int): AlarmWithExceptions? {
        return alarmDao.getAlarmWithExceptionsById(id)
    }

    suspend fun insertAlarm(alarm: AlarmEntity): Int {
        return alarmDao.insertAlarm(alarm).toInt()
    }

    suspend fun updateAlarm(alarm: AlarmEntity) {
        alarmDao.updateAlarm(alarm)
    }

    suspend fun deleteAlarm(alarm: AlarmEntity) {
        alarmDao.deleteAlarm(alarm)
    }

    suspend fun addException(alarmId: Int, date: String) {
        alarmDao.insertException(AlarmExceptionEntity(alarmId = alarmId, exceptionDate = date))
    }

    suspend fun removeException(alarmId: Int, date: String) {
        alarmDao.deleteException(alarmId, date)
    }

    suspend fun cleanOldExceptions(today: String) {
        alarmDao.deleteOldExceptions(today)
    }
}
