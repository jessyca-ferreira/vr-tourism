package com.example.ra_quadrado.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ra_quadrado.R
import com.example.ra_quadrado.viewmodels.AttractionsViewModel
import com.example.ra_quadrado.viewmodels.LocationViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    locationViewModel: LocationViewModel,
    attractionsViewModel: AttractionsViewModel,
    onNavigateToDetail: (attractionId: String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { BottomNavScreen.entries.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // locationViewModel.requestLocationPermission()
    }

    Scaffold(
        bottomBar = {
            AppBottomNavigation(
                currentScreen = BottomNavScreen.fromIndex(pagerState.currentPage),
                onTabTapped = { screen ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(screen.index)
                    }
                }
            )
        }
    ) { innerPadding -> // This is the padding we need
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
            when (BottomNavScreen.fromIndex(pageIndex)) {
                // Pass the padding to MapScreen
                BottomNavScreen.Map -> MapScreen(contentPadding = innerPadding, onNavigateToDetail = onNavigateToDetail)

                // Other screens manage their own padding
                BottomNavScreen.Attractions -> AttractionsScreen(attractionsViewModel, onNavigateToDetail = onNavigateToDetail)
                BottomNavScreen.Profile -> ProfileScreen()
                BottomNavScreen.AR -> ARScreen()
            }
        }
    }
}

@Composable
private fun AppBottomNavigation(
    currentScreen: BottomNavScreen,
    onTabTapped: (BottomNavScreen) -> Unit
) {
    Column(modifier = Modifier.shadow(10.dp)) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            BottomNavScreen.entries.forEach { screen ->
                val isSelected = currentScreen == screen
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabTapped(screen) },
                    label = {
                        Text(
                            text = screen.title,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = if (isSelected) screen.activeIconRes else screen.inactiveIconRes),
                            contentDescription = screen.title
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

private enum class BottomNavScreen(
    val index: Int,
    val title: String,
    val inactiveIconRes: Int,
    val activeIconRes: Int
) {
    Map(0, "Mapa", R.drawable.ic_map_outlined, R.drawable.ic_map_filled),
    Attractions(1, "Atrações", R.drawable.ic_explore_outlined, R.drawable.ic_explore_filled),
    Profile(2, "Perfil", R.drawable.ic_person_outlined, R.drawable.ic_person_filled),
    AR(3, "AR", R.drawable.ic_ar_outlined, R.drawable.ic_ar_filled); // New Screen

    companion object {
        fun fromIndex(index: Int) = entries.firstOrNull { it.index == index } ?: Map
    }
}