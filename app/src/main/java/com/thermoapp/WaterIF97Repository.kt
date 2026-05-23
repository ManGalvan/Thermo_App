package com.thermoapp

import com.hummeling.if97.IF97

/**
 * Repositorio de propiedades del agua usando la librería IF97.
 * Implementa las ecuaciones IAPWS-IF97 directamente — sin CSV,
 * sin interpolación, con precisión de referencia industrial.
 */
class WaterIF97Repository {

    // IF97.UnitSystem.SI usa: Pa, K, J/kg, J/kg·K
    // Nosotros convertimos a kPa, °C, kJ/kg, kJ/kg·K
    private val if97 = IF97(IF97.UnitSystem.SI)

    // Rangos válidos según IAPWS-IF97
    private val pressureRangeKPa = 0.611657 to 22064.0   // kPa
    private val temperatureRangeC = 0.01 to 373.946       // °C

    /**
     * Calcula propiedades de saturación dada una presión en kPa.
     * IF97 trabaja en Pa internamente — multiplicamos por 1000.
     */
    fun getByPressure(pressureKPa: Double): WaterEntry? {
        if (pressureKPa < pressureRangeKPa.first ||
            pressureKPa > pressureRangeKPa.second) return null

        return try {
            val P = pressureKPa * 1000.0 // kPa → Pa

            WaterEntry(
                pressure    = pressureKPa,
                temperature = if97.saturationTemperatureP(P) - 273.15, // K → °C
                vf          = if97.specificVolumeSaturatedLiquidP(P),   // m³/kg
                vg          = if97.specificVolumeSaturatedVapourP(P),   // m³/kg
                hf          = if97.specificEnthalpySaturatedLiquidP(P) / 1000.0, // J/kg → kJ/kg
                hg          = if97.specificEnthalpySaturatedVapourP(P) / 1000.0,
                sf          = if97.specificEntropySaturatedLiquidP(P) / 1000.0,  // J/kg·K → kJ/kg·K
                sg          = if97.specificEntropySaturatedVapourP(P) / 1000.0
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calcula propiedades de saturación dada una temperatura en °C.
     * IF97 trabaja en Kelvin internamente — sumamos 273.15.
     */
    fun getByTemperature(tempC: Double): WaterEntry? {
        if (tempC < temperatureRangeC.first ||
            tempC > temperatureRangeC.second) return null

        return try {
            val T = tempC + 273.15 // °C → K

            WaterEntry(
                pressure    = if97.saturationPressureT(T) / 1000.0, // Pa → kPa
                temperature = tempC,
                vf          = if97.specificVolumeSaturatedLiquidT(T),
                vg          = if97.specificVolumeSaturatedVapourT(T),
                hf          = if97.specificEnthalpySaturatedLiquidT(T) / 1000.0,
                hg          = if97.specificEnthalpySaturatedVapourT(T) / 1000.0,
                sf          = if97.specificEntropySaturatedLiquidT(T) / 1000.0,
                sg          = if97.specificEntropySaturatedVapourT(T) / 1000.0
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getState(entry: WaterEntry, h: Double?): ThermoResult {
        if (h == null) return ThermoResult(entry = entry, phase = Phase.SATURATED)
        return when {
            h < entry.hf -> ThermoResult(entry = entry, phase = Phase.LIQUID, hInput = h)
            h > entry.hg -> ThermoResult(entry = entry, phase = Phase.VAPOR, hInput = h)
            else -> {
                val x = (h - entry.hf) / (entry.hg - entry.hf)
                ThermoResult(entry = entry, phase = Phase.MIXTURE, quality = x, hInput = h)
            }
        }
    }

    /**
     * Genera una tabla de puntos para la gráfica de la campana.
     * IF97 calcula cada punto directamente — no necesita CSV.
     */
    fun getTable(): List<WaterEntry> {
        val points = mutableListOf<WaterEntry>()
        // Generamos 200 puntos distribuidos uniformemente en el rango de temperatura
        val tMin = temperatureRangeC.first
        val tMax = temperatureRangeC.second
        val steps = 200

        for (i in 0..steps) {
            val t = tMin + (tMax - tMin) * i / steps
            getByTemperature(t)?.let { points.add(it) }
        }
        return points
    }

    fun getPressureRange() = pressureRangeKPa
    fun getTemperatureRange() = temperatureRangeC
}