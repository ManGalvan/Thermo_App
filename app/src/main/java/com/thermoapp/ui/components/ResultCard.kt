package com.thermoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thermoapp.Phase
import com.thermoapp.ThermoResult

@Composable
fun ResultCard(result: ThermoResult) {
    val e = result.entry

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Encabezado con chip de fase
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Resultados", style = MaterialTheme.typography.titleMedium)
                PhaseChip(result.phase)
            }

            Text(
                text = result.phase.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            PropertyRow("Presión",          "%.2f kPa".format(e.pressure))
            PropertyRow("Temperatura sat.", "%.2f °C".format(e.temperature))

            HorizontalDivider()

            PropertyRow("vf", "%.6f m³/kg".format(e.vf))
            PropertyRow("vg", "%.4f m³/kg".format(e.vg))
            PropertyRow("hf", "%.2f kJ/kg".format(e.hf))
            PropertyRow("hg", "%.2f kJ/kg".format(e.hg))
            PropertyRow("sf", "%.4f kJ/kg·K".format(e.sf))
            PropertyRow("sg", "%.4f kJ/kg·K".format(e.sg))

            // Sección de mezcla — solo aparece si hay calidad x
            result.quality?.let { x ->
                HorizontalDivider()
                Text("Propiedades de la mezcla", style = MaterialTheme.typography.titleSmall)

                QualityBar(x)

                PropertyRow("Calidad (x)", "%.4f".format(x))
                PropertyRow("h ingresada", "%.2f kJ/kg".format(result.hInput!!))

                val v = e.vf + x * (e.vg - e.vf)
                val s = e.sf + x * (e.sg - e.sf)
                PropertyRow("v mezcla", "%.6f m³/kg".format(v))
                PropertyRow("s mezcla", "%.4f kJ/kg·K".format(s))
            }
        }
    }
}

@Composable
fun QualityBar(x: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Líquido sat.", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Vapor sat.", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { x.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = "x = %.1f%% vapor,  %.1f%% líquido".format(x * 100, (1 - x) * 100),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun PhaseChip(phase: Phase) {
    val containerColor = when (phase) {
        Phase.LIQUID    -> MaterialTheme.colorScheme.primaryContainer
        Phase.MIXTURE   -> MaterialTheme.colorScheme.tertiaryContainer
        Phase.VAPOR     -> MaterialTheme.colorScheme.secondaryContainer
        Phase.SATURATED -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (phase) {
        Phase.LIQUID    -> MaterialTheme.colorScheme.onPrimaryContainer
        Phase.MIXTURE   -> MaterialTheme.colorScheme.onTertiaryContainer
        Phase.VAPOR     -> MaterialTheme.colorScheme.onSecondaryContainer
        Phase.SATURATED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = phase.label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}