package com.example.pedometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import com.example.pedometer.BuildConfig.MAPS_API_KEY
import com.example.pedometer.database.DatabaseAPI
import com.example.pedometer.databinding.AbsLayoutBinding
import com.example.pedometer.databinding.ActivityGpsMapBinding
import com.example.pedometer.model.countstep.Week
import com.example.pedometer.model.gps.Route
import com.example.pedometer.network.ApiInterface
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.nlopez.smartlocation.SmartLocation
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

private const val baseUrl = "https://api.mapbox.com"

const val KILOMETER_TO_CALORIE = 84
class GpsMap : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityGpsMapBinding
    private var absBinding : AbsLayoutBinding? = null

    private var markerPoints = ArrayList<LatLng>()
    private var totalDistanceRunning :Float = 0f
    private var lastDistance :Float? = -1f
    private var mGoogleMap: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null

    // The entry point to the Places API.
    private lateinit var placesClient: PlacesClient

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var locationPermissionGranted = false

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.

    private var lastKnownLocation: Location? = null
    private var desti: LatLng? = null

    private var isEnoughTwoPoint = false

    private var isUpdateLocation = false

    // Data
    private var myWeek : Week? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGpsMapBinding.inflate(layoutInflater)

        setContentView(binding.root)

        //Setup Activity Custom action bar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        absBinding = AbsLayoutBinding.inflate(layoutInflater)
        supportActionBar!!.customView = absBinding!!.root
        absBinding!!.activityTitleTv.text = baseContext.resources.getString(R.string.title_activity_gps_map)
        absBinding!!.activityTitleTv.textSize = 19f

        //Set up title and init value
        setupTitle()
        updateTravelInfo(0.0,0.0)

        // Take data week
        myWeek = intent.getSerializableExtra("myWeek") as Week

        // Update continuous location
        CoroutineScope(Dispatchers.IO).launch{
            while (true) {
                delay(5000)
                SmartLocation.with(baseContext).location().start{
                    lastKnownLocation = it
                    if (isUpdateLocation) {
                        lastKnownLocation!!.latitude = desti!!.latitude
                        lastKnownLocation!!.longitude = desti!!.longitude
                    }
                    if (locationPermissionGranted && isUpdateLocation) {
                        updateTrainingRoute(LatLng(lastKnownLocation!!.latitude,lastKnownLocation!!.longitude))
                        Log.v(TAG, "Update location: $lastKnownLocation")
                    }
                }
            }
        }
        // Press Start Button to show Stop and Pause Button, also active GPS Training
        binding.startBt.setOnClickListener {

            if (isEnoughTwoPoint) {
                isUpdateLocation = true
                binding.startBt.isVisible = false
                binding.stopBt.isVisible = true
                binding.pauseBt.isVisible = true
            }
            else {
                // Requiring choose destination point to start
                Toast.makeText(this, "Choosing location you want to moving ", Toast.LENGTH_SHORT)
                    .show()
                // Focus my location
                handleMyLocation()
            }
        }

        // Stopping Gps training then show Start button
        binding.stopBt.setOnClickListener {
            handleMyLocation()
            isUpdateLocation = false
            binding.stopBt.isVisible = false
            binding.pauseBt.isVisible = false
            binding.startBt.isVisible = true
        }

        binding.pauseBt.setOnClickListener {
            if (binding.pauseBt.text == "PAUSE") {
                isUpdateLocation = false
                binding.pauseBt.text = getString(R.string.continue_button)
            }
            // binding.pauseBt.text = CONTINUE
            else{
                isUpdateLocation = true
                updateTrainingRoute(LatLng(lastKnownLocation!!.latitude,lastKnownLocation!!.longitude))
                binding.pauseBt.text = getString(R.string.pause_button)
            }
        }

        //Bottom navigation
        bottomNavigationHandle()

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync (this)
        // Construct a PlacesClient
        Places.initialize(applicationContext,MAPS_API_KEY)
        placesClient = Places.createClient(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.mGoogleMap = googleMap

        // Prompt the user for permission.
        getLocationPermission()

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()

        // Get the current location of the device and set the position of the map.
        getDeviceLocation()

        mGoogleMap?.setOnMyLocationButtonClickListener {
            handleMyLocation()
        }

        mGoogleMap!!.setOnMapClickListener {
            setupTrainingRoute(it)
        }
    }
    private fun bottomNavigationHandle(){
        val bottomNavigationView : BottomNavigationView = binding.bottomNavigation

        binding.bottomNavigation.menu[0].isChecked = true
        bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId){
                R.id.home-> {
                    val countStepIntent = Intent(this, CountStep::class.java)
                    countStepIntent.putExtra("myWeek", myWeek)
                    startActivity(countStepIntent)
                }
                R.id.target-> {
                    val intentUserSetup = Intent(this,UserSetup::class.java)
                    intentUserSetup.putExtra("isRegister",false)
                    startActivity(intentUserSetup)
                }
            }
            true
        }
    }


    private fun setupTrainingRoute(it: LatLng){
        if (markerPoints.size > 1){
            markerPoints.clear()
            mGoogleMap!!.clear()
            isEnoughTwoPoint = false
        }

        // Adding new item to the ArrayList
        markerPoints.add(it)
        // Creating MarkerOptions
        val markerOptions = MarkerOptions()
        // Setting the position of the marker
        markerOptions.position(it)


        if  (!it.equals(lastKnownLocation)){
            // Only Des in Lat Array
            if (markerPoints.size == 1) {
                handleMyLocation()
                // Adding new item to the ArrayList
                markerPoints.add(it)

                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                Log.v(TAG,"Array point size : ${markerPoints.size}")
            }
            // Enough 2 point in Lat Array
            else if (markerPoints.size == 2){
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            }
        }

        // Add new marker to the Google Map Android API V2
        mGoogleMap!!.addMarker(markerOptions)
        // Checks, whether start and end locations are captured
        if (markerPoints.size >= 2){
            isEnoughTwoPoint = true
            val origin = markerPoints[0]
            val dest = markerPoints[1]
            desti = dest
            val polylineOptions = PolylineOptions()

            // Print URL to the Google Directions API
            Log.v(TAG,"Direction API Url : https://api.mapbox.com/directions/v5/mapbox/walking/" +
                    "${origin.longitude}%2C" +
                    "${origin.latitude}%3B" +
                    "${dest.longitude}%2C" +
                    "${dest.latitude}?alternatives=false&geometries=geojson&overview=simplified&steps=false&access_token=pk.eyJ1Ijoid2VlZGx5IiwiYSI6ImNsN2VpMW56bjAwa2gzbnBnaHd2MjJmZGYifQ.It2pYYoWNWQ-9Ogs49OUMg"
            )
            val route = getDirection(origin,dest)
            val coordinates = route.geometry.coordinates

            var nextStep = origin
            for (step in coordinates){
                val lng = LatLng(step[1],step[0])
                polylineOptions.add(nextStep,lng)
                nextStep = lng
            }

            polylineOptions.add(nextStep,dest)
            mGoogleMap!!.addPolyline(polylineOptions)

            updateTravelInfo(route.distance,route.duration)
        }
    }

    private fun updateTrainingRoute(it: LatLng){
        var dest : LatLng = it
        if (markerPoints.size > 1){
            dest = markerPoints[1]
            markerPoints.clear()
            mGoogleMap!!.clear()
            isEnoughTwoPoint = true
        }
        // Adding new item to the ArrayList
        markerPoints.add(it)
        markerPoints.add(dest)
        // Creating MarkerOptions
        val markerOptionsDest = MarkerOptions()
        val markerOptionsCurrent = MarkerOptions()
        // Setting the position of the marker
        markerOptionsCurrent.position(it).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        markerOptionsDest.position(dest).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

        Log.v(TAG,"Array point size : ${markerPoints.size}")

        // Add new marker to the Google Map Android API V2
        mGoogleMap!!.addMarker(markerOptionsCurrent)

        mGoogleMap!!.addMarker(markerOptionsDest)
        // Checks, whether start and end locations are captured
        if (markerPoints.size >= 2){
            isEnoughTwoPoint = true
            val origin = markerPoints[0]
            dest = markerPoints[1]

            val polylineOptions = PolylineOptions()

            // Print URL to the Google Directions API
            Log.v(TAG,"Direction API Url : https://api.mapbox.com/directions/v5/mapbox/walking/" +
                    "${origin.longitude}%2C" +
                    "${origin.latitude}%3B" +
                    "${dest.longitude}%2C" +
                    "${dest.latitude}?alternatives=false&geometries=geojson&overview=simplified&steps=false&access_token=pk.eyJ1Ijoid2VlZGx5IiwiYSI6ImNsN2VpMW56bjAwa2gzbnBnaHd2MjJmZGYifQ.It2pYYoWNWQ-9Ogs49OUMg"
            )
            val route = getDirection(origin,dest)

            val currentDistance = route.distance.toFloat()
            if (lastDistance == -1f){
                lastDistance = currentDistance
            }
            else if (currentDistance == 0f){
                Toast.makeText(this, "Congratulating you finished journey ", Toast.LENGTH_SHORT)
                    .show()
                // Focus my location
                handleMyLocation()
                isUpdateLocation = false
                binding.stopBt.isVisible = false
                binding.pauseBt.isVisible = false
                binding.startBt.isVisible = true
                val sharedPreferences = getSharedPreferences("myPrefs",Context.MODE_PRIVATE)
                val oldDay = sharedPreferences.getString("Today","")
                Log.v(TAG,"Old day: $oldDay")
                val today = getWeekday(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
                if (oldDay == today){
                    Log.v(TAG,"Still in today: $today")
                    val newDayStep =(
                            (totalDistanceRunning * 1000) / FOOT_TO_METER).toInt() +
                            DatabaseAPI(baseContext).getStepSpecifyDay(myWeek!!,today!!)

                    DatabaseAPI(baseContext).updateSpecifyDay(
                        today,
                        newDayStep
                    )
                    myWeek = DatabaseAPI(baseContext).updateWeek(
                        myWeek!!,
                        today,
                        newDayStep
                    )
                }
                else {
                    // Reset data
                    Log.v(TAG, "Change to new day is: $today")
                    val newDayStep =((totalDistanceRunning / 1000) *
                            KILOMETER_TO_CALORIE).toInt()
                    DatabaseAPI(baseContext).updateSpecifyDay(today!!,
                        ((totalDistanceRunning * 1000) / FOOT_TO_METER).toInt()
                    )
                    myWeek = DatabaseAPI(baseContext).updateWeek(
                        myWeek!!,
                        today,
                        newDayStep
                    )
                }
                totalDistanceRunning = 0f
            }else{
                totalDistanceRunning += lastDistance!! - currentDistance
                Log.v(TAG,"totalDistanceRunning : $totalDistanceRunning")
            }

            val coordinates = route.geometry.coordinates

            var nextStep = origin
            for (step in coordinates){
                val lng = LatLng(step[1],step[0])
                polylineOptions.add(nextStep,lng)
                nextStep = lng
            }

            polylineOptions.add(nextStep,dest)
            mGoogleMap!!.addPolyline(polylineOptions)

            updateTravelInfo(route.distance,route.duration)
        }
    }

    private fun getDirection(origin: LatLng ,dest: LatLng) : Route {
        val retrofit = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build()
        val service: ApiInterface by lazy { retrofit.create(ApiInterface::class.java)}
        var route : Route?

        route = runBlocking {
            withContext(Dispatchers.Default) {
                val routes = service.getPlaces(
                    origin.longitude,
                    origin.latitude,
                    dest.longitude,
                    dest.latitude
                )
                route = routes.routes[0]
            }
            route
        }
        return route!!
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mGoogleMap?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
    }
    private fun getLocationPermission(){
        if (ContextCompat.checkSelfPermission(this.applicationContext
                ,Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED){
            locationPermissionGranted = true
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }

    }

    // Handle my location action
    private fun handleMyLocation() : Boolean{
            val latLng = LatLng(lastKnownLocation!!.latitude,lastKnownLocation!!.longitude)
            Log.v(TAG,"Current location: $lastKnownLocation")
            mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15f))
            val markerOptions = MarkerOptions()
            if (markerPoints.isEmpty()) {
                markerPoints.add(latLng)
            }
            else {
                markerPoints.clear()
                mGoogleMap!!.clear()
                markerPoints.add(latLng)
            }
            markerOptions.position(latLng)
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            mGoogleMap!!.addMarker(markerOptions.title("You are here"))

            isEnoughTwoPoint = false
            return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false
        when(requestCode){
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    locationPermissionGranted = true
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        updateLocationUI()
    }

    private fun setupTitle(){
        binding.distanceCtv.titleTv.text = getString(R.string.distance_title)
        binding.distanceCtv.contentTv.text = "0"
        binding.durationCtv.titleTv.text = getString(R.string.duration_title)
        binding.durationCtv.contentTv.text = "0"
        binding.caloriesCtv.titleTv.text = getString(R.string.calorie_title)
        binding.caloriesCtv.contentTv.text = "0"
    }
    private fun updateTravelInfo(distance : Double, duration : Double){
        val kilometer = distance / 1000
        val hours = (duration / 3600).toInt()
        val minus = ((duration - hours * 3600) / 60).toInt()
        val second = (duration - hours * 3600 - minus * 60).toInt()
        binding.distanceCtv.contentTv.text = baseContext.resources.getString(R.string.distances,kilometer)
        binding.caloriesCtv.contentTv.text = baseContext.resources.getString(R.string.calories,kilometer * KILOMETER_TO_CALORIE)
        binding.durationCtv.contentTv.text = baseContext.resources.getString(R.string.duration,
            hours,
            minus,
            second)
    }


    @SuppressLint("MissingPermission")
    private fun updateLocationUI(){
        if (mGoogleMap == null){
            return
        }
        try{
            if (locationPermissionGranted){
                mGoogleMap?.isMyLocationEnabled = true
                mGoogleMap?.uiSettings?.isMyLocationButtonEnabled = true
            }else{
                mGoogleMap?.isMyLocationEnabled = false
                mGoogleMap?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        }
        catch (e :SecurityException){
            Log.e("Exception: %s",e.message,e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        try {
            if(locationPermissionGranted){
                Log.v(TAG,"Get current location")
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this){
                    task ->
                    if(task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            Log.v(TAG,"lastKnownLocation :$lastKnownLocation")
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
        }
        catch (e : SecurityException){
            Log.e("Exception %e",e.message,e)
        }
    }

    override fun onStop() {
        SmartLocation.with(baseContext).location().stop()
    super.onStop()
}
    companion object {
        private val TAG = GpsMap::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        // [START maps_current_place_state_keys]
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"

    }
}