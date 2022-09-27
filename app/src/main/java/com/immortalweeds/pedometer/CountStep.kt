package com.immortalweeds.pedometer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.github.mikephil.charting.charts.BarChart
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.immortalweeds.pedometer.builders.WeekChart
import com.immortalweeds.pedometer.database.DatabasePreference
import com.immortalweeds.pedometer.databinding.AbsLayoutBinding
import com.immortalweeds.pedometer.databinding.ActivityCountStepBinding
import com.immortalweeds.pedometer.model.countstep.Week
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
    private var maxStep: Float? = null

    //  Static data
    companion object {
        private const val PERMISSIONS_REQUEST_ACCESS_ACTIVITY_RECOGNITION = 2
        private const val TAG = "CountStep"
        private val X_TITLE: Array<String> =
            arrayOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    }
    private var todayNumber : Int? = null
    private var barChart: BarChart? = null
    private var databasePreference: DatabasePreference? = null

    private var myWeek: Week? = Week()

    private var binding: ActivityCountStepBinding? = null
    private var absBinding: AbsLayoutBinding? = null

    private var activityRecognitionGranted = false

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountStepBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        //Setup Activity Custom action bar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        absBinding = AbsLayoutBinding.inflate(layoutInflater)

        supportActionBar!!.customView = absBinding!!.root
        absBinding!!.activityTitleTv.text = baseContext.resources.getString(R.string.count_step_activity_title)
        //Bottom navigation
        bottomNavigationHandle()

        loadWeekData()
        maxStep = myWeek!!.stepPerDay!!.toFloat()
        binding!!.progressCircular.progressMax = maxStep!!

        // Init database
        databasePreference = DatabasePreference(baseContext)
        myWeek = databasePreference!!.initData(0)
        todayNumber = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        loadWeekData()
        maxStep = myWeek!!.stepPerDay!!.toFloat()
        binding!!.progressCircular.progressMax = maxStep!!

        binding!!.totalMaxStepTv.text =baseContext.resources.getString(R.string.aim_step,maxStep!!.toInt())
        Log.v(TAG, "Max step : $maxStep")

        //Counter monitor
        loadTime()
        loadData()
        resetStep()

        //Chart visualise
        barChart = binding!!.weeklyBarChartBc

        //Construct bar chart
        constructBarChar()
//        //init data to test
        initData()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

    }
    private fun loadWeekData() : Boolean {
        val sharedPreferences = getSharedPreferences("myPrefs",Context.MODE_PRIVATE)

        myWeek!!.deviceId = sharedPreferences.getString("deviceId","")
        myWeek!!.stepPerDay = sharedPreferences.getInt("stepPerDay",0)
        myWeek!!.mon = sharedPreferences.getInt("monStep",0)
        myWeek!!.tue = sharedPreferences.getInt("tueStep",0)
        myWeek!!.wed = sharedPreferences.getInt("wedStep",0)
        myWeek!!.thu = sharedPreferences.getInt("thuStep",0)
        myWeek!!.fri = sharedPreferences.getInt("friStep",0)
        myWeek!!.sat = sharedPreferences.getInt("satStep",0)
        myWeek!!.sun = sharedPreferences.getInt("sunStep",0)
        return true
    }
    private fun constructBarChar() {
        barChart = WeekChart.WeekChartBuilder(
            barChart!!,
            baseContext,
            X_TITLE
        )
            .setMaxStep(maxStep!!)
            .setWeek(myWeek!!).build()

    }

    private fun bottomNavigationHandle() {
        val bottomNavigationView: BottomNavigationView = binding!!.bottomNavigation

        binding!!.bottomNavigation.menu[1].isChecked = true
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.gps_training -> {
                    startActivity(Intent(this, GpsMap::class.java))
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


    private fun initData() {

        binding!!.stepsTakenTv.text = ("${totalStep.toInt()}")

        var percent = (totalStep * 100 / maxStep!!)
        if (percent.isNaN()) percent = 0f
        binding!!.percentTv.text =
            baseContext.resources.getString(R.string.percent_aim, percent.roundToInt())


        binding!!.progressCircular.apply {
            setProgressWithAnimation(totalStep)
        }

        binding!!.distanceContentTv.text = baseContext.resources.getString(
            R.string.distances,
            FOOT_TO_METER * totalStep
        )
        binding!!.caloriesContentTv.text = baseContext.resources.getString(
            R.string.calories,
            FOOT_TO_CALORIE * totalStep
        )
    }


    private fun activityRecognitionPermission(){
        Log.v(TAG, "version sdk : ${Build.VERSION.SDK_INT} and version code : ${Build.VERSION_CODES.Q}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {

                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                    PERMISSIONS_REQUEST_ACCESS_ACTIVITY_RECOGNITION
                )
            }
            else{
                activityRecognitionGranted = true
            }
        }
        else{
            Log.v(TAG,"seftpermis ${ContextCompat.checkSelfPermission(this,
                "com.google.android.gms.permission.ACTIVITY_RECOGNITION")}")
            if (ContextCompat.checkSelfPermission(this,
                    "com.google.android.gms.permission.ACTIVITY_RECOGNITION") == PackageManager.PERMISSION_DENIED){

                ActivityCompat.requestPermissions(this,
                    arrayOf("com.google.android.gms.permission.ACTIVITY_RECOGNITION"),
                    PERMISSIONS_REQUEST_ACCESS_ACTIVITY_RECOGNITION)
            }
            else{
                activityRecognitionGranted = true
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        activityRecognitionGranted = false
        when(requestCode){
            PERMISSIONS_REQUEST_ACCESS_ACTIVITY_RECOGNITION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    activityRecognitionGranted = true
                    startSensor()
                }
                else{
                    Toast.makeText(this,"This activity need permission to use",Toast.LENGTH_SHORT).show()
                    val intentUserSetup = Intent(this, UserSetup::class.java)
                    intentUserSetup.putExtra("isRegister", false)
                    startActivity(intentUserSetup)
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
    override fun onResume() {
        super.onResume()
        running = true
        activityRecognitionPermission()
        Log.v(TAG,"permission : $activityRecognitionGranted")

        startSensor()
    }
    private fun startSensor(){
        if (activityRecognitionGranted) {
            val stepSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            if (stepSensor == null) {
                Toast.makeText(this, "No sensor detected on this device", Toast.LENGTH_SHORT).show()
            } else {
                sensorManager?.registerListener(
                    this,
                    stepSensor,
                    SensorManager.SENSOR_DELAY_FASTEST
                )
                Toast.makeText(this, "Set up monitor!!!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        running = false
        myWeek = databasePreference!!.updateSpecifyDay(
            myWeek!!,
            todayNumber!!,
            totalStep.toInt()
        )
        saveData()
        saveWeekData()
        Toast.makeText(this, "Pause!!!", Toast.LENGTH_SHORT).show()
        Log.v(TAG, "Activity on pause, data updating!!!")
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        Toast.makeText(this, "Counting!!!", Toast.LENGTH_SHORT).show()
        if (p0!!.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
            if (running) {
                totalStep += p0.values[0]
                myWeek = databasePreference!!.updateSpecifyDay(myWeek!!,todayNumber!!,totalStep.toInt())
                Log.v(TAG,"Sensor value: $totalStep")

                binding!!.stepsTakenTv.text = ("${totalStep.toInt()}")
                var percent = (totalStep * 100 / maxStep!!)
                if (percent.isNaN()) percent = 0f
                binding!!.percentTv.text =
                    baseContext.resources.getString(R.string.percent_aim, percent.roundToInt())

                binding!!.progressCircular.apply {
                    setProgressWithAnimation(totalStep)
                }
                Toast.makeText(this, "Counting!!!", Toast.LENGTH_SHORT).show()
                binding!!.distanceContentTv.text = baseContext.resources.getString(
                    R.string.distances,
                    FOOT_TO_METER * totalStep
                )
                binding!!.caloriesContentTv.text = baseContext.resources.getString(
                    R.string.calories,
                    FOOT_TO_CALORIE * totalStep
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
            totalStep = 0f
            binding!!.stepsTakenTv.text = "0"
            binding!!.percentTv.text = baseContext.resources.getString(R.string.percent_aim, 0)
            saveData()
            saveWeekData()
            true
        }
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        // Save step
        val editor = sharedPreferences.edit()
        editor.putFloat("previousTotalSteps", totalStep)
        editor.apply()
    }

    private fun saveWeekData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        val editor = sharedPreferences.edit()

        Log.v(TAG,"Week save is : $myWeek")
        editor.putString("deviceId", myWeek!!.deviceId)
        editor.putInt("stepPerDay",myWeek!!.stepPerDay!!)
        editor.putInt("monStep",myWeek!!.mon!!)
        editor.putInt("tueStep",myWeek!!.tue!!)
        editor.putInt("wedStep",myWeek!!.wed!!)
        editor.putInt("thuStep",myWeek!!.thu!!)
        editor.putInt("friStep",myWeek!!.fri!!)
        editor.putInt("satStep",myWeek!!.sat!!)
        editor.putInt("sunStep",myWeek!!.sun!!)

        editor.apply()
    }


    private fun loadData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val saveNumber = sharedPreferences.getFloat("previousTotalSteps", 0f)
        Log.v(TAG, "$saveNumber")
        totalStep = saveNumber

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
