package com.alok.justrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alok.justrack.ui.components.neumorphicShadow
import com.alok.justrack.ui.navigation.NavGraph
import com.alok.justrack.ui.navigation.Screen
import com.alok.justrack.ui.theme.Background
import com.alok.justrack.ui.theme.JusTrackTheme
import com.alok.justrack.ui.theme.TextPrimary
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JusTrackTheme {
                MainScaffold()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val showBackIcon = currentDestination?.route?.startsWith("detail") == true

    val items = listOf(
        Screen.Watchlist to Icons.Rounded.PlaylistPlay,
        Screen.Lists to Icons.Rounded.List,
        Screen.Search to Icons.Rounded.Search,
        Screen.Stats to Icons.Rounded.BarChart,
        Screen.Profile to Icons.Rounded.Person
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("JusTrack", color = TextPrimary) },
                navigationIcon = {
                    if (showBackIcon) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Background
                ),
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextPrimary)
                    }
                },
                modifier = Modifier.neumorphicShadow(offset = 2.dp, cornerRadius = 0.dp)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Background,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(80.dp)
                    .neumorphicShadow(offset = (-2).dp, cornerRadius = 0.dp)
            ) {
                items.forEach { (screen, icon) ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = TextPrimary.copy(alpha = 0.6f),
                            indicatorColor = Background
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Background)) {
            NavGraph(navController = navController)
        }
    }
}
