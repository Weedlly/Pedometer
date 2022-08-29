package com.example.pedometer.place

data class Routes(
    val code: String,
    val routes: List<Route>,
    val uuid: String,
    val waypoints: List<Waypoint>
)