package com.example.ra_quadrado.viewmodels

import androidx.lifecycle.ViewModel
import com.example.ra_quadrado.models.LatLng
import java.text.DecimalFormat
import kotlin.math.*

class LocationViewModel : ViewModel() {

    // In a real app, this would be observed from the device's GPS
    val currentUserLocation = LatLng(-8.0578, -34.8829) // Example: Marco Zero, Recife

    fun formatDistance(distanceInKm: Double): String {
        return if (distanceInKm < 1) {
            "${(distanceInKm * 1000).roundToInt()} m de distância"
        } else {
            "${DecimalFormat("#.##").format(distanceInKm)} km de distância"
        }
    }

    fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val earthRadius = 6371 // Earth's radius in kilometers

        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLatRad = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLonRad = Math.toRadians(point2.longitude - point1.longitude)

        val a = sin(deltaLatRad / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLonRad / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c // Distance in kilometers
    }

    fun calculateDistance(destination: LatLng): Double {
        return calculateDistance(currentUserLocation, destination)
    }
}