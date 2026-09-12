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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielsalas.despertador.R
import com.danielsalas.despertador.data.local.AlarmWithExceptions
import com.danielsalas.despertador.ui.screens.AlarmEditDialog
import com.danielsalas.despertador.ui.screens.MainScreen
import com.danielsalas.despertador.ui.screens.MelodyPickerScreen
import com.danielsalas.despertador.ui.screens.SettingsScreen
import com.danielsalas.despertador.ui.viewmodel.AlarmViewModel

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
            var isDarkMode by remember { mutableStateOf(false) }
            var customBackgroundColor by remember { mutableStateOf(Color(0xFFF4F4F9)) }

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

                    var currentScreen by remember { mutableStateOf("main") }
                    var showEditDialog by remember { mutableStateOf(false) }
                    var showManualDialog by remember { mutableStateOf(false) }
                    var selectedAlarmForEdit by remember { mutableStateOf<AlarmWithExceptions?>(null) }

                    var tempMelodyName by remember { mutableStateOf<String?>(null) }
                    var tempMelodyPath by remember { mutableStateOf<String?>(null) }

                    if (currentScreen == "main") {
                        MainScreen(
                            alarmsList = alarmsList,
                            onToggleAlarm = { alarm, enabled -> viewModel.toggleAlarm(alarm, enabled) },
                            onDeleteAlarm = { alarm -> viewModel.deleteAlarm(alarm.alarm) },
                            onAlarmClick = { alarm ->
                                selectedAlarmForEdit = alarm
                                showEditDialog = true
                            },
                            onAddAlarmClick = {
                                selectedAlarmForEdit = null
                                tempMelodyName = null
                                tempMelodyPath = null
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
                                alarmWithExceptions = selectedAlarmForEdit,
                                onDismiss = { showEditDialog = false },
                            onSave = { hour, minute, days, path, name, label, vibrate, exceptions ->
                                viewModel.addOrUpdateAlarm(
                                    id = selectedAlarmForEdit?.alarm?.id ?: 0,
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
                                currentSelectedMelodyName = tempMelodyName,
                                currentSelectedMelodyPath = tempMelodyPath,
                                onAddException = { date ->
                                    selectedAlarmForEdit?.let { viewModel.addException(it.alarm.id, date) }
                                },
                                onRemoveException = { date ->
                                    selectedAlarmForEdit?.let { viewModel.removeException(it.alarm.id, date) }
                                }
                            )
                        }
                    } else if (currentScreen == "melody_picker") {
                        MelodyPickerScreen(
                            onMelodySelected = { name, path ->
                                tempMelodyName = name
                                tempMelodyPath = path
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
