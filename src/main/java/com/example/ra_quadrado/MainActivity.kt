package com.example.ra_quadrado

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ra_quadrado.navigation.AppRoutes
import com.example.ra_quadrado.screens.AttractionDetailScreen
import com.example.ra_quadrado.screens.HomeScreen
import com.example.ra_quadrado.ui.theme.RaQuadradoTheme
import com.example.ra_quadrado.viewmodels.AttractionsViewModel
import com.example.ra_quadrado.viewmodels.LocationViewModel

class MainActivity : ComponentActivity() {

    private val locationViewModel: LocationViewModel by viewModels()
    private val attractionsViewModel: AttractionsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RaQuadradoTheme {
                val navController = rememberNavController()

                // NavHost is the container for all your navigable screens
                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.HOME
                ) {
                    // Home Screen Route
                    composable(route = AppRoutes.HOME) {
                        HomeScreen(
                            locationViewModel = locationViewModel,
                            attractionsViewModel = attractionsViewModel,
                            onNavigateToDetail = { attractionId ->
                                // This is the actual navigation logic
                                navController.navigate("${AppRoutes.ATTRACTION_DETAIL}/$attractionId")
                            }
                        )
                    }

                    // Attraction Detail Screen Route
                    composable(
                        route = "${AppRoutes.ATTRACTION_DETAIL}/{${AppRoutes.ATTRACTION_ID_ARG}}",
                        arguments = listOf(navArgument(AppRoutes.ATTRACTION_ID_ARG) { type = NavType.StringType })
                    ) { backStackEntry ->
                        val attractionId = backStackEntry.arguments?.getString(AppRoutes.ATTRACTION_ID_ARG)
                        val attraction = attractionId?.let { attractionsViewModel.getAttractionById(it) }

                        if (attraction != null) {
                            AttractionDetailScreen(
                                attraction = attraction,
                                locationViewModel = locationViewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        } else {
                            // Optional: Show an error screen or navigate back if attraction is not found
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}