package com.immortalweeds.pedometer.model.gps

data class Leg(
    val admins: List<Admin>,
    val distance: Double,
    val duration: Double,
    val steps: List<Any>,
    val summary: String,
    val via_waypoints: List<Any>,
    val weight: Double
)