package com.example.pedometer

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.roundToInt

class Counter() :  SensorEventListener {
    private var sensorManager : SensorManager? = null
    private var running = false
    private var totalStep : Float = 0f
    private var previousTotalSteps = 0f
    override fun onSensorChanged(p0: SensorEvent?) {
        if (running) {
            totalStep = p0!!.values[0]
            val currentSteps = totalStep.toInt() - previousTotalSteps.toInt()

        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

}