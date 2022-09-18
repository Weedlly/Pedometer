package com.immortalweeds.pedometer.model.gps

data class Route(
    val country_crossed: Boolean,
    val distance: Double,
    val duration: Double,
    val geometry: Geometry,
    val legs: List<Leg>,
    val weight: Double,
    val weight_name: String
)