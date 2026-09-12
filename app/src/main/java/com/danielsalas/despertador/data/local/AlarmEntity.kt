package com.danielsalas.despertador.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: String, // Comma-separated integers: "1,2,3,4,5,6,7" (1 = Lunes, 7 = Domingo)
    val melodyPath: String, // Ruta completa del archivo de sonido/música
    val melodyName: String, // Nombre para mostrar de la melodía
    val isEnabled: Boolean = true,
    val isVibrate: Boolean = true,
    val label: String = ""
) {
    // Convierte los días de la semana de String a una lista de enteros
    fun getDaysList(): List<Int> {
        if (daysOfWeek.isBlank()) return emptyList()
        return daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}
