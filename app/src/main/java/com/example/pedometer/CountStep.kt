package com.example.pedometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.example.pedometer.databinding.ActivityCountStepBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import java.text.DateFormatSymbols
import java.util.*
import kotlin.math.roundToInt

const val FOOT_TO_METER = 0.3048
const val FOOT_TO_CALORIE = 0.00032
class CountStep : AppCompatActivity(), SensorEventListener {
    //Sensor and CountStep
    private var sensorManager : SensorManager? = null
    private var running = false
    private var totalStep : Float = 0f
    private var previousTotalSteps = 0f
    private var maxStep = 1000f

    //Chart
    private val MAX_X_VALUE = 7
    private val MAX_Y_VALUE = maxStep
    private val MIN_Y_VALUE = maxStep / 10
    private val X_TITLE : Array<String> = arrayOf("SUN","MON","TUE","WED","THU","FRI","SAT")
    private var barChart : BarChart? = null

    private var binding : ActivityCountStepBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountStepBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setCustomView(R.layout.abs_layout)
        supportActionBar!!.title = "Pedometer"

        //Counter monitor
        loadTime()
        loadData()
        resetStep()

        //Chart visualise
        barChart = binding!!.weeklyBarChartBc

        val data = createChartData()
        configureChartAppearance()
        prepareChartData(data)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onResume() {
        super.onResume()
        running = true
        val stepSensor : Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null){
            Toast.makeText(this,"No sensor detected on this device", Toast.LENGTH_SHORT).show()
        }
        else{
            sensorManager?.registerListener(this,stepSensor,SensorManager.SENSOR_DELAY_FASTEST)
            Log.v("MainActivity","Start")
        }
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (running) {
            totalStep = p0!!.values[0]
            val currentSteps = totalStep.toInt() - previousTotalSteps.toInt()
            binding!!.stepsTakenTv.text = ("$currentSteps")

            val percent = (currentSteps * 100 / maxStep).roundToInt()
            binding!!.percentTv.text =
                baseContext.resources.getString(R.string.percent_aim, percent)

            binding!!.progressCircular.apply {
                setProgressWithAnimation(currentSteps.toFloat())
            }

            binding!!.distanceContentTv.text = baseContext.resources.getString(
                R.string.distances,
                FOOT_TO_METER * currentSteps
            )
            binding!!.caloriesContentTv.text = baseContext.resources.getString (
                R.string.calories,
                FOOT_TO_CALORIE * currentSteps
            )
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    private fun resetStep(){
        binding!!.stepsTakenTv.setOnClickListener{
            Toast.makeText(this,"Long tap to reset steps", Toast.LENGTH_SHORT).show()
        }
        binding!!.stepsTakenTv.setOnLongClickListener{
            previousTotalSteps = totalStep
            binding!!.stepsTakenTv.text = "0"
            binding!!.percentTv.text = baseContext.resources.getString(R.string.percent_aim,0)
            saveData()
            true
        }
    }

    private fun saveData(){
        val sharedPreferences = getSharedPreferences("myPrefs",Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("key1",previousTotalSteps)
        editor.apply()
    }
    private fun loadData(){
        val sharedPreferences = getSharedPreferences("myPrefs",Context.MODE_PRIVATE)
        val saveNumber = sharedPreferences.getFloat("key1",0f)
        Log.v("MainActivity","$saveNumber")
        previousTotalSteps = saveNumber
    }
    private fun loadTime(){
        val calendarInstance = Calendar.getInstance()
        val day = calendarInstance.get(Calendar.DAY_OF_MONTH)
        val month = calendarInstance.get(Calendar.MONTH)
        val weekday = calendarInstance.get(Calendar.DAY_OF_WEEK)
        binding!!.weekdaysTv.text = getWeekday(weekday)

        binding!!.monthAndDayTv.text = baseContext.resources.getString(
            R.string.month_and_day,
            getMonth(month),
            day.toString()
        )
    }
    private fun getMonth(month: Int): String? {
        return DateFormatSymbols().months[month - 1]
    }
    private fun getWeekday(weekday: Int): String? {
        return DateFormatSymbols().weekdays[weekday]
    }
    private fun configureChartAppearance() {
        barChart!!.description.isEnabled = false
        barChart!!.setDrawValueAboveBar(false)
        barChart!!.setDrawGridBackground(true)
        barChart!!.setBackgroundColor(resources.getColor(R.color.blue_gray))
        barChart!!.setGridBackgroundColor(resources.getColor(R.color.blue_gray))

        barChart!!.xAxis.isEnabled = true
        barChart!!.axisLeft.isEnabled = false
        barChart!!.axisRight.isEnabled = false
        barChart!!.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart!!.xAxis.textColor = resources.getColor(R.color.silver)
        barChart!!.xAxis.textSize = 12f

        val xAxis : XAxis = barChart!!.xAxis

        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return X_TITLE[value.toInt()]
            }
        }
        val axisLeft : YAxis = barChart!!.axisLeft
        axisLeft.granularity = 10f
        axisLeft.axisMinimum = 0f

        val axisRight : YAxis = barChart!!.axisRight
        axisRight.granularity = 10f
        axisRight.axisMinimum = 0f
    }

    private fun createChartData(): BarData {
        val values = arrayListOf<BarEntry>()
        for (i in 0 until MAX_X_VALUE) {
            val x = i.toFloat()
            val y = (MIN_Y_VALUE.toInt()..MAX_Y_VALUE.toInt()).random().toFloat()
            values.add(
                BarEntry(x, y)
            )
        }
        val set1 = BarDataSet(values,null)

        set1.color = resources.getColor(R.color.yellow)
        set1.valueTextColor = resources.getColor(R.color.white)

        val dataSets = arrayListOf<IBarDataSet>()
        dataSets.add(set1)
        return BarData(dataSets)
    }

    private fun prepareChartData(barData : BarData){
        barData.setValueTextSize(10f)
        barChart!!.data = barData
        barChart!!.invalidate()
    }
}
