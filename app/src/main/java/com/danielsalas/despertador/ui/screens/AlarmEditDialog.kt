package com.danielsalas.despertador.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.danielsalas.despertador.R
import com.danielsalas.despertador.data.local.AlarmWithExceptions
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditDialog(
    alarmWithExceptions: AlarmWithExceptions?,
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, days: List<Int>, melodyPath: String, melodyName: String, label: String, vibrate: Boolean, exceptions: List<String>) -> Unit,
    onSelectMelodyClick: () -> Unit,
    currentSelectedMelodyName: String?,
    currentSelectedMelodyPath: String?,
    onAddException: (String) -> Unit,
    onRemoveException: (String) -> Unit
) {
    var hourStr by remember { mutableStateOf(String.format(Locale.getDefault(), "%02d", alarmWithExceptions?.alarm?.hour ?: 8)) }
    var minuteStr by remember { mutableStateOf(String.format(Locale.getDefault(), "%02d", alarmWithExceptions?.alarm?.minute ?: 0)) }
    var label by remember { mutableStateOf(alarmWithExceptions?.alarm?.label ?: "") }
    var vibrate by remember { mutableStateOf(alarmWithExceptions?.alarm?.isVibrate ?: true) }
    
    var selectedDays by remember {
        mutableStateOf(alarmWithExceptions?.alarm?.getDaysList() ?: emptyList())
    }

    var melodyName by remember { mutableStateOf(alarmWithExceptions?.alarm?.melodyName ?: "Predeterminado") }
    var melodyPath by remember { mutableStateOf(alarmWithExceptions?.alarm?.melodyPath ?: "") }

    var localExceptions by remember {
        val today = LocalDate.now()
        mutableStateOf(
            alarmWithExceptions?.exceptions
                ?.map { it.exceptionDate }
                ?.filter { LocalDate.parse(it) >= today }
                ?: emptyList<String>()
        )
    }

    LaunchedEffect(currentSelectedMelodyName, currentSelectedMelodyPath) {
        if (currentSelectedMelodyName != null && currentSelectedMelodyPath != null) {
            melodyName = currentSelectedMelodyName
            melodyPath = currentSelectedMelodyPath
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(if (alarmWithExceptions == null) R.string.add_alarm else R.string.edit_alarm),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeInput(
                        value = hourStr,
                        onValueChange = { hourStr = it },
                        label = "HH"
                    )
                    Text(
                        ":",
                        style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    TimeInput(
                        value = minuteStr,
                        onValueChange = { minuteStr = it },
                        label = "MM"
                    )
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.label)) },
                    placeholder = { Text(stringResource(R.string.label_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.days_of_week), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val daysList = listOf(
                            1 to stringResource(R.string.mon),
                            2 to stringResource(R.string.tue),
                            3 to stringResource(R.string.wed),
                            4 to stringResource(R.string.thu),
                            5 to stringResource(R.string.fri),
                            6 to stringResource(R.string.sat),
                            7 to stringResource(R.string.sun)
                        )
                        daysList.forEach { (valDay, nameDay) ->
                            val isSelected = selectedDays.contains(valDay)
                            Surface(
                                onClick = {
                                    selectedDays = if (isSelected) {
                                        selectedDays.filter { it != valDay }
                                    } else {
                                        selectedDays + valDay
                                    }
                                },
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = nameDay,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    onClick = onSelectMelodyClick
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.melody), style = MaterialTheme.typography.labelLarge)
                            Text(melodyName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                        Text(stringResource(R.string.select_melody), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = vibrate, onCheckedChange = { vibrate = it })
                        Text(stringResource(R.string.vibrate), style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    Button(
                        onClick = { showDatePicker = true },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.exceptions_calendar))
                    }
                }

                if (localExceptions.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(localExceptions) { dateStr ->
                            InputChip(
                                selected = true,
                                onClick = { },
                                label = { Text(formatExceptionDate(dateStr)) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp).clickable {
                                            localExceptions = localExceptions.filter { it != dateStr }
                                            if (alarmWithExceptions != null) {
                                                onRemoveException(dateStr)
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            val h = hourStr.toIntOrNull() ?: 8
                            val m = minuteStr.toIntOrNull() ?: 0
                            onSave(h.coerceIn(0, 23), m.coerceIn(0, 59), selectedDays, melodyPath, melodyName, label, vibrate, localExceptions)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                        if (!localExceptions.contains(date)) {
                            localExceptions = localExceptions + date
                            if (alarmWithExceptions != null) {
                                onAddException(date)
                            }
                        }
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun TimeInput(value: String, onValueChange: (String) -> Unit, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 2)) {
                    onValueChange(newValue)
                }
            },
            modifier = Modifier.width(80.dp),
            textStyle = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatExceptionDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()))
    } catch (e: Exception) {
        dateStr
    }
}
