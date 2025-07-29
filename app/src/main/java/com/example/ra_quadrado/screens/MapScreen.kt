package com.example.ra_quadrado.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ra_quadrado.models.Attraction
import com.example.ra_quadrado.viewmodels.AttractionsViewModel
import com.example.ra_quadrado.viewmodels.LocationViewModel
import com.example.ra_quadrado.widgets.AttractionInfoCard
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.PaddingValues

@Composable
fun MapScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onNavigateToDetail: (attractionId: String) -> Unit
) {
    val locationViewModel: LocationViewModel = viewModel()
    val attractionsViewModel: AttractionsViewModel = viewModel()
    val attractions by attractionsViewModel.filteredAttractions.collectAsState()

    val recifeCenter = com.google.android.gms.maps.model.LatLng(-8.0578, -34.8829)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(recifeCenter, 14f)
    }

    val coroutineScope = rememberCoroutineScope()
    var selectedAttraction by remember { mutableStateOf<Attraction?>(null) }

    LaunchedEffect(attractions) {
        if (attractions.isNotEmpty()) {
            val bounds = LatLngBounds.builder()
            attractions.forEach { bounds.include(it.location.toGmsLatLng()) }
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds.build(), 100),
                1000
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        val uiSettings by remember {
            mutableStateOf(
                MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false
                )
            )
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings,
            onMapClick = { selectedAttraction = null }
        ) {
            attractions.forEach { attraction ->
                Marker(
                    state = MarkerState(position = attraction.location.toGmsLatLng()),
                    title = attraction.name,
                    snippet = attraction.category,
                    icon = BitmapDescriptorFactory.defaultMarker(getMarkerColor(attraction.category)),
                    onClick = {
                        selectedAttraction = attraction
                        false
                    }
                )
            }
        }

        MapFabColumn(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(contentPadding)
                .padding(16.dp),
            onCenterUserClick = {
                coroutineScope.launch {
                    locationViewModel.currentUserLocation.let { userLocation ->
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(userLocation.toGmsLatLng(), 15f)
                        )
                    }
                }
            },
            onFitAttractionsClick = {
                coroutineScope.launch {
                    if (attractions.isNotEmpty()) {
                        val bounds = LatLngBounds.builder()
                        attractions.forEach { bounds.include(it.location.toGmsLatLng()) }
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
                    }
                }
            },
            onRefreshClick = {
                // TODO: Implement refresh logic
            }
        )

        AnimatedVisibility(
            visible = selectedAttraction != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(contentPadding),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            selectedAttraction?.let {
                AttractionInfoCard(
                    modifier = Modifier.padding(16.dp),
                    attraction = it,
                    onTap = { onNavigateToDetail(it.id) },
                    onClose = { selectedAttraction = null }
                )
            }
        }
    }
}


@Composable
private fun MapFabColumn(
    modifier: Modifier = Modifier,
    onCenterUserClick: () -> Unit,
    onFitAttractionsClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        SmallFloatingActionButton(
            onClick = onCenterUserClick,
            icon = Icons.Default.MyLocation
        )
        SmallFloatingActionButton(
            onClick = onFitAttractionsClick,
            icon = Icons.Default.Map
        )
        SmallFloatingActionButton(
            onClick = onRefreshClick,
            icon = Icons.Default.Refresh
        )
    }
}

@Composable
private fun SmallFloatingActionButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        elevation = FloatingActionButtonDefaults.elevation(4.dp)
    ) {
        Icon(icon, contentDescription = null)
    }
}

private fun getMarkerColor(category: String): Float {
    return when (category) {
        "Museu" -> BitmapDescriptorFactory.HUE_AZURE
        "Centro Cultural" -> BitmapDescriptorFactory.HUE_VIOLET
        "Teatro" -> BitmapDescriptorFactory.HUE_ORANGE
        "Centro de Artesanato" -> BitmapDescriptorFactory.HUE_GREEN
        "Marco" -> BitmapDescriptorFactory.HUE_RED
        "Religioso" -> BitmapDescriptorFactory.HUE_YELLOW
        else -> BitmapDescriptorFactory.HUE_ROSE
    }
}

// This is the corrected function with explicit types
private fun com.example.ra_quadrado.models.LatLng.toGmsLatLng(): com.google.android.gms.maps.model.LatLng {
    return com.google.android.gms.maps.model.LatLng(this.latitude, this.longitude)
}