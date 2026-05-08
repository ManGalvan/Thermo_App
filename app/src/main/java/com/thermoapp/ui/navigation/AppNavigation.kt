package com.thermoapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

// Sealed class: define las rutas posibles de la app.
// Es como un enum pero más poderoso — cada ruta tiene su propio ícono y etiqueta.
// "sealed" significa que solo pueden existir los casos que definimos aquí.
sealed class AppScreen(
    val route: String,          // identificador único de la pantalla
    val label: String,          // texto que aparece en el tab
    val icon: ImageVector       // ícono del tab
) {
    object Calc : AppScreen(
        route = "calc",
        label = "Calcular",
        icon = Icons.Default.Calculate
    )
    object Chart : AppScreen(
        route = "chart",
        label = "Diagrama",
        icon = Icons.Default.ShowChart
    )
    object Info : AppScreen(
        route = "info",
        label = "Ayuda",
        icon = Icons.Default.Info
    )
}

// Lista ordenada de tabs para construir la barra de navegación
val appScreens = listOf(AppScreen.Calc, AppScreen.Chart, AppScreen.Info)