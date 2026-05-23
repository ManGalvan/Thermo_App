package com.thermoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thermoapp.ui.navigation.AppScreen
import com.thermoapp.ui.navigation.appScreens
import com.thermoapp.ui.screens.CalcScreen
import com.thermoapp.ui.screens.ChartScreen
import com.thermoapp.ui.screens.InfoScreen
import com.thermoapp.ui.theme.ThermoAppTheme
import androidx.compose.runtime.Composable


class MainActivity : ComponentActivity() {

    private val repo by lazy { WaterIF97Repository() }

    // viewModels() le pide a Android que cree/recupere el ViewModel
    // usando nuestro Factory — garantiza que solo exista una instancia
    private val viewModel: ThermoViewModel by viewModels {
        ThermoViewModelFactory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThermoAppTheme {
                ThermoApp(viewModel)
            }
        }
    }
}

@Composable
fun ThermoApp(viewModel: ThermoViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                appScreens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppScreen.Calc.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppScreen.Calc.route)  { CalcScreen(viewModel) }
            composable(AppScreen.Chart.route) { ChartScreen(viewModel) }
            composable(AppScreen.Info.route)  { InfoScreen() }
        }
    }
}