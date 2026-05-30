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

enum class SearchMode { PRESSURE, TEMPERATURE, PRESSURE_QUALITY, PRESSURE_TEMPERATURE, PRESSURE_VOLUME }

@Composable
fun CalcScreen(viewModel: ThermoViewModel, modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("") }
    var qualityInput by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf(SearchMode.PRESSURE) }
    var error by remember { mutableStateOf("") }
    var errorQuality by remember { mutableStateOf("") }

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

        // Selector de modo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = searchMode == SearchMode.PRESSURE,
                onClick  = { searchMode = SearchMode.PRESSURE; input = ""; error = "" },
                label    = { Text("Presión") }
            )
            FilterChip(
                selected = searchMode == SearchMode.TEMPERATURE,
                onClick  = { searchMode = SearchMode.TEMPERATURE; input = ""; error = "" },
                label    = { Text("Temp.") }
            )
            FilterChip(
                selected = searchMode == SearchMode.PRESSURE_QUALITY,
                onClick  = {
                    searchMode = SearchMode.PRESSURE_QUALITY
                    input = ""; qualityInput = ""; error = ""; errorQuality = ""
                },
                label    = { Text("P + x") }
            )
            FilterChip(
                selected = searchMode == SearchMode.PRESSURE_TEMPERATURE,
                onClick  = {
                    searchMode = SearchMode.PRESSURE_TEMPERATURE
                    input = ""; qualityInput = ""; error = ""; errorQuality = ""
                },
                label    = { Text("P + T") }
            )
            FilterChip(
                selected = searchMode == SearchMode.PRESSURE_VOLUME,
                onClick  = {
                    searchMode = SearchMode.PRESSURE_VOLUME
                    input = ""; qualityInput = ""; error = ""; errorQuality = ""
                },
                label    = { Text("P + v") }
            )
        }

        // Campo principal
        OutlinedTextField(
            value         = input,
            onValueChange = { input = it; error = "" },
            label         = {
                Text(when (searchMode) {
                    SearchMode.PRESSURE             -> "Presión (kPa)"
                    SearchMode.TEMPERATURE          -> "Temperatura (°C)"
                    SearchMode.PRESSURE_QUALITY     -> "Presión (kPa)"
                    SearchMode.PRESSURE_TEMPERATURE -> "Presión (kPa)"
                    SearchMode.PRESSURE_VOLUME      -> "Presión (kPa)"
                })
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError         = error.isNotEmpty(),
            supportingText  = {
                val range = if (searchMode == SearchMode.TEMPERATURE)
                    repo.getTemperatureRange() else repo.getPressureRange()
                Text(
                    if (error.isNotEmpty()) error
                    else "Rango: %.2f – %.1f".format(range.first, range.second)
                )
            },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth()
        )

        // Campo de calidad x — solo en modo P + x
        if (searchMode == SearchMode.PRESSURE_QUALITY) {
            OutlinedTextField(
                value         = qualityInput,
                onValueChange = { qualityInput = it; errorQuality = "" },
                label         = { Text("Calidad x (0 – 1)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError         = errorQuality.isNotEmpty(),
                supportingText  = {
                    Text(
                        if (errorQuality.isNotEmpty()) errorQuality
                        else "x=0 líquido sat.  ·  x=1 vapor sat."
                    )
                },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        }

        // Campo de temperatura — solo en modo P + T
        if (searchMode == SearchMode.PRESSURE_TEMPERATURE) {
            OutlinedTextField(
                value         = qualityInput,
                onValueChange = { qualityInput = it; errorQuality = "" },
                label         = { Text("Temperatura (°C)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError         = errorQuality.isNotEmpty(),
                supportingText  = {
                    Text(
                        if (errorQuality.isNotEmpty()) errorQuality
                        else "Rango: 0.01 – 2000 °C aprox."
                    )
                },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        }

        // Campo de volumen — solo en modo P + v
        if (searchMode == SearchMode.PRESSURE_VOLUME) {
            OutlinedTextField(
                value         = qualityInput,
                onValueChange = { qualityInput = it; errorQuality = "" },
                label         = { Text("Volumen específico v (m³/kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError         = errorQuality.isNotEmpty(),
                supportingText  = {
                    Text(
                        if (errorQuality.isNotEmpty()) errorQuality
                        else "Ej: 0.001043 (líq. sat.)  ·  1.694 (vap. sat.) a 100 kPa"
                    )
                },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        }

        // Botones
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val value = input.toDoubleOrNull()
                    if (value == null) { error = "Ingresa un número válido"; return@Button }

                    when (searchMode) {
                        SearchMode.PRESSURE -> {
                            val range = repo.getPressureRange()
                            if (value < range.first || value > range.second) {
                                error = "Fuera de rango (%.2f – %.1f)".format(range.first, range.second)
                                return@Button
                            }
                            viewModel.calculate(value, true, null)
                        }
                        SearchMode.TEMPERATURE -> {
                            val range = repo.getTemperatureRange()
                            if (value < range.first || value > range.second) {
                                error = "Fuera de rango (%.2f – %.1f)".format(range.first, range.second)
                                return@Button
                            }
                            viewModel.calculate(value, false, null)
                        }
                        SearchMode.PRESSURE_QUALITY -> {
                            val x = qualityInput.toDoubleOrNull()
                            if (x == null || x < 0.0 || x > 1.0) {
                                errorQuality = "Ingresa un valor entre 0 y 1"
                                return@Button
                            }
                            val range = repo.getPressureRange()
                            if (value < range.first || value > range.second) {
                                error = "Fuera de rango (%.2f – %.1f)".format(range.first, range.second)
                                return@Button
                            }
                            viewModel.calculateByPressureAndQuality(value, x)
                        }
                        SearchMode.PRESSURE_TEMPERATURE -> {
                            val t = qualityInput.toDoubleOrNull()
                            if (t == null) {
                                errorQuality = "Ingresa una temperatura válida"
                                return@Button
                            }
                            val range = repo.getPressureRange()
                            if (value < range.first || value > range.second) {
                                error = "Fuera de rango (%.2f – %.1f)".format(range.first, range.second)
                                return@Button
                            }
                            viewModel.calculateByPressureAndTemperature(value, t)
                        }
                        SearchMode.PRESSURE_VOLUME -> {
                            val v = qualityInput.toDoubleOrNull()
                            if (v == null || v <= 0.0) {
                                errorQuality = "Ingresa un volumen específico válido"
                                return@Button
                            }
                            val range = repo.getPressureRange()
                            if (value < range.first || value > range.second) {
                                error = "Fuera de rango (%.2f – %.1f)".format(range.first, range.second)
                                return@Button
                            }
                            viewModel.calculateByPressureAndVolume(value, v)
                        }
                    }
                    error = ""
                },
                modifier = Modifier.weight(1f)
            ) { Text("Calcular") }

            OutlinedButton(
                onClick  = {
                    viewModel.clearPoints()
                    input = ""; qualityInput = ""; error = ""; errorQuality = ""
                },
                modifier = Modifier.weight(1f)
            ) { Text("Limpiar") }
        }

        result?.let { ResultCard(it) }
    }
}