package com.example.pedometer.place

import com.google.gson.annotations.SerializedName

data class Geometry(
    @SerializedName("coordinates")
    val coordinates: List<List<Double>>,
    @SerializedName("type")
    val type: String
)