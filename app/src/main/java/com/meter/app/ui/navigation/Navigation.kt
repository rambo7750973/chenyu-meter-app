package com.meter.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.meter.app.ui.billing.BillingScreen
import com.meter.app.ui.home.HomeScreen
import com.meter.app.ui.meter.MeterDetailScreen
import com.meter.app.ui.settings.SettingsScreen
import com.meter.app.ui.wifi.WiFiConfigScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "首页", Icons.Default.Home)
    object Billing : Screen("billing", "账单", Icons.Default.Receipt)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Billing,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onMeterClick = { meterId ->
                        navController.navigate("meter/$meterId")
                    }
                )
            }

            composable(
                route = "meter/{meterId}",
                arguments = listOf(navArgument("meterId") { type = NavType.StringType })
            ) { backStackEntry ->
                val meterId = backStackEntry.arguments?.getString("meterId") ?: ""
                MeterDetailScreen(
                    meterId = meterId,
                    onBackClick = { navController.popBackStack() },
                    onWiFiConfig = { id ->
                        navController.navigate("wifi/$id")
                    }
                )
            }

            composable(
                route = "wifi/{meterId}",
                arguments = listOf(navArgument("meterId") { type = NavType.StringType })
            ) { backStackEntry ->
                val meterId = backStackEntry.arguments?.getString("meterId") ?: ""
                WiFiConfigScreen(
                    meterId = meterId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Billing.route) {
                BillingScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}