package com.example.pedometer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.example.pedometer.database.DatabaseAPI
import com.example.pedometer.database.db
import com.example.pedometer.databinding.AbsLayoutBinding
import com.example.pedometer.databinding.ActivityUserSetupBinding
import com.example.pedometer.model.countstep.Week
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.ktx.toObjects
import java.util.*

class UserSetup : AppCompatActivity() {
    private var database : DatabaseAPI? = null
    private var deviceId : String? = null
    private var binding : ActivityUserSetupBinding? = null
    private var absBinding : AbsLayoutBinding? = null

    private var maxSteps: Float? = 0f
    private var calories: Float? = 0f
    private var isRegister : Boolean = false
    private var myWeek : Week? = null

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
        database = DatabaseAPI(baseContext)
        deviceId = database!!.deviceId
        Log.v("User","Get key success: $deviceId")

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
        // Save day
        saveData()

        Toast.makeText(this, "Pause!!!", Toast.LENGTH_SHORT).show()
        myWeek!!.stepPerDay = maxSteps!!.toInt()

        database!!.updateTargetStepToFireStore(maxSteps!!.toInt())
        Log.v(TAG, "Activity on pause, data updating!!!")
    }
    private fun isTargetFill(){
        db.collection("Week").whereEqualTo("deviceId",deviceId)
            .get().addOnSuccessListener {
                if (it.documents.isEmpty()){
                    Log.v("User","Init user data")
                    database!!.initData(0)
                    myWeek = Week(database!!.deviceId)
                    bottomNavigationVisible(false)
                }
                else{
                    Log.v("User","User data is exist")
                    myWeek = it.toObjects<Week>()[0]
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
                }
                checkNewDay()
            }
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
                    val gpsMapIntent = Intent(this, GpsMap::class.java)
                    gpsMapIntent.putExtra("myWeek", myWeek)
                    startActivity(gpsMapIntent)
                }
                R.id.home-> {
                    val countStepIntent = Intent(this, CountStep::class.java)
                    countStepIntent.putExtra("myWeek", myWeek)
                    startActivity(countStepIntent)
                }
            }
            true
        }
    }
    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        val editor = sharedPreferences.edit()
        // Save day
        val today = getWeekday(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        editor.putString("Today", today)
        Log.v(TAG, "Today save is: $today")
        editor.apply()

    }
    private fun checkNewDay() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val oldDay = sharedPreferences.getString("Today", "")
        Log.v(TAG, "Old day: $oldDay")
        val today = getWeekday(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        if (oldDay == today) {
            Log.v(TAG, "Still in today: $today")
        } else {
            // Reset data
            Log.v(TAG, "Change to new day is: $today")
            myWeek = database!!.updateSpecifyDayOnWeek(myWeek!!,today!!,0)
        }
    }
}