package com.danielsalas.despertador.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class AlarmWithExceptions(
    @Embedded val alarm: AlarmEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "alarmId"
    )
    val exceptions: List<AlarmExceptionEntity>
)

@Dao
interface AlarmDao {
    @Transaction
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarmsWithExceptions(): Flow<List<AlarmWithExceptions>>

    @Transaction
    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    suspend fun getAlarmWithExceptionsById(alarmId: Int): AlarmWithExceptions?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertException(exception: AlarmExceptionEntity): Long

    @Query("DELETE FROM alarm_exceptions WHERE alarmId = :alarmId AND exceptionDate = :date")
    suspend fun deleteException(alarmId: Int, date: String)

    @Query("DELETE FROM alarm_exceptions WHERE alarmId = :alarmId")
    suspend fun deleteAllExceptionsForAlarm(alarmId: Int)

    @Query("DELETE FROM alarm_exceptions WHERE exceptionDate < :date")
    suspend fun deleteOldExceptions(date: String)
}
