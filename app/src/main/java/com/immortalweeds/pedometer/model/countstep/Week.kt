package com.immortalweeds.pedometer.model.countstep

import java.io.Serializable

data class Week (
    var deviceId: String? = "",
    var stepPerDay: Int? = 0,
    var mon: Int? = 0,
    var tue: Int? = 0,
    var wed: Int? = 0,
    var thu: Int? = 0,
    var fri: Int? = 0,
    var sat: Int? = 0,
    var sun: Int? = 0,
) : Serializable