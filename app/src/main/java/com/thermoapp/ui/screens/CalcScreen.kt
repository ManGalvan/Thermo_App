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
import com.thermoapp.ThermoProperty
import com.thermoapp.ThermoViewModel
import com.thermoapp.ui.components.ResultCard
import com.thermoapp.ui.components.ThermoStateCard
import com.thermoapp.validPairsStage1
import androidx.compose.foundation.horizontalScroll

enum class SearchMode {
    PRESSURE, TEMPERATURE, PRESSURE_QUALITY,
    PRESSURE_TEMPERATURE, PRESSURE_VOLUME, DYNAMIC
}

@Composable
fun CalcScreen(viewModel: ThermoViewModel, modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("") }
    var qualityInput by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf(SearchMode.PRESSURE) }
    var error by remember { mutableStateOf("") }
    var errorQuality by remember { mutableStateOf("") }

    val thermoState by viewModel.thermoState.collectAsState()
    var selectedProp1 by remember { mutableStateOf(ThermoProperty.PRESSURE) }
    var selectedProp2 by remember { mutableStateOf(ThermoProperty.TEMPERATURE) }
    var inputProp1 by remember { mutableStateOf("") }
    var inputProp2 by remember { mutableStateOf("") }
    var errorProp1 by remember { mutableStateOf("") }
    var errorProp2 by remember { mutableStateOf("") }

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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
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
            FilterChip(
                selected = searchMode == SearchMode.DYNAMIC,
                onClick  = {
                    searchMode = SearchMode.DYNAMIC
                    input = ""; qualityInput = ""; error = ""; errorQuality = ""
                },
                label    = { Text("Par libre") }
            )
        }

        // Campo principal — oculto en modo DYNAMIC
        if (searchMode != SearchMode.DYNAMIC) {
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
                        SearchMode.DYNAMIC              -> ""
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
        }

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

        // Modo selector dinámico
        if (searchMode == SearchMode.DYNAMIC) {
            Text("Primera propiedad", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ThermoProperty.entries
                    .filter { it != selectedProp2 }
                    .forEach { prop ->
                        FilterChip(
                            selected = selectedProp1 == prop,
                            onClick  = { selectedProp1 = prop; inputProp1 = ""; errorProp1 = "" },
                            label    = { Text(prop.symbol) }
                        )
                    }
            }
            OutlinedTextField(
                value           = inputProp1,
                onValueChange   = { inputProp1 = it; errorProp1 = "" },
                label           = { Text("${selectedProp1.label} (${selectedProp1.unit})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError         = errorProp1.isNotEmpty(),
                supportingText  = { Text(if (errorProp1.isNotEmpty()) errorProp1 else selectedProp1.hint) },
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth()
            )

            Text("Segunda propiedad", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                validPairsStage1
                    .filter { it.first == selectedProp1 || it.second == selectedProp1 }
                    .map { if (it.first == selectedProp1) it.second else it.first }
                    .forEach { prop ->
                        FilterChip(
                            selected = selectedProp2 == prop,
                            onClick  = { selectedProp2 = prop; inputProp2 = ""; errorProp2 = "" },
                            label    = { Text(prop.symbol) }
                        )
                    }
            }
            OutlinedTextField(
                value           = inputProp2,
                onValueChange   = { inputProp2 = it; errorProp2 = "" },
                label           = { Text("${selectedProp2.label} (${selectedProp2.unit})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError         = errorProp2.isNotEmpty(),
                supportingText  = { Text(if (errorProp2.isNotEmpty()) errorProp2 else selectedProp2.hint) },
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth()
            )
        }

        // Botones
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    when (searchMode) {
                        SearchMode.PRESSURE -> {
                            val value = input.toDoubleOrNull()
                            if (value == null) { error = "Ingresa un número válido"; return@Button }
                            val range = repo.getPressureRange()
                            if (value < range.first || value > range.second) {
                                error = "Fuera de rango (%.2f – %.1f)".format(range.first, range.second)
                                return@Button
                            }
                            viewModel.calculate(value, true, null)
                        }
                        SearchMode.TEMPERATURE -> {
                            val value = input.toDoubleOrNull()
                            if (value == null) { error = "Ingresa un número válido"; return@Button }
                            val range = repo.getTemperatureRange()
                            if (value < range.first || value > range.second) {
                                error = "Fuera de rango (%.2f – %.1f)".format(range.first, range.second)
                                return@Button
                            }
                            viewModel.calculate(value, false, null)
                        }
                        SearchMode.PRESSURE_QUALITY -> {
                            val value = input.toDoubleOrNull()
                            if (value == null) { error = "Ingresa un número válido"; return@Button }
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
                            val value = input.toDoubleOrNull()
                            if (value == null) { error = "Ingresa un número válido"; return@Button }
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
                            val value = input.toDoubleOrNull()
                            if (value == null) { error = "Ingresa un número válido"; return@Button }
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
                        SearchMode.DYNAMIC -> {
                            val v1 = inputProp1.toDoubleOrNull()
                            val v2 = inputProp2.toDoubleOrNull()
                            if (v1 == null) { errorProp1 = "Ingresa un valor válido"; return@Button }
                            if (v2 == null) { errorProp2 = "Ingresa un valor válido"; return@Button }
                            viewModel.calculateFromPair(selectedProp1, v1, selectedProp2, v2)
                        }
                    }
                    error = ""
                },
                modifier = Modifier.weight(1f)
            ) { Text("Calcular") }

            OutlinedButton(
                onClick = {
                    viewModel.clearPoints()
                    input = ""; qualityInput = ""; error = ""; errorQuality = ""
                    inputProp1 = ""; inputProp2 = ""; errorProp1 = ""; errorProp2 = ""
                },
                modifier = Modifier.weight(1f)
            ) { Text("Limpiar") }
        }

        result?.let { ResultCard(it) }

        if (searchMode == SearchMode.DYNAMIC) {
            thermoState?.let { ThermoStateCard(it) }
        }
    }
}