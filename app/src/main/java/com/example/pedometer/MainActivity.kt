package com.example.pedometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.example.pedometer.databinding.ActivityMainBinding
import java.util.zip.Inflater

class MainActivity : AppCompatActivity(), SensorEventListener {
    private var sensorManager : SensorManager? = null
    private var running = false
    private var totalStep : Float = 0f
    private var previousTotalSteps = 0f

    private var binding : ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        loadData()
        resetStep()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onResume() {
        super.onResume()
        running = true
        val stepSensor : Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null){
            Toast.makeText(this,"No senser detected on this device", Toast.LENGTH_SHORT).show()
        }
        else{
            sensorManager?.registerListener(this,stepSensor,SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (running){
            totalStep = p0!!.values[0]
            val currentSteps = totalStep.toInt() - previousTotalSteps.toInt()
            binding!!.stepsTakenTv.text = ("$currentSteps")

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
        Log.d("MainActivity","$saveNumber")
        previousTotalSteps = saveNumber
    }

}
