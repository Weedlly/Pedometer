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
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.example.pedometer.database.DatabaseAPI
import com.example.pedometer.databinding.AbsLayoutBinding
import com.example.pedometer.databinding.ActivityCountStepBinding
import com.example.pedometer.model.countstep.Week
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.DateFormatSymbols
import java.util.*
import kotlin.math.roundToInt

const val FOOT_TO_METER = 0.7867
const val FOOT_TO_CALORIE = 0.0667
class CountStep : AppCompatActivity(), SensorEventListener {
    //Sensor and CountStep
    private var sensorManager : SensorManager? = null
    private var running = false
    private var totalStep : Float = 0f
    private var previousTotalSteps = 0f
    private var myToday : String? = null

    //  Static data
    companion object {
        private const val TAG = "CountStep"
        private var maxStep = 1000f
        private val X_TITLE : Array<String> = arrayOf("SUN","MON","TUE","WED","THU","FRI","SAT")
    }

    private var barChart : BarChart? = null
    private var database : DatabaseAPI? = null
    private var deviceId : String? = null

    private var myWeek : Week? = null

    private var binding : ActivityCountStepBinding? = null
    private var absBinding : AbsLayoutBinding? = null

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
        absBinding!!.activityTitleTv.text = "Count Step"
        absBinding!!.activityTitleTv.textSize = 23f
        //Bottom navigation
        bottomNavigationHandle()

        // Get My database Key
        database = DatabaseAPI(baseContext)
        deviceId = database!!.deviceId
        Log.v(TAG,"Get key success: $deviceId")

        // Take data week
        myWeek = intent.getSerializableExtra("myWeek") as Week


        //Counter monitor
        loadTime()
        loadData()
        resetStep()
        checkNewDay()

//        initData(0f)
        //Chart visualise
        barChart = binding!!.weeklyBarChartBc

        createChartData()

        //init data to test
//        initData(500f)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

    }

    private fun bottomNavigationHandle(){
        val bottomNavigationView : BottomNavigationView = binding!!.bottomNavigation

        binding!!.bottomNavigation.menu[1].isChecked = true
        bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId){
                R.id.gps_training-> {
                    var gpsMapIntent = Intent(this, GpsMap::class.java)
                    gpsMapIntent.putExtra("myWeek", myWeek)
                    startActivity(gpsMapIntent)
                }
                R.id.target-> {
                    val intentUserSetup = Intent(this,UserSetup::class.java)
                    intentUserSetup.putExtra("isRegister",false)
                    startActivity(intentUserSetup)
                }
            }
            true
        }
    }
    private fun countCurrentSteps() : Int{
        return totalStep.toInt() - previousTotalSteps.toInt()
    }
    private fun initData(step : Float){
        totalStep = step
        val currentSteps = countCurrentSteps()
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

    override fun onResume() {
        super.onResume()
        Toast.makeText(this, "Resume!!!", Toast.LENGTH_SHORT).show()
        running = true
        val stepSensor : Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null){
            Toast.makeText(this,"No sensor detected on this device", Toast.LENGTH_SHORT).show()
        }
        else{
            sensorManager?.registerListener(this,stepSensor,SensorManager.SENSOR_DELAY_FASTEST)
            Log.v(TAG,"Start")
        }
    }

    override fun onPause() {
        super.onPause()
        saveData()
        Toast.makeText(this, "Pause!!!", Toast.LENGTH_SHORT).show()
        database!!.updateDataSpecifyDay(myToday!!,totalStep.toInt())
        Log.v(TAG,"Activity on pause, data updating!!!")
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (p0!!.sensor.type == Sensor.TYPE_STEP_COUNTER) {
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
        // Save step
        val editor = sharedPreferences.edit()
        editor.putFloat("previousTotalSteps",previousTotalSteps)

        // Save day
        val today = getWeekday(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        editor.putString("Today",today)
        Log.v(TAG,"Today save is: $today")

        editor.apply()

    }
    private fun checkNewDay(){
        val sharedPreferences = getSharedPreferences("myPrefs",Context.MODE_PRIVATE)
        val oldDay = sharedPreferences.getString("Today","")
        Log.v(TAG,"Old day: $oldDay")
        val today = getWeekday(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        if (oldDay == today){
            Log.v(TAG,"Still in today: $today")
        }
        else {
            // Reset data
            Log.v(TAG, "Change to new day is: $today")
            database!!.updateDataSpecifyDay(today!!,totalStep.toInt())
            previousTotalSteps = 0f
            initData(0f)
        }
        myToday = today
    }

    private fun loadData(){
        val sharedPreferences = getSharedPreferences("myPrefs",Context.MODE_PRIVATE)
        val saveNumber = sharedPreferences.getFloat("previousTotalSteps",0f)
        Log.v(TAG,"$saveNumber")
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

        barChart!!.setBackgroundColor(ContextCompat.getColor(baseContext,R.color.blue_gray))
        barChart!!.setGridBackgroundColor(ContextCompat.getColor(baseContext,R.color.blue_gray))

        barChart!!.xAxis.isEnabled = true
        barChart!!.axisLeft.isEnabled = false
        barChart!!.axisRight.isEnabled = false
        barChart!!.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart!!.xAxis.textColor = ContextCompat.getColor(baseContext,R.color.silver)
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

    private fun createChartData() {
        val values = arrayListOf<BarEntry>()
//        db.collection("Week").whereEqualTo("deviceId",deviceId)
//            .get().addOnSuccessListener {
//                if (it.documents.isNotEmpty()) {



//                }
//            }
        var i = 0f
        maxStep = myWeek!!.stepPerDay!!.toFloat()
        Log.v(TAG,"Get data from Firestore")
        binding!!.totalMaxStepTv.text = maxStep.toInt().toString()
        values.add(BarEntry(i++,myWeek!!.sun!!.toFloat()))
        values.add(BarEntry(i++,myWeek!!.mon!!.toFloat()))
        values.add(BarEntry(i++,myWeek!!.tue!!.toFloat()))
        values.add(BarEntry(i++,myWeek!!.wed!!.toFloat()))
        values.add(BarEntry(i++,myWeek!!.thu!!.toFloat()))
        values.add(BarEntry(i++,myWeek!!.fri!!.toFloat()))
        values.add(BarEntry(i,myWeek!!.sat!!.toFloat()))
        val set1 = BarDataSet(values,null)

        set1.color = ContextCompat.getColor(baseContext,R.color.yellow)
        set1.valueTextColor = ContextCompat.getColor(baseContext,R.color.white)

        val dataSets = arrayListOf<IBarDataSet>()
        dataSets.add(set1)
        configureChartAppearance()
        prepareChartData(BarData(dataSets))
        Log.v(TAG,"Implement chart")
    }

    private fun prepareChartData(barData : BarData){
        barData.setValueTextSize(10f)
        barChart!!.data = barData
        barChart!!.invalidate()
    }
}
