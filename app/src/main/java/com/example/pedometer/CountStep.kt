package com.example.pedometer

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.example.pedometer.builders.WeekChart
import com.example.pedometer.database.DatabaseAPI
import com.example.pedometer.databinding.AbsLayoutBinding
import com.example.pedometer.databinding.ActivityCountStepBinding
import com.example.pedometer.model.countstep.Week
import com.github.mikephil.charting.charts.BarChart
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.DateFormatSymbols
import java.util.*
import kotlin.math.roundToInt

const val FOOT_TO_METER = 0.7867
const val FOOT_TO_CALORIE = 0.0667
class CountStep : AppCompatActivity(), SensorEventListener {
    //Sensor and CountStep
    private var sensorManager: SensorManager? = null
    private var running = false
    private var totalStep: Float = 0f
    private var previousTotalSteps = 0f
    private var maxStep: Float? = null

    //  Static data
    companion object {
        private const val TAG = "CountStep"
        private val X_TITLE: Array<String> =
            arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    }

    private var barChart: BarChart? = null
    private var database: DatabaseAPI? = null
    private var deviceId: String? = null

    private var myWeek: Week? = null

    private var binding: ActivityCountStepBinding? = null
    private var absBinding: AbsLayoutBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, "Create!!!", Toast.LENGTH_SHORT).show()
        binding = ActivityCountStepBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        //Setup Activity Custom action bar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        absBinding = AbsLayoutBinding.inflate(layoutInflater)
        supportActionBar!!.customView = absBinding!!.root
        absBinding!!.activityTitleTv.text =
            baseContext.resources.getString(R.string.count_step_activity_title)
        absBinding!!.activityTitleTv.textSize = 23f
        //Bottom navigation
        bottomNavigationHandle()

        // Get My database Key
        database = DatabaseAPI(baseContext)
        deviceId = database!!.deviceId
        Log.v(TAG, "Get key success: $deviceId")

        // Take data week
        myWeek = intent.getSerializableExtra("myWeek") as Week
        maxStep = myWeek!!.stepPerDay!!.toFloat()
        binding!!.progressCircular.progressMax = maxStep!!

        //Counter monitor
        loadTime()
        loadData()
        resetStep()

        //Chart visualise
        barChart = binding!!.weeklyBarChartBc

        //Construct bar chart
        constructBarChar()
        //init data to test
//        initData(500f)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

    }

    private fun constructBarChar() {
        barChart = WeekChart.WeekChartBuilder(
            barChart!!,
            baseContext,
            X_TITLE
        )
            .setMaxStep(maxStep!!)
            .setWeek(myWeek!!).build()
        binding!!.totalMaxStepTv.text = maxStep!!.toInt().toString()
        Log.v(TAG, "Max step : $maxStep")
    }

    private fun bottomNavigationHandle() {
        val bottomNavigationView: BottomNavigationView = binding!!.bottomNavigation

        binding!!.bottomNavigation.menu[1].isChecked = true
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.gps_training -> {
                    val gpsMapIntent = Intent(this, GpsMap::class.java)
                    gpsMapIntent.putExtra("myWeek", myWeek)
                    startActivity(gpsMapIntent)
                }
                R.id.target -> {
                    val intentUserSetup = Intent(this, UserSetup::class.java)
                    intentUserSetup.putExtra("isRegister", false)
                    startActivity(intentUserSetup)
                }
            }
            true
        }
    }

    private fun countCurrentSteps(): Int {
        return totalStep.toInt() - previousTotalSteps.toInt()
    }

    private fun initData(step: Float) {
        totalStep = step
        val currentSteps = countCurrentSteps()
        binding!!.stepsTakenTv.text = ("$currentSteps")

        var percent = (currentSteps * 100 / maxStep!!)
        if (percent.isNaN()) percent = 0f
        binding!!.percentTv.text =
            baseContext.resources.getString(R.string.percent_aim, percent.roundToInt())


        binding!!.progressCircular.apply {
            setProgressWithAnimation(currentSteps.toFloat())
        }

        binding!!.distanceContentTv.text = baseContext.resources.getString(
            R.string.distances,
            FOOT_TO_METER * currentSteps
        )
        binding!!.caloriesContentTv.text = baseContext.resources.getString(
            R.string.calories,
            FOOT_TO_CALORIE * currentSteps
        )
    }

    override fun onResume() {
        super.onResume()
        Toast.makeText(this, "Resume!!!", Toast.LENGTH_SHORT).show()
        running = true
        val stepSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null) {
            Toast.makeText(this, "No sensor detected on this device", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
            Log.v(TAG, "Start")
        }
    }

    override fun onPause() {
        super.onPause()
        saveData()
        Toast.makeText(this, "Pause!!!", Toast.LENGTH_SHORT).show()
        myWeek = database!!.updateSpecifyDayOnWeek(
            myWeek!!,
            getWeekday(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))!!,
            totalStep.toInt()
        )
        database!!.updateWeekToFireStore(myWeek!!)
        Log.v(TAG, "Activity on pause, data updating!!!")
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (p0!!.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            if (running) {
                totalStep = p0.values[0]
                val currentSteps = totalStep.toInt() - previousTotalSteps.toInt()
                binding!!.stepsTakenTv.text = ("$currentSteps")
                var percent = (currentSteps * 100 / maxStep!!)
                if (percent.isNaN()) percent = 0f
                binding!!.percentTv.text =
                    baseContext.resources.getString(R.string.percent_aim, percent.roundToInt())

                binding!!.progressCircular.apply {
                    setProgressWithAnimation(currentSteps.toFloat())
                }
                Toast.makeText(this, "Counting!!!", Toast.LENGTH_SHORT).show()
                Log.v(TAG, "Counting!!!")
                binding!!.distanceContentTv.text = baseContext.resources.getString(
                    R.string.distances,
                    FOOT_TO_METER * currentSteps
                )
                binding!!.caloriesContentTv.text = baseContext.resources.getString(
                    R.string.calories,
                    FOOT_TO_CALORIE * currentSteps
                )
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    private fun resetStep() {
        binding!!.stepsTakenTv.setOnClickListener {
            Toast.makeText(this, "Long tap to reset steps", Toast.LENGTH_SHORT).show()
        }
        binding!!.stepsTakenTv.setOnLongClickListener {
            previousTotalSteps = totalStep
            binding!!.stepsTakenTv.text = "0"
            binding!!.percentTv.text = baseContext.resources.getString(R.string.percent_aim, 0)
            saveData()
            true
        }
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        // Save step
        val editor = sharedPreferences.edit()
        editor.putFloat("previousTotalSteps", previousTotalSteps)

        editor.apply()

    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val saveNumber = sharedPreferences.getFloat("previousTotalSteps", 0f)
        Log.v(TAG, "$saveNumber")
        previousTotalSteps = saveNumber
    }

    private fun loadTime() {
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
}

fun getMonth(month: Int): String? {
    return DateFormatSymbols().months[month - 1]
}
fun getWeekday(weekday: Int): String? {
    return DateFormatSymbols().weekdays[weekday]
}
