package com.thermoapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.dp
import com.thermoapp.ChartPoint
import com.thermoapp.ThermoViewModel
import com.thermoapp.WaterEntry
import com.thermoapp.WaterIF97Repository
import kotlin.math.log10

@Composable
fun ChartScreen(viewModel: ThermoViewModel, modifier: Modifier = Modifier) {
    var showPv by remember { mutableStateOf(true) }
    val chartPoints by viewModel.chartPoints.collectAsState()

    // Estado del zoom y desplazamiento
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // transformableState: maneja pinch y drag simultáneamente
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        // Limitar zoom entre 1x y 10x
        scale = (scale * zoomChange).coerceIn(1f, 10f)
        // Actualizar desplazamiento según el arrastre
        offsetX += panChange.x
        offsetY += panChange.y
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Barra superior con controles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Diagrama:", style = MaterialTheme.typography.bodyMedium)
            FilterChip(selected = showPv,  onClick = { showPv = true },  label = { Text("P-v") })
            FilterChip(selected = !showPv, onClick = { showPv = false }, label = { Text("T-v") })

            Spacer(Modifier.weight(1f))

            // Indicador de zoom — solo visible cuando zoom > 1
            if (scale > 1.05f) {
                Text(
                    "%.1fx".format(scale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (chartPoints.isNotEmpty()) {
                Text(
                    "${chartPoints.size} punto(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Botón reset zoom
            IconButton(
                onClick = {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                },
                enabled = scale > 1.05f || offsetX != 0f || offsetY != 0f
            ) {
                Icon(
                    Icons.Default.FitScreen,
                    contentDescription = "Restablecer vista",
                    tint = if (scale > 1.05f) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SaturationChart(
            repo             = viewModel.getRepo(),
            showPv           = showPv,
            points           = chartPoints,
            scale            = scale,
            offsetX          = offsetX,
            offsetY          = offsetY,
            transformableState = transformableState,
            modifier         = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SaturationChart(
    repo: WaterIF97Repository,
    showPv: Boolean,
    points: List<ChartPoint>,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    transformableState: TransformableState,
    modifier: Modifier = Modifier
) {
    val textMeasurer  = rememberTextMeasurer()
    val colorScheme   = MaterialTheme.colorScheme
    val curveColor    = colorScheme.primary
    val axisColor     = colorScheme.onSurface
    val labelColor    = colorScheme.onSurfaceVariant
    val criticalColor = colorScheme.error
    val pointColors   = listOf(
        colorScheme.tertiary,
        colorScheme.error,
        colorScheme.secondary,
        colorScheme.primary,
        colorScheme.inversePrimary
    )

    Canvas(
        modifier = modifier.transformable(state = transformableState)
    ) {
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

        // Funciones base de transformación (sin zoom)
        fun xPxBase(v: Double) =
            (marginLeft + (log10(v) - vMin) / (vMax - vMin) * chartWidth).toFloat()
        fun yPxBase(y: Double) =
            (marginTop + (1.0 - (y - yMin) / (yMax - yMin)) * chartHeight).toFloat()

        // Centro del área de la gráfica — punto de anclaje del zoom
        val centerX = marginLeft + chartWidth / 2f
        val centerY = marginTop + chartHeight / 2f

        // Función con zoom aplicado
        // El zoom escala desde el centro y desplaza según el offset
        fun xPx(v: Double): Float {
            val base = xPxBase(v)
            return centerX + (base - centerX) * scale + offsetX
        }
        fun yPx(y: Double): Float {
            val base = yPxBase(y)
            return centerY + (base - centerY) * scale + offsetY
        }

        // Dibujar ejes (sin transformar — siempre fijos)
        drawLine(axisColor, Offset(marginLeft, marginTop),
            Offset(marginLeft, marginTop + chartHeight), strokeWidth = 1.5f)
        drawLine(axisColor, Offset(marginLeft, marginTop + chartHeight),
            Offset(marginLeft + chartWidth, marginTop + chartHeight), strokeWidth = 1.5f)

        // Etiquetas de ejes (sin transformar)
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
            yMax         = yMax,
            scale        = scale,
            offsetX      = offsetX,
            offsetY      = offsetY,
            centerX      = centerX,
            centerY      = centerY
        )

        // Curvas de saturación (con zoom)
        drawSaturationCurve(
            points = table.map { Offset(xPx(it.vf), yPx(if (showPv) it.pressure else it.temperature)) },
            color  = curveColor
        )
        drawSaturationCurve(
            points = table.map { Offset(xPx(it.vg), yPx(if (showPv) it.pressure else it.temperature)) },
            color  = curveColor
        )

        // Punto crítico (con zoom)
        val critical = table.last()
        val cpX = xPx(critical.vf)
        val cpY = yPx(if (showPv) critical.pressure else critical.temperature)
        drawCircle(color = criticalColor, radius = 6f * scale.coerceAtMost(2f),
            center = Offset(cpX, cpY))
        val cpMeasured = textMeasurer.measure(
            "Punto crítico",
            TextStyle(fontSize = 10.sp, color = criticalColor)
        )
        drawText(cpMeasured, topLeft = Offset(cpX - cpMeasured.size.width / 2f, cpY - 20f))

        // Puntos de estado calculados (con zoom)
        points.forEachIndexed { index, point ->
            val color = pointColors[index % pointColors.size]
            val yVal  = if (showPv) point.pressure else point.temperature
            val px = xPx(point.vf)
            val py = yPx(yVal)

            val radius = 18f * scale.coerceAtMost(2f)
            drawCircle(color = color, radius = radius,
                center = Offset(px, py), style = Stroke(width = 2f))
            drawCircle(color = color.copy(alpha = 0.3f), radius = radius - 2f,
                center = Offset(px, py))

            val labelMeasured = textMeasurer.measure(
                point.label,
                TextStyle(fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
            )
            val labelY = if (py > marginTop + 30f) py - 24f else py + 14f
            drawText(labelMeasured,
                topLeft = Offset(px - labelMeasured.size.width / 2f, labelY))
        }
    }
}

fun DrawScope.drawSaturationCurve(points: List<Offset>, color: Color) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }
    drawPath(path, color = color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
}

fun DrawScope.drawAxisLabels(
    textMeasurer: TextMeasurer,
    table: List<WaterEntry>,
    showPv: Boolean,
    marginLeft: Float, marginTop: Float,
    chartWidth: Float, chartHeight: Float,
    axisColor: Color, labelColor: Color,
    vMin: Double, vMax: Double,
    yMin: Double, yMax: Double,
    scale: Float,
    offsetX: Float, offsetY: Float,
    centerX: Float, centerY: Float
) {
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)
    val titleStyle = TextStyle(fontSize = 11.sp, color = axisColor, fontWeight = FontWeight.Medium)

    // Calcular el rango visible actual en coordenadas de datos
    // Invertimos la transformación: px = center + (base - center) * scale + offset
    // → base = (px - center - offset) / scale + center
    // Para el borde izquierdo del área (px = marginLeft):
    fun pxToLogV(px: Float): Double {
        val base = (px - centerX - offsetX) / scale + centerX
        val fraction = (base - marginLeft) / chartWidth
        return vMin + fraction * (vMax - vMin)
    }
    fun pxToY(py: Float): Double {
        val base = (py - centerY - offsetY) / scale + centerY
        val fraction = (base - marginTop) / chartHeight
        return yMin + (1.0 - fraction) * (yMax - yMin)
    }

    // Rango visible actual
    val visibleLogVMin = pxToLogV(marginLeft)
    val visibleLogVMax = pxToLogV(marginLeft + chartWidth)
    val visibleYMax = pxToY(marginTop)
    val visibleYMin = pxToY(marginTop + chartHeight)

    // --- Marcas eje X dinámicas ---
    // Todos los ticks posibles en escala log
    val allXTicks = listOf(
        0.0001, 0.0002, 0.0005,
        0.001, 0.002, 0.005,
        0.01, 0.02, 0.05,
        0.1, 0.2, 0.5,
        1.0, 2.0, 5.0,
        10.0, 20.0, 50.0, 100.0
    )

    // Filtrar solo los ticks visibles en el rango actual
    val visibleXTicks = allXTicks.filter { v ->
        val logV = log10(v)
        logV >= visibleLogVMin && logV <= visibleLogVMax
    }

    // Si hay demasiados ticks visibles, mostrar solo cada N
    val step = when {
        visibleXTicks.size > 8 -> 3
        visibleXTicks.size > 5 -> 2
        else -> 1
    }

    visibleXTicks.filterIndexed { i, _ -> i % step == 0 }.forEach { v ->
        val logV = log10(v)
        // Transformar de coordenada de dato a píxel con zoom
        val basePx = marginLeft + (logV - vMin) / (vMax - vMin) * chartWidth
        val xPx = centerX + (basePx.toFloat() - centerX) * scale + offsetX

        if (xPx < marginLeft || xPx > marginLeft + chartWidth) return@forEach

        val yBase = marginTop + chartHeight
        drawLine(axisColor, Offset(xPx, yBase), Offset(xPx, yBase + 5f), strokeWidth = 1f)

        val label = when {
            v < 0.001  -> "%.4f".format(v)
            v < 0.01   -> "%.3f".format(v)
            v < 0.1    -> "%.2f".format(v)
            v < 1.0    -> "%.1f".format(v)
            else       -> "%.0f".format(v)
        }
        val measured = textMeasurer.measure(label, labelStyle)
        drawText(measured, topLeft = Offset(xPx - measured.size.width / 2f, yBase + 8f))
    }

    // Título eje X
    val xTitle = textMeasurer.measure("v (m³/kg)", titleStyle)
    drawText(xTitle, topLeft = Offset(
        marginLeft + chartWidth / 2f - xTitle.size.width / 2f,
        marginTop + chartHeight + 36f
    ))

    // --- Marcas eje Y dinámicas ---
    // Calcular un intervalo "bonito" para el rango visible
    val yRange = visibleYMax - visibleYMin
    val rawStep = yRange / 5.0

    // Redondear el paso a un número bonito (1, 2, 5, 10, 20, 50, 100, 500, 1000...)
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(rawStep)))
    val niceStep = when {
        rawStep / magnitude < 1.5 -> magnitude
        rawStep / magnitude < 3.5 -> 2 * magnitude
        rawStep / magnitude < 7.5 -> 5 * magnitude
        else -> 10 * magnitude
    }

    // Primer tick por encima del mínimo visible
    val firstTick = Math.ceil(visibleYMin / niceStep) * niceStep

    // Generar hasta 8 ticks dentro del rango visible
    var tick = firstTick
    while (tick <= visibleYMax) {
        val basePy = marginTop + (1.0 - (tick - yMin) / (yMax - yMin)) * chartHeight
        val yPx = centerY + (basePy.toFloat() - centerY) * scale + offsetY

        if (yPx >= marginTop && yPx <= marginTop + chartHeight) {
            drawLine(axisColor, Offset(marginLeft - 5f, yPx),
                Offset(marginLeft, yPx), strokeWidth = 1f)

            // Formato inteligente según magnitud
            val label = when {
                niceStep >= 1000 -> "%.0f".format(tick)
                niceStep >= 100  -> "%.0f".format(tick)
                niceStep >= 10   -> "%.0f".format(tick)
                niceStep >= 1    -> "%.0f".format(tick)
                else             -> "%.2f".format(tick)
            }
            val measured = textMeasurer.measure(label, labelStyle)
            drawText(measured, topLeft = Offset(
                marginLeft - measured.size.width - 8f,
                yPx - measured.size.height / 2f
            ))
        }
        tick += niceStep
    }

    // Título eje Y
    val yTitle = textMeasurer.measure(if (showPv) "P (kPa)" else "T (°C)", titleStyle)
    drawText(yTitle, topLeft = Offset(
        4f,
        marginTop + chartHeight / 2f - yTitle.size.height / 2f
    ))
}