package com.thermoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thermoapp.Phase
import com.thermoapp.ThermoState

@Composable
fun ThermoStateCard(state: ThermoState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Estado termodinámico", style = MaterialTheme.typography.titleMedium)
                PhaseChip(state.phase)
            }

            Text(
                text = state.phase.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            PropertyRow("P — Presión",        "%.4f kPa".format(state.pressure))
            PropertyRow("T — Temperatura",     "%.4f °C".format(state.temperature))
            PropertyRow("v — Vol. específico", "%.6f m³/kg".format(state.specificVolume))
            PropertyRow("h — Entalpía",        "%.4f kJ/kg".format(state.enthalpy))
            PropertyRow("s — Entropía",        "%.6f kJ/kg·K".format(state.entropy))

            state.quality?.let { x ->
                HorizontalDivider()
                QualityBar(x)
                PropertyRow("x — Calidad", "%.4f".format(x))
            }

            // Propiedades de saturación a esa presión
            state.satEntry?.let { sat ->
                HorizontalDivider()
                Text("Saturación a %.2f kPa".format(state.pressure),
                    style = MaterialTheme.typography.titleSmall)
                PropertyRow("T sat.", "%.4f °C".format(sat.temperature))
                PropertyRow("hf", "%.4f kJ/kg".format(sat.hf))
                PropertyRow("hg", "%.4f kJ/kg".format(sat.hg))
                PropertyRow("sf", "%.6f kJ/kg·K".format(sat.sf))
                PropertyRow("sg", "%.6f kJ/kg·K".format(sat.sg))
            }
        }
    }
}