package com.immortalweeds.pedometer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.immortalweeds.pedometer.database.DatabasePreference
import com.immortalweeds.pedometer.databinding.AbsLayoutBinding
import com.immortalweeds.pedometer.databinding.ActivityUserSetupBinding
import com.immortalweeds.pedometer.model.countstep.Week
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

class UserSetup : AppCompatActivity() {
    private var databasePreference : DatabasePreference? = null
    private var binding : ActivityUserSetupBinding? = null
    private var absBinding : AbsLayoutBinding? = null

    private var maxSteps: Float? = 0f
    private var calories: Float? = 0f
    private var isRegister : Boolean = false
    private var myWeek : Week? = Week()

    private var isInitAccount : Boolean? = null
    companion object{
        private const val TAG = "UserSetup"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSetupBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        //Setup Activity Custom action bar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM

        absBinding = AbsLayoutBinding.inflate(layoutInflater)
        supportActionBar!!.customView = absBinding!!.root
        absBinding!!.activityTitleTv.text =  baseContext.resources.getString(R.string.user_setup_activity_title)

        // Because delay when connect to Firestore so need to prevent user moving to another screen
        bottomNavigationVisible(false)

        //Bottom navigation
        bottomNavigationHandle()

        //Setup database

        databasePreference = DatabasePreference(baseContext)
        Log.v(TAG,"Get key success: ${databasePreference!!.deviceId}")

        isRegister = intent.getBooleanExtra("isRegister",false)

        isTargetFill()
        eventHandle()
    }
    private fun eventHandle(){
        binding!!.goBt.setOnClickListener{
            if (!binding!!.totalMaxStepEv.text.isNullOrBlank() && !binding!!.totalCaloriesEv.text.isNullOrBlank()){
                myWeek!!.stepPerDay = maxSteps!!.toInt()

                val countStepIntent = Intent(this,CountStep::class.java)
                countStepIntent.putExtra("myWeek",myWeek)
                startActivity(countStepIntent)
            }
            else{
                Toast.makeText(this,"You need to fill Step or Calories target to starting",Toast.LENGTH_SHORT).show()
            }
        }

        binding!!.totalMaxStepEv.setOnClickListener {
            if (!binding!!.totalMaxStepEv.text.isNullOrBlank()) {
                maxSteps = binding!!.totalMaxStepEv.text.toString().toFloat()
                calories = (maxSteps!!.toFloat() * FOOT_TO_CALORIE).toFloat()
                binding!!.totalCaloriesEv.setText(calories!!.toInt().toString())
            }
        }
        binding!!.totalCaloriesEv.setOnClickListener {
            if (!binding!!.totalCaloriesEv.text.isNullOrBlank()) {
                calories = binding!!.totalCaloriesEv.text.toString().toFloat()
                maxSteps = (calories!!.toFloat() / FOOT_TO_CALORIE).toFloat()
                binding!!.totalMaxStepEv.setText(maxSteps!!.toInt().toString())
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Save
        myWeek!!.stepPerDay = maxSteps!!.toInt()
        saveData()

//        Toast.makeText(this, "Pause!!!", Toast.LENGTH_SHORT).show()
//
        myWeek!!.stepPerDay = maxSteps!!.toInt()
        Log.v(TAG, "Activity on pause, data updating!!!")
    }
    private fun isTargetFill(){
        if (!loadWeekData()){
            Log.v(TAG,"User data is init")
            bottomNavigationVisible(false)
            isInitAccount = true
        }
        else{
            Log.v(TAG,"User data is exist")
            if (myWeek!!.stepPerDay == 0){
                bottomNavigationVisible(false)
            }
            else {
                maxSteps = myWeek!!.stepPerDay!!.toFloat()
                calories = (maxSteps!!.toFloat() * FOOT_TO_CALORIE).toFloat()
                binding!!.totalCaloriesEv.setText(calories!!.toInt().toString())
                binding!!.totalMaxStepEv.setText(maxSteps!!.toInt().toString())
                bottomNavigationVisible(true)
            }
            if(isRegister) {
                val countStepIntent = Intent(this, CountStep::class.java)
                countStepIntent.putExtra("myWeek", myWeek)
                startActivity(countStepIntent)
            }
            isInitAccount = false
        }
        checkNewDay()
    }

    private fun bottomNavigationVisible(flag : Boolean){
        binding!!.bottomNavigation.menu[0].isVisible = flag
        binding!!.bottomNavigation.menu[1].isVisible = flag
    }
    private fun bottomNavigationHandle(){
        val bottomNavigationView : BottomNavigationView = binding!!.bottomNavigation

        binding!!.bottomNavigation.menu[2].isChecked = true
        bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId){
                R.id.gps_training-> {
                    startActivity(Intent(this, GpsMap::class.java))
                }
                R.id.home-> {
                    startActivity(Intent(this, CountStep::class.java))
                }
            }
            true
        }
    }
    private fun loadWeekData() : Boolean {
        val sharedPreferences = getSharedPreferences("myPrefs",Context.MODE_PRIVATE)

        myWeek = databasePreference!!.initData(0)

        val deviceId = sharedPreferences.getString("deviceId","")
        if (deviceId == ""){
            return false
        }
        myWeek!!.deviceId = deviceId
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
    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        val editor = sharedPreferences.edit()
        // Save day
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        editor.putInt("today", today)
        Log.v(TAG, "Today save is: $today")

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
    private fun resetData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        val editor = sharedPreferences.edit()
        editor.putFloat("previousTotalSteps",0f)
        editor.apply()

    }
    private fun checkNewDay() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val oldDay = sharedPreferences.getInt("today",0)
        Log.v(TAG, "Old day: $oldDay")
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (oldDay == today && !isInitAccount!!) {
            Log.v(TAG, "Still in today: $today")
        } else {
            // Reset data
            Log.v(TAG, "Change to new day is: $today")
            resetData()
            myWeek = databasePreference!!.updateSpecifyDay(myWeek!!, today,0)
        }
    }
}