package com.immortalweeds.pedometer.model.gps

data class Waypoint(
    val distance: Double,
    val location: List<Double>,
    val name: String
)