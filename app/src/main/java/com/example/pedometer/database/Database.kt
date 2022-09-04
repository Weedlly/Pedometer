package com.example.pedometer.database

import android.content.Context
import android.content.ContextWrapper
import java.io.File
import java.io.FileOutputStream


class Database (val context: Context){
//    private val fileName : String = "PedometerData.txt"
////    private val filename = "internalStorage.txt"
//    var file= File(fileName)
//    fun createNewFile(){
//        if (!file.exists()){
//            var fileOutput = context.openFileOutput(fileName,Context.MODE_PRIVATE)
//            val rd = Random(10000).toString()
//            println("Random key: $rd")
//            fileOutput.write(rd.toInt())
//            fileOutput.close()
//
//            //Create new user key in Firestore
//
//        }
//    }
//    fun readFile(){
//        if (file.exists()){
//            val key = file.readText()
//            println("Exist key: $key")
//
//            // Find data in Firestore
//
//        }
//    }
private var filename: String? = "internalStorage.txt"

    //Thư mục do mình đặt
    private val filepath = "ThuMucCuaToi"
    var myInternalFile: File? = null
    var  contextWrapper =  ContextWrapper(context)
    //Tạo (Hoặc là mở file nếu nó đã tồn tại) Trong bộ nhớ trong có thư mục là ThuMucCuaToi.
    var directory = contextWrapper.getDir(filepath,Context.MODE_PRIVATE)

    fun save() {
        myInternalFile = File(directory, filename)
        val fos = FileOutputStream(myInternalFile)
        //Ghi dữ liệu vào file
        //Ghi dữ liệu vào file
        var myInputText = "1234"
        fos.write(myInputText.toInt())
        fos.close()
    }
}