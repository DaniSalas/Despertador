package com.danielsalas.despertador.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielsalas.despertador.R
import com.danielsalas.despertador.data.local.AlarmWithExceptions
import com.danielsalas.despertador.ui.screens.AlarmEditDialog
import com.danielsalas.despertador.ui.screens.MainScreen
import com.danielsalas.despertador.ui.screens.MelodyPickerScreen
import com.danielsalas.despertador.ui.screens.SettingsScreen
import com.danielsalas.despertador.ui.viewmodel.AlarmViewModel
import java.time.LocalDate
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar los permisos necesarios al iniciar
        val permissions = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { _ -> }
        permissionLauncher.launch(permissions.toTypedArray())

        setContent {
            // Configuración de Tema Personalizado y Modo Oscuro
            var isDarkMode by rememberSaveable { mutableStateOf(false) }
            val colorSaver = Saver<Color, Int>(
                save = { it.toArgb() },
                restore = { Color(it) }
            )
            var customBackgroundColor by rememberSaveable(stateSaver = colorSaver) { 
                mutableStateOf(Color(0xFFF4F4F9)) 
            }

            val systemInDark = isSystemInDarkTheme()
            val useDarkTheme = isDarkMode || systemInDark

            val colorScheme = if (useDarkTheme) {
                darkColorScheme(background = Color(0xFF121212))
            } else {
                lightColorScheme(background = customBackgroundColor)
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val viewModel: AlarmViewModel = viewModel()
                    val alarmsList by viewModel.allAlarms.collectAsState()

                    var currentScreen by rememberSaveable { mutableStateOf("main") }
                    var showEditDialog by rememberSaveable { mutableStateOf(false) }
                    var showManualDialog by rememberSaveable { mutableStateOf(false) }
                    
                    // Draft Alarm state
                    var draftHour by rememberSaveable { mutableStateOf("08") }
                    var draftMinute by rememberSaveable { mutableStateOf("00") }
                    var draftLabel by rememberSaveable { mutableStateOf("") }
                    var draftVibrate by rememberSaveable { mutableStateOf(true) }
                    var draftSelectedDays by rememberSaveable { mutableStateOf(listOf<Int>()) }
                    var draftMelodyName by rememberSaveable { mutableStateOf("Predeterminado") }
                    var draftMelodyPath by rememberSaveable { mutableStateOf("") }
                    var draftExceptions by rememberSaveable { mutableStateOf(listOf<String>()) }
                    
                    var selectedAlarmId by rememberSaveable { mutableIntStateOf(0) }
                    var isEditingExisting by rememberSaveable { mutableStateOf(false) }

                    if (currentScreen == "main") {
                        MainScreen(
                            alarmsList = alarmsList,
                            onToggleAlarm = { alarm, enabled -> viewModel.toggleAlarm(alarm, enabled) },
                            onDeleteAlarm = { alarm -> viewModel.deleteAlarm(alarm.alarm) },
                            onAlarmClick = { alarm ->
                                isEditingExisting = true
                                selectedAlarmId = alarm.alarm.id
                                draftHour = String.format(Locale.getDefault(), "%02d", alarm.alarm.hour)
                                draftMinute = String.format(Locale.getDefault(), "%02d", alarm.alarm.minute)
                                draftLabel = alarm.alarm.label
                                draftVibrate = alarm.alarm.isVibrate
                                draftSelectedDays = alarm.alarm.getDaysList()
                                draftMelodyName = alarm.alarm.melodyName
                                draftMelodyPath = alarm.alarm.melodyPath
                                val today = LocalDate.now()
                                draftExceptions = alarm.exceptions
                                    .map { it.exceptionDate }
                                    .filter { LocalDate.parse(it) >= today }
                                showEditDialog = true
                            },
                            onAddAlarmClick = {
                                isEditingExisting = false
                                selectedAlarmId = 0
                                draftHour = "08"
                                draftMinute = "00"
                                draftLabel = ""
                                draftVibrate = true
                                draftSelectedDays = emptyList()
                                draftMelodyName = "Predeterminado"
                                draftMelodyPath = ""
                                draftExceptions = emptyList()
                                showEditDialog = true
                            },
                            onSettingsClick = {
                                currentScreen = "settings"
                            },
                            onManualClick = {
                                showManualDialog = true
                            }
                        )

                        if (showManualDialog) {
                            AlertDialog(
                                onDismissRequest = { showManualDialog = false },
                                title = { Text(stringResource(R.string.manual)) },
                                text = { 
                                    Box(modifier = androidx.compose.ui.Modifier.heightIn(max = 400.dp)) {
                                        androidx.compose.foundation.lazy.LazyColumn {
                                            item {
                                                Text(stringResource(R.string.manual_content))
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showManualDialog = false }) {
                                        Text("OK")
                                    }
                                }
                            )
                        }

                        if (showEditDialog) {
                            AlarmEditDialog(
                                alarmWithExceptions = if (isEditingExisting) {
                                    alarmsList.find { it.alarm.id == selectedAlarmId }
                                } else null,
                                onDismiss = { showEditDialog = false },
                                onSave = { hour, minute, days, path, name, label, vibrate, exceptions ->
                                    viewModel.addOrUpdateAlarm(
                                        id = if (isEditingExisting) selectedAlarmId else 0,
                                        hour = hour,
                                        minute = minute,
                                        days = days,
                                        melodyPath = path,
                                        melodyName = name,
                                        label = label,
                                        vibrate = vibrate,
                                        initialExceptions = exceptions
                                    )
                                    showEditDialog = false
                                },
                                onSelectMelodyClick = {
                                    currentScreen = "melody_picker"
                                },
                                hourStr = draftHour,
                                onHourChange = { draftHour = it },
                                minuteStr = draftMinute,
                                onMinuteChange = { draftMinute = it },
                                label = draftLabel,
                                onLabelChange = { draftLabel = it },
                                vibrate = draftVibrate,
                                onVibrateChange = { draftVibrate = it },
                                selectedDays = draftSelectedDays,
                                onSelectedDaysChange = { draftSelectedDays = it },
                                melodyName = draftMelodyName,
                                onMelodyNameChange = { draftMelodyName = it },
                                melodyPath = draftMelodyPath,
                                onMelodyPathChange = { draftMelodyPath = it },
                                localExceptions = draftExceptions,
                                onLocalExceptionsChange = { newExceptions ->
                                    draftExceptions = newExceptions
                                },
                                onAddException = { date ->
                                    if (isEditingExisting) {
                                        viewModel.addException(selectedAlarmId, date)
                                    }
                                },
                                onRemoveException = { date ->
                                    if (isEditingExisting) {
                                        viewModel.removeException(selectedAlarmId, date)
                                    }
                                }
                            )
                        }
                    } else if (currentScreen == "melody_picker") {
                        MelodyPickerScreen(
                            onMelodySelected = { name, path ->
                                draftMelodyName = name
                                draftMelodyPath = path
                                currentScreen = "main"
                                showEditDialog = true
                            },
                            onBack = { 
                                currentScreen = "main"
                                showEditDialog = true
                            }
                        )
                    } else if (currentScreen == "settings") {
                        SettingsScreen(
                            isDarkMode = isDarkMode,
                            onDarkModeChange = { isDarkMode = it },
                            selectedColor = customBackgroundColor,
                            onColorSelect = { customBackgroundColor = it },
                            onBack = { currentScreen = "main" }
                        )
                    }
                }
            }
        }
    }
}
