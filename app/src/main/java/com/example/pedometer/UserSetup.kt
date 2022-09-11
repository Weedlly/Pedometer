package com.example.pedometer

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

class UserSetup : AppCompatActivity() {
    private var database : DatabaseAPI? = null
    private var deviceId : String? = null
    private var binding : ActivityUserSetupBinding? = null
    private var absBinding : AbsLayoutBinding? = null

    private var maxSteps: Float? = 0f
    private var calories: Float? = 0f
    private var isRegister : Boolean = false
    private var myWeek : Week? = null
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
    private fun isTargetFill(){
        db.collection("Week").whereEqualTo("deviceId",deviceId)
            .get().addOnSuccessListener {
                if (it.documents.isEmpty()){
                    database!!.initData(0)
                }
                else{
                    myWeek = it.toObjects<Week>()[0]
                    if(isRegister) {
                        val countStepIntent = Intent(this, CountStep::class.java)
                        countStepIntent.putExtra("myWeek", myWeek)
                        startActivity(countStepIntent)
                    }
                }
            }
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
}