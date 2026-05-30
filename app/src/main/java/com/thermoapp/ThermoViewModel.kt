package com.thermoapp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThermoViewModel(private val repo: WaterIF97Repository) : ViewModel() {

    // StateFlow: como mutableStateOf pero diseñado para ViewModels.
    // Cualquier composable que lo observe se redibuja cuando cambia.
    private val _result = MutableStateFlow<ThermoResult?>(null)
    val result: StateFlow<ThermoResult?> = _result.asStateFlow()

    // Lista de puntos calculados para mostrar en la gráfica
    private val _chartPoints = MutableStateFlow<List<ChartPoint>>(emptyList())
    val chartPoints: StateFlow<List<ChartPoint>> = _chartPoints.asStateFlow()

    fun calculate(input: Double, byPressure: Boolean, h: Double?) {
        val entry = if (byPressure) repo.getByPressure(input)
        else repo.getByTemperature(input)
        if (entry != null) {
            val thermoResult = repo.getState(entry, h)
            _result.value = thermoResult

            // Agregar punto a la gráfica
            val point = ChartPoint(
                vf          = entry.vf,
                vg          = entry.vg,
                v           = entry.vf,  // en saturación el punto va sobre la curva izquierda
                pressure    = entry.pressure,
                temperature = entry.temperature,
                label       = "P=%.0f kPa".format(entry.pressure)
            )
            // Máximo 5 puntos en la gráfica — elimina el más viejo si se supera
            val updated = (_chartPoints.value + point).takeLast(5)
            _chartPoints.value = updated
        }
    }

    fun calculateByPressureAndQuality(pressureKPa: Double, x: Double) {
        val result = repo.getByPressureAndQuality(pressureKPa, x) ?: return
        _result.value = result

        val entry = result.entry
        val x = result.quality ?: 0.0
        val point = ChartPoint(
            vf          = entry.vf,
            vg          = entry.vg,
            v           = entry.vf + x * (entry.vg - entry.vf), // ← posición real dentro de la campana
            pressure    = entry.pressure,
            temperature = entry.temperature,
            label       = "P=%.0f, x=%.2f".format(entry.pressure, x)
        )
        val updated = (_chartPoints.value + point).takeLast(5)
        _chartPoints.value = updated
    }

    fun calculateByPressureAndTemperature(pressureKPa: Double, tempC: Double) {
        val result = repo.getByPressureAndTemperature(pressureKPa, tempC) ?: return
        _result.value = result

        val entry = result.entry

        // Usar vReal si está disponible, de lo contrario fallback según fase
        val v = result.vReal ?: when (result.phase) {
            Phase.LIQUID    -> entry.vf * 0.95
            Phase.VAPOR     -> entry.vg * 1.05
            else            -> entry.vf
        }

        val point = ChartPoint(
            vf          = entry.vf,
            vg          = entry.vg,
            v           = v,
            pressure    = entry.pressure,
            temperature = tempC,
            label       = "P=%.0f, T=%.1f°C".format(pressureKPa, tempC)
        )
        val updated = (_chartPoints.value + point).takeLast(5)
        _chartPoints.value = updated
    }

    fun calculateByPressureAndVolume(pressureKPa: Double, v: Double) {
        val result = repo.getByPressureAndVolume(pressureKPa, v) ?: return
        _result.value = result

        val entry = result.entry
        val point = ChartPoint(
            vf          = entry.vf,
            vg          = entry.vg,
            v           = v,  // posición exacta en el eje X
            pressure    = entry.pressure,
            temperature = entry.temperature,
            label       = "P=%.0f, v=%.4f".format(pressureKPa, v)
        )
        val updated = (_chartPoints.value + point).takeLast(5)
        _chartPoints.value = updated
    }

    fun clearPoints() {
        _chartPoints.value = emptyList()
        _result.value = null
    }

    fun getRepo() = repo
}

/** Representa un punto de estado para dibujar sobre la campana */
data class ChartPoint(
    val vf: Double,
    val vg: Double,
    val v: Double,          // ← volumen específico real del estado
    val pressure: Double,
    val temperature: Double,
    val label: String
)