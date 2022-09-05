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

class Database (context: Context) {
    companion object {
        const val TAG = "Database"

    }

    private val db = Firebase.firestore

    private val filename: String = "my_key.txt"
    private val filepath = "identify_key"
    private var myInternalFile: File? = null
    private var contextWrapper = ContextWrapper(context)

    //Create or open new directory in internal storage
    private val directory = contextWrapper.getDir(filepath, Context.MODE_PRIVATE)
    private var myKey: Int? = 0
    private var docId: String? = null

    fun isExist() {
        if (File(directory, filename).exists()) {
            Log.v(TAG, "File is exists")
            readMyKey()
        } else {
            Log.v(TAG, "File is not exists")
            createMyKey()
        }
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

    private fun readMyKey() {
        myInternalFile = File(directory, filename)
        myKey = myInternalFile!!.readText().toInt()
        Log.v(TAG, "My key: $myKey")
        updateData()

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

    private fun updateData(){
        db.collection("Week").whereEqualTo("key", myKey)
            .get().addOnSuccessListener {
                if (it.documents.isNotEmpty()) {
                    docId = it.documents[0].id
                    Log.v(TAG, "DocId: ${it.documents[0].id}")
                    db.collection("Week").document(docId!!).delete()
                    db.collection("Week").add(
                        Week(
                            myKey!!,
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
                            Log.v(TAG, "Update data successful")
                        }
                }
            }
    }
}