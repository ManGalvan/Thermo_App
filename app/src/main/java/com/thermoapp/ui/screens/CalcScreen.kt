package com.thermoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thermoapp.ThermoViewModel
import com.thermoapp.ui.components.ResultCard

@Composable
fun CalcScreen(viewModel: ThermoViewModel, modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("") }
    var searchByPressure by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    // collectAsState() observa el StateFlow del ViewModel
    // cuando cambia, Compose redibuja automáticamente
    val result by viewModel.result.collectAsState()
    val repo = viewModel.getRepo()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Agua — Vapor Saturado", style = MaterialTheme.typography.headlineSmall)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Buscar por:", style = MaterialTheme.typography.bodyMedium)
            FilterChip(
                selected = searchByPressure,
                onClick = { searchByPressure = true; input = ""; error = "" },
                label = { Text("Presión (kPa)") }
            )
            FilterChip(
                selected = !searchByPressure,
                onClick = { searchByPressure = false; input = ""; error = "" },
                label = { Text("Temp. (°C)") }
            )
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it; error = "" },
            label = { Text(if (searchByPressure) "Presión (kPa)" else "Temperatura (°C)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = error.isNotEmpty(),
            supportingText = {
                val range = if (searchByPressure) repo.getPressureRange()
                else repo.getTemperatureRange()
                Text(
                    if (error.isNotEmpty()) error
                    else "Rango: %.1f – %.1f".format(range.first, range.second)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val value = input.toDoubleOrNull()
                    if (value == null) { error = "Ingresa un número válido"; return@Button }
                    val range = if (searchByPressure) repo.getPressureRange()
                    else repo.getTemperatureRange()
                    if (value < range.first || value > range.second) {
                        error = "Fuera de rango (%.1f – %.1f)".format(range.first, range.second)
                        return@Button
                    }
                    viewModel.calculate(value, searchByPressure, null)
                    error = ""
                },
                modifier = Modifier.weight(1f)
            ) { Text("Calcular") }

            OutlinedButton(
                onClick = { viewModel.clearPoints(); input = ""; error = "" },
                modifier = Modifier.weight(1f)
            ) { Text("Limpiar") }
        }

        result?.let { ResultCard(it) }
    }
}