package com.thermoapp

/**
 * Propiedades termodinámicas disponibles como entrada.
 * Cada una tiene una etiqueta, unidades y símbolo para la UI.
 */
enum class ThermoProperty(
    val label: String,
    val symbol: String,
    val unit: String,
    val hint: String
) {
    PRESSURE(
        label  = "Presión",
        symbol = "P",
        unit   = "kPa",
        hint   = "0.61 – 22064 kPa"
    ),
    TEMPERATURE(
        label  = "Temperatura",
        symbol = "T",
        unit   = "°C",
        hint   = "0.01 – 373.95 °C"
    ),
    SPECIFIC_VOLUME(
        label  = "Vol. específico",
        symbol = "v",
        unit   = "m³/kg",
        hint   = "Ej: 1.694 a 100 kPa sat."
    ),
    ENTHALPY(
        label  = "Entalpía",
        symbol = "h",
        unit   = "kJ/kg",
        hint   = "Ej: 417 – 2675 kJ/kg sat."
    ),
    ENTROPY(
        label  = "Entropía",
        symbol = "s",
        unit   = "kJ/kg·K",
        hint   = "Ej: 1.30 – 7.36 kJ/kg·K sat."
    ),
    QUALITY(
        label  = "Calidad",
        symbol = "x",
        unit   = "",
        hint   = "0 (líq. sat.) – 1 (vap. sat.)"
    )
}

/**
 * Par de propiedades válido para el cálculo.
 * Solo incluye los pares que IF97 puede resolver.
 */
data class PropertyPair(val first: ThermoProperty, val second: ThermoProperty)

/**
 * Pares válidos de la Etapa 1 — resolubles directamente con IF97.
 * El orden importa: first siempre es la propiedad "principal".
 */
val validPairsStage1 = listOf(
    PropertyPair(ThermoProperty.PRESSURE,     ThermoProperty.TEMPERATURE),
    PropertyPair(ThermoProperty.PRESSURE,     ThermoProperty.ENTHALPY),
    PropertyPair(ThermoProperty.PRESSURE,     ThermoProperty.ENTROPY),
    PropertyPair(ThermoProperty.PRESSURE,     ThermoProperty.SPECIFIC_VOLUME),
    PropertyPair(ThermoProperty.PRESSURE,     ThermoProperty.QUALITY),
    PropertyPair(ThermoProperty.TEMPERATURE,  ThermoProperty.QUALITY)
)