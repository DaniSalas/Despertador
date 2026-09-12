package com.danielsalas.despertador.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alarm_exceptions",
    foreignKeys = [
        ForeignKey(
            entity = AlarmEntity::class,
            parentColumns = ["id"],
            childColumns = ["alarmId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["alarmId"])]
)
data class AlarmExceptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val alarmId: Int,
    val exceptionDate: String // Formato "yyyy-MM-dd" para identificar el día que no debe sonar
)
