package com.obdmaster.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.obdmaster.app.ui.screens.*
import com.obdmaster.app.ui.theme.*
import com.obdmaster.app.ui.viewmodel.ObdViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Приборы", Icons.Default.Speed)
    data object Diagnostics : Screen("diagnostics", "Ошибки", Icons.Default.Warning)
    data object Charts : Screen("charts", "Графики", Icons.Default.ShowChart)
    data object Terminal : Screen("terminal", "Терминал", Icons.Default.Terminal)
    data object Connection : Screen("connection", "Связь", Icons.Default.Bluetooth)
    data object Settings : Screen("settings", "Настройки", Icons.Default.Settings)
}

@Composable
fun AppNavigation(viewModel: ObdViewModel) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Dashboard,
        Screen.Diagnostics,
        Screen.Charts,
        Screen.Terminal,
        Screen.Connection,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, fontSize = 10.sp) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkBackground,
                            selectedTextColor = CyanAccent,
                            indicatorColor = CyanAccent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel = viewModel)
            }
            composable(Screen.Diagnostics.route) {
                DiagnosticsScreen(viewModel = viewModel)
            }
            composable(Screen.Charts.route) {
                LiveChartsScreen(viewModel = viewModel)
            }
            composable(Screen.Terminal.route) {
                TerminalScreen(viewModel = viewModel)
            }
            composable(Screen.Connection.route) {
                ConnectionScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
