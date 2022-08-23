package com.example.pedometer

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.IBinder


class GPSTracker () : Service() {
    
    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}