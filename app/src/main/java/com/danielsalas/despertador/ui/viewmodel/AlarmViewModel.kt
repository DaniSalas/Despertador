package com.danielsalas.despertador.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.danielsalas.despertador.data.local.AlarmDatabase
import com.danielsalas.despertador.data.local.AlarmEntity
import com.danielsalas.despertador.data.local.AlarmWithExceptions
import com.danielsalas.despertador.data.repository.AlarmRepository
import com.danielsalas.despertador.domain.AlarmScheduler
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository
    private val scheduler: AlarmScheduler
    val allAlarms: StateFlow<List<AlarmWithExceptions>>

    init {
        val alarmDao = AlarmDatabase.getDatabase(application).alarmDao
        repository = AlarmRepository(alarmDao)
        scheduler = AlarmScheduler(application)
        allAlarms = repository.allAlarms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        // Cleanup old exceptions on startup
        viewModelScope.launch {
            repository.cleanOldExceptions(LocalDate.now().toString())
        }
    }

    fun addOrUpdateAlarm(
        id: Int = 0,
        hour: Int,
        minute: Int,
        days: List<Int>,
        melodyPath: String,
        melodyName: String,
        label: String,
        vibrate: Boolean,
        enabled: Boolean = true,
        initialExceptions: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val daysStr = days.joinToString(",")
            val alarm = AlarmEntity(
                id = id,
                hour = hour,
                minute = minute,
                daysOfWeek = daysStr,
                melodyPath = melodyPath,
                melodyName = melodyName,
                label = label,
                isVibrate = vibrate,
                isEnabled = enabled
            )
            
            val newId = if (id == 0) {
                val insertedId = repository.insertAlarm(alarm)
                // Add initial exceptions for new alarm
                initialExceptions.forEach { date ->
                    repository.addException(insertedId, date)
                }
                insertedId
            } else {
                repository.updateAlarm(alarm)
                id
            }

            // Reprogramar en el sistema
            repository.getAlarmById(newId)?.let {
                scheduler.schedule(it)
            }
        }
    }

    fun toggleAlarm(alarmWithExceptions: AlarmWithExceptions, enabled: Boolean) {
        viewModelScope.launch {
            val updatedAlarm = alarmWithExceptions.alarm.copy(isEnabled = enabled)
            repository.updateAlarm(updatedAlarm)
            
            if (enabled) {
                scheduler.schedule(alarmWithExceptions.copy(alarm = updatedAlarm))
            } else {
                scheduler.cancel(updatedAlarm.id)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            scheduler.cancel(alarm.id)
            repository.deleteAlarm(alarm)
        }
    }

    fun addException(alarmId: Int, date: String) {
        viewModelScope.launch {
            repository.addException(alarmId, date)
            repository.getAlarmById(alarmId)?.let {
                scheduler.schedule(it)
            }
        }
    }

    fun removeException(alarmId: Int, date: String) {
        viewModelScope.launch {
            repository.removeException(alarmId, date)
            repository.getAlarmById(alarmId)?.let {
                scheduler.schedule(it)
            }
        }
    }
}
