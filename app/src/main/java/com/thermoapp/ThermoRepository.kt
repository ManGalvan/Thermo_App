// ThermoRepository.kt
package com.thermoapp

import android.content.Context

class ThermoRepository(context: Context) {

    // La tabla completa cargada en memoria al iniciar
    private val table: List<WaterEntry> = loadCsv(context)

    /**
     * Lee el CSV desde assets y lo convierte en lista de WaterEntry.
     * context.assets.open() es la forma de acceder a la carpeta assets en Android.
     */
    private fun loadCsv(context: Context): List<WaterEntry> {
        val entries = mutableListOf<WaterEntry>()

        context.assets.open("Water_v2_0.csv").bufferedReader().useLines { lines ->
            lines.drop(1) // Saltar la línea de encabezados
                .filter { it.isNotBlank() }
                .forEach { line ->
                    val cols = line.split(",")
                    if (cols.size >= 8) {
                        entries.add(
                            WaterEntry(
                                pressure    = cols[0].toDoubleOrNull() ?: return@forEach,
                                temperature = cols[1].toDoubleOrNull() ?: return@forEach,
                                vf          = cols[2].toDoubleOrNull() ?: return@forEach,
                                vg          = cols[3].toDoubleOrNull() ?: return@forEach,
                                hf          = cols[4].toDoubleOrNull() ?: return@forEach,
                                hg          = cols[5].toDoubleOrNull() ?: return@forEach,
                                sf          = cols[6].toDoubleOrNull() ?: return@forEach,
                                sg          = cols[7].toDoubleOrNull() ?: return@forEach
                            )
                        )
                    }
                }
        }
        return entries
    }

    /**
     * Busca propiedades por PRESIÓN usando interpolación lineal.
     * Si el valor exacto no está en la tabla, calcula el resultado
     * proporcional entre las dos filas más cercanas.
     */
    fun getByPressure(p: Double): WaterEntry? {
        if (table.isEmpty()) return null
        val min = table.first().pressure
        val max = table.last().pressure
        if (p < min || p > max) return null

        // Encontrar las dos filas que rodean el valor buscado
        val lower = table.lastOrNull { it.pressure <= p } ?: return table.first()
        val upper = table.firstOrNull { it.pressure >= p } ?: return table.last()

        // Si coincide exactamente con una fila, devolverla directo
        if (lower.pressure == upper.pressure) return lower

        // Interpolación lineal: fracción de dónde está p entre lower y upper
        val fraction = (p - lower.pressure) / (upper.pressure - lower.pressure)

        return WaterEntry(
            pressure    = p,
            temperature = interpolate(lower.temperature, upper.temperature, fraction),
            vf          = interpolate(lower.vf, upper.vf, fraction),
            vg          = interpolate(lower.vg, upper.vg, fraction),
            hf          = interpolate(lower.hf, upper.hf, fraction),
            hg          = interpolate(lower.hg, upper.hg, fraction),
            sf          = interpolate(lower.sf, upper.sf, fraction),
            sg          = interpolate(lower.sg, upper.sg, fraction)
        )
    }

    // Mismo método pero buscando por TEMPERATURA de saturación
    fun getByTemperature(t: Double): WaterEntry? {
        if (table.isEmpty()) return null
        val sorted = table.sortedBy { it.temperature }
        if (t < sorted.first().temperature || t > sorted.last().temperature) return null

        val lower = sorted.lastOrNull { it.temperature <= t } ?: return sorted.first()
        val upper = sorted.firstOrNull { it.temperature >= t } ?: return sorted.last()

        if (lower.temperature == upper.temperature) return lower

        val fraction = (t - lower.temperature) / (upper.temperature - lower.temperature)

        return WaterEntry(
            pressure    = interpolate(lower.pressure, upper.pressure, fraction),
            temperature = t,
            vf          = interpolate(lower.vf, upper.vf, fraction),
            vg          = interpolate(lower.vg, upper.vg, fraction),
            hf          = interpolate(lower.hf, upper.hf, fraction),
            hg          = interpolate(lower.hg, upper.hg, fraction),
            sf          = interpolate(lower.sf, upper.sf, fraction),
            sg          = interpolate(lower.sg, upper.sg, fraction)
        )
    }

    /** Fórmula de interpolación lineal
     *  resultado = a + (b - a) * fracción */
    private fun interpolate(a: Double, b: Double, fraction: Double): Double {
        return a + (b - a) * fraction
    }

    fun getPressureRange() = table.first().pressure to table.last().pressure
    fun getTemperatureRange() = table.minOf { it.temperature } to table.maxOf { it.temperature }

    // Necesario para que ChartScreen acceda a los datos directamente
    fun getTable(): List<WaterEntry> = table

    fun getState(entry: WaterEntry, h: Double?): ThermoResult {
        if (h == null) {
            return ThermoResult(entry = entry, phase = Phase.SATURATED)
        }

        return when {
            h < entry.hf -> ThermoResult(entry = entry, phase = Phase.LIQUID, hInput = h)
            h > entry.hg -> ThermoResult(entry = entry, phase = Phase.VAPOR, hInput = h)
            else -> {
                val x = (h - entry.hf) / (entry.hg - entry.hf)
                ThermoResult(entry = entry, phase = Phase.MIXTURE, quality = x, hInput = h)
            }
        }
    }
}