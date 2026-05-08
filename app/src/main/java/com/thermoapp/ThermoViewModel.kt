package com.thermoapp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThermoViewModel(private val repo: ThermoRepository) : ViewModel() {

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
                vf = entry.vf,
                vg = entry.vg,
                pressure = entry.pressure,
                temperature = entry.temperature,
                label = "P=%.0f kPa".format(entry.pressure)
            )
            // Máximo 5 puntos en la gráfica — elimina el más viejo si se supera
            val updated = (_chartPoints.value + point).takeLast(5)
            _chartPoints.value = updated
        }
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
    val pressure: Double,
    val temperature: Double,
    val label: String
)