

package com.immortalweeds.pedometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.telephony.CarrierConfigManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.immortalweeds.pedometer.BuildConfig.MAPS_API_KEY
import com.immortalweeds.pedometer.GpsMapTest.Companion.PERMISSIONS_REQUEST_ACCESS_ACTIVITY_RECOGNITION
import com.immortalweeds.pedometer.database.DatabasePreference
import com.immortalweeds.pedometer.databinding.AbsLayoutBinding
import com.immortalweeds.pedometer.databinding.FragmentGgMapTestBinding
import com.immortalweeds.pedometer.model.countstep.Week

private const val baseUrl = "https://api.mapbox.com"


class GpsMapTest : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: FragmentGgMapTestBinding

    private var absBinding : AbsLayoutBinding? = null

    private var markerPoints = ArrayList<LatLng>()
    private var totalStep : Float = 0f

    private var mGoogleMap: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null

    // The entry point to the Places API.
    private lateinit var placesClient: PlacesClient

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var locationPermissionGranted = false
    private var activityRecognitionGranted = false

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.

    private var lastKnownLocation: Location? = null
    private var desti: LatLng? = null

    private var isEnoughTwoPoint = false

    private var isUpdateLocation = false

    // Counter step
    private var sensorManager : SensorManager? = null

    // Data
    private var databasePreference : DatabasePreference? = null
    private var myWeek : Week? = null
    private var today : Int? = null

    override fun onMapReady(googleMap: GoogleMap) {
        this.mGoogleMap = googleMap
        Log.v("permiss","1")
        getLocationPermission()
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        Log.v("permiss","getDeviceLocation")
        try {
            val locationResult = fusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(this){
                    task ->
                if(task.isSuccessful) {
                    // Set the map's camera position to the current location of the device.
                    lastKnownLocation = task.result
                    if (lastKnownLocation != null) {

                        mGoogleMap?.moveCamera(
                            (CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude
                                ), DEFAULT_ZOOM.toFloat()
                            ))
                        )
                    }
                }
            }
        }
        catch (e : SecurityException){
            Log.e("Exception %e",e.message,e)
        }
    }

    private fun getLocationPermission(){
        Log.v("permiss","2")
        if (ContextCompat.checkSelfPermission(this.applicationContext
                , Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED){
            Log.v("permiss","PERMISSION_GRANTED")
            locationPermissionGranted = true
        }else{

            Log.v("permiss","PERMISSION_DENNIE")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                GpsMapTest.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }

    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when(requestCode){
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                Log.v("permiss","code PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION")
                // If request is cancelled, the result arrays are empty.
                locationPermissionGranted = false
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.v("permiss","accpect")

                    locationPermissionGranted = true
//                    getDeviceLocation()
                }
                else{
                    Log.v("permiss","not accpect")
                    onPause()
                }

            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
//        updateLocationUI()
    }
//    @SuppressLint("MissingPermission")
//    private fun updateLocationUI(){
//        Log.v("permiss","updateLocationUI")
//        if (mGoogleMap == null){
//            return
//        }
//        try{
//            if (locationPermissionGranted){
//                mGoogleMap?.isMyLocationEnabled = true
//                mGoogleMap?.uiSettings?.isMyLocationButtonEnabled = true
//            }else{
//                mGoogleMap?.isMyLocationEnabled = false
//                mGoogleMap?.uiSettings?.isMyLocationButtonEnabled = true
//                lastKnownLocation = null
//                getLocationPermission()
//            }
//        }
//        catch (e :SecurityException){
//            Log.e("Exception: %s",e.message,e)
//        }
//    }
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v("permiss","0")
        binding = FragmentGgMapTestBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.maptest) as? SupportMapFragment
        mapFragment?.getMapAsync (this)
        // Construct a PlacesClient
        Places.initialize(applicationContext,MAPS_API_KEY)
        placesClient = Places.createClient(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

    }


    companion object {
        private val TAG = GpsMap::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_ACTIVITY_RECOGNITION = 2
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        // [START maps_current_place_state_keys]
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"

    }
}