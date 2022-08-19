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
import java.text.DateFormatSymbols
import java.util.*
import kotlin.math.roundToInt

class CountStep : AppCompatActivity(), SensorEventListener {
    private var sensorManager : SensorManager? = null
    private var running = false
    private var totalStep : Float = 0f
    private var previousTotalSteps = 0f
    private var maxStep = 1000f

    private var binding : ActivityCountStepBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountStepBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setCustomView(R.layout.abs_layout)
        supportActionBar!!.title = "Pedometer"
        loadTime()
        loadData()
        resetStep()


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
        if (running){
            totalStep = p0!!.values[0]
            val currentSteps = totalStep.toInt() - previousTotalSteps.toInt()
            binding!!.stepsTakenTv.text = ("$currentSteps")
            val percent = (currentSteps * 100/ maxStep).roundToInt()
            binding!!.percentTv.text = baseContext.resources.getString(R.string.percent_aim,percent)
            binding!!.progressCircular.apply {
                setProgressWithAnimation(currentSteps.toFloat())
            }
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
}
