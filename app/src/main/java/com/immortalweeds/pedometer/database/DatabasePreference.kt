package com.immortalweeds.pedometer.database

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.immortalweeds.pedometer.model.countstep.Week

class DatabasePreference (context: Context){
    @SuppressLint("HardwareIds")
    var deviceId: String =  Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID)

    fun initData(targetStep: Int) : Week {
        return Week(
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
    }
//        // Update the  specify day
//        fun updateWeekToFireStore(newWeek: Week){
//
//            db.collection("Week").whereEqualTo("deviceId", deviceId)
//                .get().addOnSuccessListener {
//                    if (it.documents.isNotEmpty()) {
//
//                        docId = it.documents[0].id
//                        Log.v(TAG, "DocId: ${it.documents[0].id}")
//
//                        db.collection("Week").document(docId!!).delete()
//
//                        db.collection("Week").add(newWeek)
//                            .addOnSuccessListener {
//                                Log.v(TAG, "Update data successful")
//                            }
//                    }else{
//                        Log.v(TAG, "Update failed : Data not exist")
//                    }
//                }
//        }


    fun getStepSpecifyDay(myWeek :Week, dayNumber: Int) : Int{
         return when (dayNumber){
            1 ->{
                 myWeek.sun!!
            }
            2 ->{
                myWeek.mon!!
            }
            3 ->{
                myWeek.tue!!
            }
            4 ->{
                myWeek.wed!!
            }
            5 ->{
                myWeek.thu!!
            }
            6 ->{
                myWeek.fri!!
            }
            7->{
                myWeek.sat!!
            }
            else -> 0
        }
    }
    fun updateSpecifyDay(myWeek: Week,dayNumber: Int, step :Int) : Week {
        return when (dayNumber){
            1 ->{
                myWeek.sun = step
                myWeek
            }
            2 -> {
                myWeek.mon = step
                myWeek
            }
            3 -> {
                myWeek.tue = step
                myWeek
            }
            4 -> {
                myWeek.wed = step
                myWeek
            }
            5 -> {
                myWeek.thu = step
                myWeek
            }
            6 -> {
                myWeek.fri = step
                myWeek
            }
            7 -> {
                myWeek.sat = step
                myWeek
            }
            else -> myWeek
        }
    }
    fun plusStepSpecificDay(myWeek: Week, dayNumber: Int, step :Int) : Week {
        return when (dayNumber){
            1 -> {
                myWeek.sun = myWeek.sun!!.plus(step)
                myWeek
            }
            2 -> {
                myWeek.mon = myWeek.mon!!.plus(step)
                myWeek
            }
            3 -> {
                myWeek.tue = myWeek.tue!!.plus(step)
                myWeek
            }
            4 -> {
                myWeek.wed = myWeek.wed!!.plus(step)
                myWeek
            }
            5 -> {
                myWeek.thu = myWeek.thu!!.plus(step)
                myWeek
            }
            6 -> {
                myWeek.fri = myWeek.fri!!.plus(step)
                myWeek
            }
            7-> {
                myWeek.sat = myWeek.sat!!.plus(step)
                myWeek
            }
            else -> myWeek
        }
    }
}