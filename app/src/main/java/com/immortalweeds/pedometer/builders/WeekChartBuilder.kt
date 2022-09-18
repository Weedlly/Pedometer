package com.immortalweeds.pedometer.builders

import android.content.Context
import androidx.core.content.ContextCompat
import com.immortalweeds.pedometer.R
import com.immortalweeds.pedometer.model.countstep.Week
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet

class WeekChart private constructor(
    private var barChart: BarChart
)  {
    data class WeekChartBuilder(
        var barChart : BarChart,
        var baseContext : Context,
        var X_TITLE : Array<String>,
        private var maxStep: Float = 0f,
        private var myWeek: Week = Week("",0,0,0,0,0,0,0,0)
    ){
        fun setMaxStep(maxStep: Float) = apply {
            this.maxStep = maxStep

            barChart.description.isEnabled = false
            barChart.setDrawValueAboveBar(false)
            barChart.setDrawGridBackground(true)

            barChart.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.blue_gray))
            barChart.setGridBackgroundColor(
                ContextCompat.getColor(
                    baseContext,
                    R.color.blue_gray
                )
            )

            barChart.xAxis.isEnabled = true
            barChart.axisLeft.isEnabled = false
            barChart.axisRight.isEnabled = false
            barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            barChart.xAxis.textColor = ContextCompat.getColor(baseContext, R.color.silver)
            barChart.xAxis.textSize = 18f
            barChart.setDrawValueAboveBar(true)

            val xAxis: XAxis = barChart.xAxis

            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return X_TITLE[value.toInt()]
                }
            }

            val axisLeft: YAxis = barChart.axisLeft
            axisLeft.granularity = 10f
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = maxStep

            val axisRight: YAxis = barChart.axisRight
            axisRight.granularity = 10f
            axisRight.axisMinimum = 0f

        }

        fun setWeek(week: Week) = apply {
            this.myWeek = week

            val values = arrayListOf<BarEntry>()
            var i = 0f

            values.add(BarEntry(i++, myWeek.sun!!.toFloat()))
            values.add(BarEntry(i++, myWeek.mon!!.toFloat()))
            values.add(BarEntry(i++, myWeek.tue!!.toFloat()))
            values.add(BarEntry(i++, myWeek.wed!!.toFloat()))
            values.add(BarEntry(i++, myWeek.thu!!.toFloat()))
            values.add(BarEntry(i++, myWeek.fri!!.toFloat()))
            values.add(BarEntry(i, myWeek.sat!!.toFloat()))


            val barDataSet = BarDataSet(values, null)
            barDataSet.color = ContextCompat.getColor(baseContext, R.color.yellow)
            barDataSet.valueTextColor = ContextCompat.getColor(baseContext, R.color.white)

            val dataSets = arrayListOf<IBarDataSet>()
            dataSets.add(barDataSet)

            val barData = BarData(dataSets)
            barData.setValueTextSize(15f)
            barChart.data = barData
            barChart.invalidate()

        }

        fun build() = WeekChart(
            barChart
        ).barChart

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as WeekChartBuilder

            if (!X_TITLE.contentEquals(other.X_TITLE)) return false

            return true
        }

        override fun hashCode(): Int {
            return X_TITLE.contentHashCode()
        }
    }
}