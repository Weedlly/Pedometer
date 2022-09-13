package com.example.pedometer.database

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.example.pedometer.model.countstep.Week
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase

@SuppressLint("StaticFieldLeak")
val db = Firebase.firestore
class DatabaseAPI (context: Context) {
    companion object {
        const val TAG = "Database"
    }
    @SuppressLint("HardwareIds")
    var deviceId: String =  Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID)

    private var docId: String? = null

    fun initData(targetStep: Int) {
        db.collection("Week").add(
            Week(
                deviceId,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                targetStep
            )
        )
            .addOnSuccessListener {
                Log.v(TAG,"Init data successful")
            }
    }
    fun updateSpecifyDay(dayName: String, step: Int){

        db.collection("Week").whereEqualTo("deviceId", deviceId)
            .get().addOnSuccessListener {
                if (it.documents.isNotEmpty()) {

                    docId = it.documents[0].id
                    Log.v(TAG, "DocId: ${it.documents[0].id}")

                    val oldWeek = it.toObjects<Week>()[0]
                    when (dayName){
                        "Monday"->{
                            oldWeek.mon = step
                        }
                        "Tuesday"->{
                            oldWeek.tue = step
                        }
                        "Wednesday"->{
                            oldWeek.wed = step
                        }
                        "Thursday"->{
                            oldWeek.thu = step
                        }
                        "Friday"->{
                            oldWeek.fri = step
                        }
                        "Saturday"->{
                            oldWeek.sat = step
                        }
                        "Sunday"->{
                            oldWeek.sun = step
                        }
                    }

                    db.collection("Week").document(docId!!).delete()

                    db.collection("Week").add(oldWeek)
                        .addOnSuccessListener {
                            Log.v(TAG, "Update data successful")
                        }
                }
            }
    }
    fun updateTargetStep(targetStep: Int){
        db.collection("Week").whereEqualTo("deviceId", deviceId)
            .get().addOnSuccessListener {
                if (it.documents.isNotEmpty()) {

                    docId = it.documents[0].id
                    Log.v(TAG, "DocId: ${it.documents[0].id}")

                    val oldWeek = it.toObjects<Week>()[0]
                    oldWeek.stepPerDay = targetStep

                    db.collection("Week").document(docId!!).delete()

                    db.collection("Week").add(oldWeek)
                        .addOnSuccessListener {
                            Log.v(TAG, "Update data successful")
                        }
                }
            }
    }
    fun getStepSpecifyDay(week: Week,dayName: String) : Int{
        return when (dayName){
            "Monday"->{
                week.mon!!
            }
            "Tuesday"->{
                week.tue!!
            }
            "Wednesday"->{
                week.wed!!
            }
            "Thursday"->{
                week.thu!!
            }
            "Friday"->{
                week.fri!!
            }
            "Saturday"->{
                week.sat!!
            }
            "Sunday"->{
                week.sun!!
            }
            else -> 0
        }
    }
    fun updateWeek(week: Week,dayName: String, step :Int) : Week{
        return when (dayName){
            "Monday" -> {
                week.mon!!.plus(step)
                return week
            }
            "Tuesday" -> {
                week.tue!!.plus(step)
                return week
            }
            "Wednesday" -> {
                week.wed!!.plus(step)
                return week
            }
            "Thursday" -> {
                week.thu!!.plus(step)
                return week
            }
            "Friday" -> {
                week.fri!!.plus(step)
                return week
            }
            "Saturday" -> {
                week.sat!!.plus(step)
                return week
            }
            "Sunday" -> {
                week.sun!!.plus(step)
                return week
            }
            else -> week
        }
    }
}