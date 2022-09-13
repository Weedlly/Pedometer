package com.example.pedometer.model

import android.content.Context
import com.github.mikephil.charting.charts.BarChart

class WeekChart (
    var barChart : BarChart,
    private val baseContext : Context,
    val X_TITLE : Array<String>
    ) {
    private var maxStep: Float = 0f
    private var TAG: String = ""
}