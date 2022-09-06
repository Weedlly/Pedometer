package com.example.pedometer.database

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.example.pedometer.model.countstep.Week
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import java.io.File
import kotlin.random.Random
val db = Firebase.firestore
class Database (context: Context) {
    companion object {
        const val TAG = "Database"

    }

    private val filename: String = "my_key.txt"
    private val filepath = "identify_key"
    private var myInternalFile: File? = null
    private var contextWrapper = ContextWrapper(context)

    //Create or open new directory in internal storage
    private val directory = contextWrapper.getDir(filepath, Context.MODE_PRIVATE)
    private var myKey: Int? = 0
    private var docId: String? = null

    fun isKeyExist() : Boolean {
        if (!File(directory, filename).exists()) {
            Log.v(TAG, "File is not exists")
            createMyKey()
        }
        return true
    }

    private fun deleteDirectory() {
        myInternalFile = File(directory, filename)
        myInternalFile!!.delete()
        Log.v(TAG, "File is deleted")
    }

    private fun createMyKey() {
        myInternalFile = File(directory, filename)

        var isExistInFirestore = true

        db.collection("Week")
            .get().addOnSuccessListener {
                if (it.documents.isNotEmpty()) {
                    val listWeek: List<Week> = it.toObjects()
                    var key = Random.nextInt(0, 10000)
                    while (isExistInFirestore) {
                        isExistInFirestore = false
                        listWeek.forEach { week ->
                            if (week.key == key) {
                                isExistInFirestore = true
                            }
                        }
                        key = Random.nextInt(0, 10000)
                    }
                    Log.v(TAG, "My create key: $key")
                    myInternalFile!!.writeText(key.toString())
                    initData(key)
                }
            }
    }

    fun getMyKey() : Int {
        myInternalFile = File(directory, filename)
        myKey = myInternalFile!!.readText().toInt()
        Log.v(TAG, "My key: $myKey")
        return myKey!!
    }

    private fun initData(newKey: Int) {
        db.collection("Week").add(
            Week(
                newKey,
                0,
                0,
                0,
                0,
                0,
                0,
                0
            )
        )
            .addOnSuccessListener {
                Log.v(TAG,"Init data successful")
            }
    }
    fun updateData(key : Int ,dayName: String,step: Int){
        db.collection("Week").whereEqualTo("key", key)
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
                    db.collection("Week").add(
                        oldWeek
                    )
                        .addOnSuccessListener {
                            Log.v(TAG, "Update data successful")
                        }
                }
            }
    }
}