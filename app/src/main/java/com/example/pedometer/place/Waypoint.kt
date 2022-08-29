package com.example.pedometer.place

data class Waypoint(
    val distance: Double,
    val location: List<Double>,
    val name: String
)