package com.thermoapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.dp
import com.thermoapp.ChartPoint
import com.thermoapp.ThermoViewModel
import com.thermoapp.WaterEntry
import kotlin.math.log10

@Composable
fun ChartScreen(viewModel: ThermoViewModel, modifier: Modifier = Modifier) {
    var showPv by remember { mutableStateOf(true) }
    val chartPoints by viewModel.chartPoints.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Diagrama de saturación — Agua",
            style = MaterialTheme.typography.headlineSmall)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Diagrama:", style = MaterialTheme.typography.bodyMedium)
            FilterChip(selected = showPv,  onClick = { showPv = true },  label = { Text("P-v") })
            FilterChip(selected = !showPv, onClick = { showPv = false }, label = { Text("T-v") })

            // Indicador de cuántos puntos hay graficados
            if (chartPoints.isNotEmpty()) {
                Spacer(Modifier.weight(1f))
                Text(
                    "${chartPoints.size} punto(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        SaturationChart(
            repo     = viewModel.getRepo(),
            showPv   = showPv,
            points   = chartPoints,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SaturationChart(
    repo: com.thermoapp.ThermoRepository,
    showPv: Boolean,
    points: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    val textMeasurer  = rememberTextMeasurer()
    val colorScheme   = MaterialTheme.colorScheme
    val curveColor    = colorScheme.primary
    val axisColor     = colorScheme.onSurface
    val labelColor    = colorScheme.onSurfaceVariant
    val criticalColor = colorScheme.error
    val pointColor    = colorScheme.tertiary

    // Colores distintos para hasta 5 puntos
    val pointColors = listOf(
        colorScheme.tertiary,
        colorScheme.error,
        colorScheme.secondary,
        colorScheme.primary,
        colorScheme.inversePrimary
    )

    Canvas(modifier = modifier) {
        val table = repo.getTable()
        if (table.isEmpty()) return@Canvas

        val marginLeft   = 72f
        val marginRight  = 24f
        val marginTop    = 24f
        val marginBottom = 56f
        val chartWidth   = size.width  - marginLeft - marginRight
        val chartHeight  = size.height - marginTop  - marginBottom

        val vMin = log10(table.minOf { it.vf })
        val vMax = log10(table.maxOf { it.vg })
        val yMin = if (showPv) table.minOf { it.pressure }    else table.minOf { it.temperature }
        val yMax = if (showPv) table.maxOf { it.pressure }    else table.maxOf { it.temperature }

        fun xPx(v: Double) =
            (marginLeft + (log10(v) - vMin) / (vMax - vMin) * chartWidth).toFloat()
        fun yPx(y: Double) =
            (marginTop + (1.0 - (y - yMin) / (yMax - yMin)) * chartHeight).toFloat()

        // Ejes
        drawLine(axisColor, Offset(marginLeft, marginTop),
            Offset(marginLeft, marginTop + chartHeight), strokeWidth = 1.5f)
        drawLine(axisColor, Offset(marginLeft, marginTop + chartHeight),
            Offset(marginLeft + chartWidth, marginTop + chartHeight), strokeWidth = 1.5f)

        // Curvas de saturación
        drawSaturationCurve(
            points = table.map { Offset(xPx(it.vf), yPx(if (showPv) it.pressure else it.temperature)) },
            color  = curveColor
        )
        drawSaturationCurve(
            points = table.map { Offset(xPx(it.vg), yPx(if (showPv) it.pressure else it.temperature)) },
            color  = curveColor
        )

        // Punto crítico
        val critical = table.last()
        val cpX = xPx(critical.vf)
        val cpY = yPx(if (showPv) critical.pressure else critical.temperature)
        drawCircle(color = criticalColor, radius = 6f, center = Offset(cpX, cpY))
        val cpMeasured = textMeasurer.measure(
            "Punto crítico",
            TextStyle(fontSize = 10.sp, color = criticalColor)
        )
        drawText(cpMeasured, topLeft = Offset(cpX - cpMeasured.size.width / 2f, cpY - 20f))

        // --- Puntos de estado calculados ---
        points.forEachIndexed { index, point ->
            val color = pointColors[index % pointColors.size]
            val yVal  = if (showPv) point.pressure else point.temperature

            // El punto se grafica sobre la curva de líquido saturado (vf)
            // ya que representa el estado de saturación buscado
            val px = xPx(point.vf)
            val py = yPx(yVal)

            // Círculo exterior (borde)
            drawCircle(color = color, radius = 10f, center = Offset(px, py),
                style = Stroke(width = 2f))
            // Círculo interior (relleno)
            drawCircle(color = color.copy(alpha = 0.3f), radius = 8f,
                center = Offset(px, py))

            // Etiqueta del punto
            val labelMeasured = textMeasurer.measure(
                point.label,
                TextStyle(fontSize = 9.sp, color = color, fontWeight = FontWeight.Medium)
            )
            // Posición de la etiqueta: arriba del punto si hay espacio, abajo si no
            val labelY = if (py > marginTop + 30f) py - 24f else py + 14f
            drawText(labelMeasured,
                topLeft = Offset(px - labelMeasured.size.width / 2f, labelY))
        }

        // Etiquetas de ejes
        drawAxisLabels(
            textMeasurer = textMeasurer,
            table        = table,
            showPv       = showPv,
            marginLeft   = marginLeft,
            marginTop    = marginTop,
            chartWidth   = chartWidth,
            chartHeight  = chartHeight,
            axisColor    = axisColor,
            labelColor   = labelColor,
            vMin         = vMin,
            vMax         = vMax,
            yMin         = yMin,
            yMax         = yMax
        )
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSaturationCurve(
    points: List<Offset>,
    color: Color
) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }
    drawPath(path, color = color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAxisLabels(
    textMeasurer: TextMeasurer,
    table: List<WaterEntry>,
    showPv: Boolean,
    marginLeft: Float, marginTop: Float,
    chartWidth: Float, chartHeight: Float,
    axisColor: Color, labelColor: Color,
    vMin: Double, vMax: Double,
    yMin: Double, yMax: Double
) {
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)
    val titleStyle = TextStyle(fontSize = 11.sp, color = axisColor, fontWeight = FontWeight.Medium)

    // Marcas eje X — escala logarítmica
    val xTicks = listOf(0.001, 0.01, 0.1, 1.0, 10.0, 100.0)
    xTicks.forEach { v ->
        val logV = log10(v)
        if (logV < vMin || logV > vMax) return@forEach
        val xPx = (marginLeft + (logV - vMin) / (vMax - vMin) * chartWidth).toFloat()
        val yBase = marginTop + chartHeight

        drawLine(axisColor, Offset(xPx, yBase), Offset(xPx, yBase + 5f), strokeWidth = 1f)

        val label = if (v < 1.0) "%.3f".format(v) else "%.0f".format(v)
        val measured = textMeasurer.measure(label, labelStyle)
        drawText(measured, topLeft = Offset(xPx - measured.size.width / 2f, yBase + 8f))
    }

    // Título eje X
    val xTitle = textMeasurer.measure("v (m³/kg)", titleStyle)
    drawText(xTitle, topLeft = Offset(
        marginLeft + chartWidth / 2f - xTitle.size.width / 2f,
        marginTop + chartHeight + 36f
    ))

    // Marcas eje Y — 5 divisiones
    for (i in 0..5) {
        val yVal = yMin + (yMax - yMin) * i / 5.0
        val yPx  = (marginTop + (1.0 - (yVal - yMin) / (yMax - yMin)) * chartHeight).toFloat()

        drawLine(axisColor, Offset(marginLeft - 5f, yPx),
            Offset(marginLeft, yPx), strokeWidth = 1f)

        val measured = textMeasurer.measure("%.0f".format(yVal), labelStyle)
        drawText(measured, topLeft = Offset(
            marginLeft - measured.size.width - 8f,
            yPx - measured.size.height / 2f
        ))
    }

    // Título eje Y
    val yTitle = textMeasurer.measure(if (showPv) "P (kPa)" else "T (°C)", titleStyle)
    drawText(yTitle, topLeft = Offset(
        4f,
        marginTop + chartHeight / 2f - yTitle.size.height / 2f
    ))
}