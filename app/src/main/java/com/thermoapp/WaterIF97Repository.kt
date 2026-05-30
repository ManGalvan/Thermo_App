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

    /**
     * Calcula propiedades dada una presión en kPa y calidad x (0-1).
     * x=0 → líquido saturado, x=1 → vapor saturado, 0<x<1 → mezcla
     */
    fun getByPressureAndQuality(pressureKPa: Double, x: Double): ThermoResult? {
        if (pressureKPa < pressureRangeKPa.first ||
            pressureKPa > pressureRangeKPa.second) return null
        if (x < 0.0 || x > 1.0) return null

        return try {
            val entry = getByPressure(pressureKPa) ?: return null
            ThermoResult(
                entry   = entry,
                phase   = Phase.MIXTURE,
                quality = x,
                hInput  = entry.hf + x * (entry.hg - entry.hf)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Determina la región de estado dada una presión en kPa y temperatura en °C.
     * Compara T ingresada con T_sat a esa presión para identificar la fase.
     */
    fun getByPressureAndTemperature(pressureKPa: Double, tempC: Double): ThermoResult? {
        if (pressureKPa < pressureRangeKPa.first ||
            pressureKPa > pressureRangeKPa.second) return null

        return try {
            val P     = pressureKPa * 1000.0
            val T     = tempC + 273.15
            val entry = getByPressure(pressureKPa) ?: return null
            val tSat  = entry.temperature

            val phase = when {
                tempC < tSat - 0.01 -> Phase.LIQUID
                tempC > tSat + 0.01 -> Phase.VAPOR
                else                -> Phase.MIXTURE
            }

            // Propiedades reales a esa P y T
            val vReal = if97.specificVolumePT(P, T)
            val hReal = if97.specificEnthalpyPT(P, T) / 1000.0  // J/kg → kJ/kg
            val sReal = if97.specificEntropyPT(P, T) / 1000.0   // J/kg·K → kJ/kg·K

            ThermoResult(
                entry = entry,
                phase = phase,
                vReal = vReal,
                hReal = hReal,
                sReal = sReal
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calcula propiedades dada una presión en kPa y volumen específico en m³/kg.
     * Determina la región comparando v con vf y vg de saturación.
     * Para líquido y vapor, encuentra T por iteración numérica.
     */
    fun getByPressureAndVolume(pressureKPa: Double, v: Double): ThermoResult? {
        if (pressureKPa < pressureRangeKPa.first ||
            pressureKPa > pressureRangeKPa.second) return null
        if (v <= 0.0) return null

        return try {
            val P     = pressureKPa * 1000.0
            val entry = getByPressure(pressureKPa) ?: return null

            when {
                // --- Mezcla líquido-vapor ---
                v >= entry.vf && v <= entry.vg -> {
                    val x = (v - entry.vf) / (entry.vg - entry.vf)
                    val h = entry.hf + x * (entry.hg - entry.hf)
                    val s = entry.sf + x * (entry.sg - entry.sf)
                    ThermoResult(
                        entry   = entry,
                        phase   = Phase.MIXTURE,
                        quality = x,
                        hInput  = h,
                        vReal   = v,
                        hReal   = h,
                        sReal   = s
                    )
                }

                // --- Líquido comprimido ---
                v < entry.vf -> {
                    // Buscar T por bisección: encontrar T tal que v(P,T) = v ingresado
                    val tK = findTemperatureByVolume(P, v, 273.16, entry.temperature + 273.15)
                        ?: return null
                    val hReal = if97.specificEnthalpyPT(P, tK) / 1000.0
                    val sReal = if97.specificEntropyPT(P, tK) / 1000.0
                    ThermoResult(
                        entry = entry,
                        phase = Phase.LIQUID,
                        vReal = v,
                        hReal = hReal,
                        sReal = sReal
                    )
                }

                // --- Vapor sobrecalentado ---
                else -> {
                    // Buscar T por bisección en rango de vapor
                    val tK = findTemperatureByVolume(P, v, entry.temperature + 273.15, 2273.15)
                        ?: return null
                    val hReal = if97.specificEnthalpyPT(P, tK) / 1000.0
                    val sReal = if97.specificEntropyPT(P, tK) / 1000.0
                    ThermoResult(
                        entry = entry,
                        phase = Phase.VAPOR,
                        vReal = v,
                        hReal = hReal,
                        sReal = sReal
                    )
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Encuentra la temperatura T (en Kelvin) tal que v(P, T) = vTarget
     * usando el método de bisección — divide el intervalo a la mitad
     * en cada iteración hasta converger al valor buscado.
     */
    private fun findTemperatureByVolume(
        P: Double,
        vTarget: Double,
        tMin: Double,
        tMax: Double,
        maxIter: Int = 100,
        tolerance: Double = 1e-9
    ): Double? {
        var lo = tMin
        var hi = tMax

        repeat(maxIter) {
            val mid = (lo + hi) / 2.0
            val vMid = try {
                if97.specificVolumePT(P, mid)
            } catch (e: Exception) {
                return null
            }

            if (Math.abs(vMid - vTarget) < tolerance) return mid

            // En líquido: v aumenta con T
            // En vapor: v también aumenta con T
            // Por lo tanto si vMid < vTarget, necesitamos T más alta
            if (vMid < vTarget) lo = mid else hi = mid
        }

        return (lo + hi) / 2.0
    }
}