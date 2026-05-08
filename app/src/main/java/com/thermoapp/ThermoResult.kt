package com.thermoapp

data class ThermoResult(
    val entry: WaterEntry,
    val phase: Phase,
    val quality: Double? = null,
    val hInput: Double? = null
)

enum class Phase(val label: String, val description: String) {
    LIQUID(
        label = "Líquido comprimido",
        description = "h < hf — el agua está completamente en fase líquida"
    ),
    MIXTURE(
        label = "Mezcla líquido-vapor",
        description = "hf ≤ h ≤ hg — coexisten líquido y vapor saturados"
    ),
    VAPOR(
        label = "Vapor sobrecalentado",
        description = "h > hg — el vapor está completamente en fase gaseosa"
    ),
    SATURATED(
        label = "Estado saturado",
        description = "Solo presión o temperatura — propiedades de saturación"
    )
}