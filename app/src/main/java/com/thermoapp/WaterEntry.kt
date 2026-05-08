// WaterEntry.kt
package com.thermoapp

/**
 * Representa una fila de la tabla de vapor saturado.
 * "data class" en Kotlin es como un dataclass de Python:
 * genera automáticamente equals(), toString(), copy(), etc.
 */
data class WaterEntry(
    val pressure: Double,      // kPa
    val temperature: Double,   // °C (temperatura de saturación)
    val vf: Double,            // m³/kg - volumen líquido saturado
    val vg: Double,            // m³/kg - volumen vapor saturado
    val hf: Double,            // kJ/kg - entalpía líquido saturado
    val hg: Double,            // kJ/kg - entalpía vapor saturado
    val sf: Double,            // kJ/kg·K - entropía líquido saturado
    val sg: Double             // kJ/kg·K - entropía vapor saturado
)