package com.danielsalas.despertador.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.danielsalas.despertador.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    selectedColor: Color,
    onColorSelect: (Color) -> Unit,
    onBack: () -> Unit
) {
    var red by remember { mutableFloatStateOf(selectedColor.red) }
    var green by remember { mutableFloatStateOf(selectedColor.green) }
    var blue by remember { mutableFloatStateOf(selectedColor.blue) }

    val currentColor = Color(red, green, blue)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Modo Oscuro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dark_mode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Switch(checked = isDarkMode, onCheckedChange = onDarkModeChange)
            }

            HorizontalDivider()

            // Selector de Color de Fondo (Espectro Completo RGB)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.background_color),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Visualización del color actual
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(currentColor, shape = RoundedCornerShape(12.dp))
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "RGB: ${(red * 255).toInt()}, ${(green * 255).toInt()}, ${(blue * 255).toInt()}",
                        color = if ((red + green + blue) / 3 > 0.5f) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Sliders para RGB
                ColorSlider(label = "R", value = red, color = Color.Red, onValueChange = { red = it })
                ColorSlider(label = "G", value = green, color = Color.Green, onValueChange = { green = it })
                ColorSlider(label = "B", value = blue, color = Color.Blue, onValueChange = { blue = it })

                Button(
                    onClick = { onColorSelect(currentColor) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
fun ColorSlider(label: String, value: Float, color: Color, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color.copy(alpha = 0.5f)
            )
        )
        Text("${(value * 255).toInt()}", modifier = Modifier.width(35.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}
