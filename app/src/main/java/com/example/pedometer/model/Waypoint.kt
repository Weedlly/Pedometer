package com.example.pedometer.model

data class Waypoint(
    val distance: Double,
    val location: List<Double>,
    val name: String
)