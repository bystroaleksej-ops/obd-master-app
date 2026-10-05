package com.obdmaster.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    data object Charts : Screen("charts", "Графики", Icons.Default.ShowChart)
    data object Terminal : Screen("terminal", "Терминал", Icons.Default.Terminal)
    data object Diagnostics : Screen("diagnostics", "Ошибки", Icons.Default.Warning)
    data object Settings : Screen("settings", "Настройки", Icons.Default.Settings)
}

@Composable
fun AppNavigation(viewModel: ObdViewModel) {
    val navController = rememberNavController()

    // 5 согласованных вкладок в точном порядке пользователя
    val items = listOf(
        Screen.Dashboard,
        Screen.Charts,
        Screen.Terminal,
        Screen.Diagnostics,
        Screen.Settings
    )

    val activeAlarm by viewModel.activeAlarm.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    // Восстановление последней активной вкладки
    val initialRoute = remember {
        val saved = appSettings.lastActiveScreenRoute
        if (saved in listOf("dashboard", "charts", "terminal", "diagnostics", "settings")) saved else "dashboard"
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: initialRoute

                LaunchedEffect(currentRoute) {
                    viewModel.onScreenChanged(currentRoute)
                }

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
                                viewModel.onScreenChanged(screen.route)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Красный мигающий баннер тревоги при превышении порогов
            AnimatedVisibility(
                visible = activeAlarm != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RedError),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.dismissActiveAlarm() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = activeAlarm ?: "",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✕",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = initialRoute,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(viewModel = viewModel)
                }
                composable(Screen.Charts.route) {
                    LiveChartsScreen(viewModel = viewModel)
                }
                composable(Screen.Terminal.route) {
                    TerminalScreen(viewModel = viewModel)
                }
                composable(Screen.Diagnostics.route) {
                    DiagnosticsScreen(viewModel = viewModel)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
