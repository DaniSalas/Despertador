package com.danielsalas.despertador.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.danielsalas.despertador.R
import com.danielsalas.despertador.data.local.AlarmWithExceptions
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    alarmsList: List<AlarmWithExceptions>,
    onToggleAlarm: (AlarmWithExceptions, Boolean) -> Unit,
    onDeleteAlarm: (AlarmWithExceptions) -> Unit,
    onAlarmClick: (AlarmWithExceptions) -> Unit,
    onAddAlarmClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onManualClick: () -> Unit
) {
    var showLanguageMenu by remember { mutableStateOf(false) }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLanguage = if (currentLocales.isEmpty) "en" else currentLocales.get(0)?.language ?: "en"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onManualClick) {
                        Icon(Icons.Default.Info, contentDescription = "Manual")
                    }
                    TextButton(onClick = { showLanguageMenu = true }) {
                        Text("🌐", style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false }
                    ) {
                        val languages = listOf(
                            "en" to "English",
                            "es" to "Español Latino",
                            "ca" to "Català",
                            "fr" to "Français",
                            "de" to "Deutsch",
                            "it" to "Italiano",
                            "pt" to "Português"
                        )
                        languages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    val tag = if (code == "es") "es-US" else code
                                    changeAppLanguage(tag)
                                    showLanguageMenu = false
                                },
                                trailingIcon = {
                                    // Comprobamos si el idioma coincide
                                    val isSelected = currentLanguage == code || 
                                                    (code == "es" && currentLanguage.startsWith("es"))
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                    TextButton(onClick = onSettingsClick) {
                        Text("⚙️", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAlarmClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }
    ) { paddingValues ->
        if (alarmsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_alarms),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarmsList) { alarmWithExceptions ->
                    val alarm = alarmWithExceptions.alarm
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAlarmClick(alarmWithExceptions) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (alarm.isEnabled) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val timeText = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)
                                Text(
                                    text = timeText,
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (alarm.label.isNotEmpty()) {
                                    Text(
                                        text = alarm.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                val daysText = getDaysSummary(alarm.getDaysList())
                                if (daysText.isNotEmpty()) {
                                    Text(
                                        text = daysText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Mostrar excepciones activas (que no hayan pasado)
                                val today = LocalDate.now()
                                val activeExceptions = alarmWithExceptions.exceptions
                                    .filter { LocalDate.parse(it.exceptionDate) >= today }
                                    .sortedBy { it.exceptionDate }

                                if (activeExceptions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.exceptions_calendar) + ": " + 
                                               activeExceptions.joinToString(", ") { formatExceptionDate(it.exceptionDate) },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = alarm.isEnabled,
                                    onCheckedChange = { onToggleAlarm(alarmWithExceptions, it) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { onDeleteAlarm(alarmWithExceptions) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Alarm",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun changeAppLanguage(langCode: String) {
    val localeList = LocaleListCompat.forLanguageTags(langCode)
    AppCompatDelegate.setApplicationLocales(localeList)
}

@Composable
private fun getDaysSummary(daysList: List<Int>): String {
    if (daysList.isEmpty()) return ""
    if (daysList.size == 7) return "Todos los días"
    
    val shortNames = mapOf(
        1 to stringResource(R.string.mon),
        2 to stringResource(R.string.tue),
        3 to stringResource(R.string.wed),
        4 to stringResource(R.string.thu),
        5 to stringResource(R.string.fri),
        6 to stringResource(R.string.sat),
        7 to stringResource(R.string.sun)
    )
    
    return daysList.sorted().mapNotNull { shortNames[it] }.joinToString(", ")
}

private fun formatExceptionDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        date.format(DateTimeFormatter.ofPattern("dd/MM"))
    } catch (e: Exception) {
        dateStr
    }
}
