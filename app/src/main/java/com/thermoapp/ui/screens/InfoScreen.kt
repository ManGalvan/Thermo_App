package com.thermoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InfoScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Acerca de ThermoCalc", style = MaterialTheme.typography.headlineSmall)

        InfoCard(
            title = "¿Qué calcula esta app?",
            body = "Propiedades termodinámicas del agua en la región de vapor saturado: " +
                    "volumen específico (v), entalpía (h) y entropía (s) para las fases " +
                    "líquida y vapor, a partir de presión o temperatura."
        )

        InfoCard(
            title = "Simbología",
            body = "vf — Volumen líquido saturado (m³/kg)\n" +
                    "vg — Volumen vapor saturado (m³/kg)\n" +
                    "hf — Entalpía líquido saturado (kJ/kg)\n" +
                    "hg — Entalpía vapor saturado (kJ/kg)\n" +
                    "sf — Entropía líquido saturado (kJ/kg·K)\n" +
                    "sg — Entropía vapor saturado (kJ/kg·K)"
        )

        InfoCard(
            title = "Fuente de datos",
            body = "Tablas termodinámicas estándar IAPWS. " +
                    "Rango válido: 1 kPa (6.97 °C) a 22,064 kPa (373.95 °C — punto crítico). " +
                    "Los valores intermedios se obtienen por interpolación lineal."
        )

        InfoCard(
            title = "Interpolación lineal",
            body = "Cuando el valor ingresado no existe exactamente en la tabla, " +
                    "la app calcula el resultado proporcional entre los dos valores " +
                    "más cercanos de la tabla. Esto introduce un error mínimo " +
                    "pero aceptable para uso ingenieril."
        )
    }
}

@Composable
fun InfoCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}