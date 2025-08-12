package com.example.ra_quadrado.models

// A placeholder LatLng data class. You might replace this with a library version.
data class LatLng(val latitude: Double, val longitude: Double)

// The main data model for an attraction.
data class Attraction(
    val id: String,
    val name: String,
    val description: String,
    val history: String,
    val importance: String,
    val imageUrl: String,
    val category: String,
    val location: LatLng,
    val address: String,
    val phoneNumber: String,
    val website: String,
    val rating: Double,
    val visitDuration: Int, // in minutes
    val price: Double,
    val openingHours: Map<String, String>,
    val tags: List<String>
)