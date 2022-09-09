package com.example.pedometer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.example.pedometer.database.DatabaseAPI
import com.example.pedometer.database.db
import com.example.pedometer.databinding.ActivityUserSetupBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class UserSetup : AppCompatActivity() {
    private var database : DatabaseAPI? = null
    private var deviceId : String? = null
    private var binding : ActivityUserSetupBinding? = null

    private var maxSteps: Float? = 0f
    private var calories: Float? = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSetupBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setCustomView(R.layout.abs_layout)
        supportActionBar!!.title = "YOUR TARGET"
        //Bottom navigation
        bottomNavigationHandle()

        //Setup database
        database = DatabaseAPI(baseContext)
        deviceId = database!!.deviceId

        isTargetFill()
        Log.v("User","Get key success: $deviceId")

        binding!!.totalMaxStepEv.setOnClickListener {
            maxSteps = binding!!.totalMaxStepEv.text.toString().toFloat()
            calories = (maxSteps!!.toFloat() * FOOT_TO_CALORIE).toFloat()
            binding!!.totalCaloriesEv.setText(calories!!.toInt().toString())
        }
        binding!!.totalCaloriesEv.setOnClickListener {
            calories = binding!!.totalCaloriesEv.text.toString().toFloat()
            maxSteps = (calories!!.toFloat() / FOOT_TO_CALORIE ).toFloat()
            binding!!.totalMaxStepEv.setText(maxSteps!!.toInt().toString())
        }
    }
    private fun isTargetFill(){
        db.collection("Week").whereEqualTo("deviceId",deviceId)
            .get().addOnSuccessListener {
                if (it.documents.isEmpty()){
                    database!!.initData(0)
                }
            }
    }
    private fun bottomNavigationHandle(){
        val bottomNavigationView : BottomNavigationView = binding!!.bottomNavigation

        binding!!.bottomNavigation.menu[2].isChecked = true
        bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId){
                R.id.gps_training-> {
                    startActivity(Intent(this,GpsMap::class.java))
                }
                R.id.home-> {
                    startActivity(Intent(this,CountStep::class.java))
                }
            }
            true
        }
    }
}