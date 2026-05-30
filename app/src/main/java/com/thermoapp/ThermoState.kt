package com.thermoapp

/**
 * Estado termodinámico completo del agua.
 * Todas las propiedades en unidades SI de ingeniería.
 */
data class ThermoState(
    val pressure: Double,       // kPa
    val temperature: Double,    // °C
    val specificVolume: Double, // m³/kg
    val enthalpy: Double,       // kJ/kg
    val entropy: Double,        // kJ/kg·K
    val quality: Double?,       // 0-1, null si no aplica
    val phase: Phase,
    // Propiedades de saturación a esa presión
    val satEntry: WaterEntry?   // null si está fuera del rango de saturación
)