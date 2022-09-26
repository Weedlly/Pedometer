

package com.immortalweeds.pedometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.immortalweeds.pedometer.BuildConfig.MAPS_API_KEY
import com.immortalweeds.pedometer.database.DatabasePreference
import com.immortalweeds.pedometer.databinding.AbsLayoutBinding
import com.immortalweeds.pedometer.databinding.ActivityGpsMapBinding
import com.immortalweeds.pedometer.model.countstep.Week
import com.immortalweeds.pedometer.model.gps.Route
import com.immortalweeds.pedometer.network.ApiInterface
import io.nlopez.smartlocation.SmartLocation
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

private const val baseUrl = "https://api.mapbox.com"

const val KILOMETER_TO_CALORIE = 84
class GpsMap : AppCompatActivity(), SensorEventListener, OnMapReadyCallback {

    private lateinit var binding: ActivityGpsMapBinding

    private var absBinding : AbsLayoutBinding? = null

    private var markerPoints = ArrayList<LatLng>()
    private var totalStep : Float = 0f

    private var mGoogleMap: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null

    // The entry point to the Places API.
    private lateinit var placesClient: PlacesClient

    //Permission
    private var locationPermissionGranted = false

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
    private var myWeek : Week? = Week()
    private var today : Int? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGpsMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Setup Activity Custom action bar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        absBinding = AbsLayoutBinding.inflate(layoutInflater)
        supportActionBar!!.customView = absBinding!!.root
        absBinding!!.activityTitleTv.text =
            baseContext.resources.getString(R.string.title_activity_gps_map)

        //Bottom navigation
        bottomNavigationHandle()

        //Set up title and init value
        setupTitle()
        updateJourneyInformation(0.0, 0.0)

        // Init database
        today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        databasePreference = DatabasePreference(baseContext)
        myWeek = databasePreference!!.initData(0)
        loadWeekData()
        Log.v(TAG, "GPS take week : $myWeek")

        // button Start, Stop, Pause
        buttonControlListener()

        // savedInstanceState
        if (savedInstanceState != null) {
            Log.v(TAG,"saveInstance :$savedInstanceState")
            lastKnownLocation = if (Build.VERSION.SDK_INT >= 33)
                savedInstanceState.getParcelable(KEY_LOCATION, Location::class.java)
            else
                savedInstanceState.getParcelable(KEY_LOCATION)

            cameraPosition = if (Build.VERSION.SDK_INT >= 33)
                savedInstanceState.getParcelable(
                    KEY_CAMERA_POSITION,
                    CameraPosition::class.java
                )
            else
                savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }

        // Setup map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        // Construct a PlacesClient
        Places.initialize(applicationContext, MAPS_API_KEY)
        placesClient = Places.createClient(this)

        // Set up sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.mGoogleMap = googleMap
        // Prompt the user for permission.
        getLocationPermission()

        Log.v(TAG,"don't waiting")
        locationPermissionAccepted()
    }
    private fun locationPermissionAccepted(){
        if (locationPermissionGranted) {

            updateCurrentLocation()

            mGoogleMap?.setOnMyLocationButtonClickListener {
                handleMyLocation()
            }

            mGoogleMap!!.setOnMapClickListener {
                setupTrainingRoute(it)
            }
        }
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if (p0!!.sensor.type == Sensor.TYPE_STEP_DETECTOR){
            if (isUpdateLocation)
            {
                totalStep += p0.values[0]
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onResume() {
        super.onResume()

        val stepSensor : Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepSensor == null) {
            Toast.makeText(this, "No sensor detected on this device", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
            Log.v(TAG, "Start")
        }
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
                    locationPermissionAccepted()
                }
                else{
                    Toast.makeText(this,"This activity need GPS permission to use",Toast.LENGTH_SHORT).show()
                    val intentUserSetup = Intent(this,UserSetup::class.java)
                    intentUserSetup.putExtra("isRegister",false)
                    startActivity(intentUserSetup)
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    @SuppressLint("MissingPermission")
    private fun updateCurrentLocation(){
        // Start continuous location
        mGoogleMap!!.isMyLocationEnabled = true
        SmartLocation.with(baseContext).location().start{
            lastKnownLocation = it
            Log.v(TAG, "lastKnownLocation: $lastKnownLocation")
            focusCurrentLocation()
        }
        // Update continuous location
        CoroutineScope(Dispatchers.IO).launch{
            while (true) {
                // Get current location each 5 second
                delay(3000)
                Log.v(TAG, "isUpdateLocation $isUpdateLocation")
//                if (isUpdateLocation) {
//                    CoroutineScope(Dispatchers.Main).launch {
//                        lastKnownLocation = SmartLocation.with(baseContext).location().get().lastLocation
//                        lastKnownLocation!!.latitude = desti!!.latitude
//                        lastKnownLocation!!.longitude = desti!!.longitude
//                        updateTrainingRoute(
//                            LatLng(
//                                lastKnownLocation!!.latitude,
//                                lastKnownLocation!!.longitude
//                            )
//                        )
//                        Log.v(TAG, "Update location: $lastKnownLocation")
//                    }
//                }

                if ( isUpdateLocation) {
                    CoroutineScope(Dispatchers.Main).launch {
                        //get new current location
                        lastKnownLocation = SmartLocation.with(baseContext).location().get().lastLocation
                        updateTrainingRoute(
                            LatLng(
                                lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude
                            )
                        )
                        Log.v(TAG, "Update location: $lastKnownLocation")
                    }
                }
            }
        }
    }

    // Mark origin and dest point ,then draw line between them
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

            updateJourneyInformation(route.distance,route.duration)
        }
    }

    // Update current location by mark and draw again
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
        markerOptionsCurrent.position(it).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        markerOptionsDest.position(dest).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

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

            journeyFinishingListener(currentDistance)

            val coordinates = route.geometry.coordinates

            var nextStep = origin
            for (step in coordinates){
                val lng = LatLng(step[1],step[0])
                polylineOptions.add(nextStep,lng)
                nextStep = lng
            }

            polylineOptions.add(nextStep,dest)
            mGoogleMap!!.addPolyline(polylineOptions)

            updateJourneyInformation(route.distance,route.duration)
        }
    }
    private fun journeyFinishingListener(currentDistance : Float){
        if (currentDistance <= 10f){
            // Notify to user they finish journey
            Toast.makeText(this, "Congratulating you finished journey ", Toast.LENGTH_SHORT)
                .show()

            // Focus my location
            handleMyLocation()
            isUpdateLocation = false
            binding.stopBt.isVisible = false
            binding.pauseBt.isVisible = false
            binding.startBt.isVisible = true
            setupTitle()

            updateData()
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

    // Handle my location action
    private fun handleMyLocation() : Boolean{
        Log.v(TAG,"Current location: $lastKnownLocation")
        val latLng = LatLng(lastKnownLocation!!.latitude,lastKnownLocation!!.longitude)

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

    private fun focusCurrentLocation(){
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

    private fun buttonControlListener(){
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
            setupTitle()
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
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        // Save step
        val editor = sharedPreferences.edit()
        editor.putFloat("previousTotalSteps", totalStep)
        editor.apply()
    }
    private fun loadWeekData() : Boolean {
        val sharedPreferences = getSharedPreferences("myPrefs",Context.MODE_PRIVATE)

        myWeek!!.deviceId = sharedPreferences.getString("deviceId","")
        myWeek!!.stepPerDay = sharedPreferences.getInt("stepPerDay",0)
        myWeek!!.mon = sharedPreferences.getInt("monStep",0)
        myWeek!!.tue = sharedPreferences.getInt("tueStep",0)
        myWeek!!.wed = sharedPreferences.getInt("wedStep",0)
        myWeek!!.thu = sharedPreferences.getInt("thuStep",0)
        myWeek!!.fri = sharedPreferences.getInt("friStep",0)
        myWeek!!.sat = sharedPreferences.getInt("satStep",0)
        myWeek!!.sun = sharedPreferences.getInt("sunStep",0)
        return true
    }

    // Set up title for some showing information
    private fun setupTitle(){

        binding.distanceTitleTv.text = getString(R.string.distance_title)
        binding.distanceContentTv.text = "0"
        binding.durationTitleTv.text = getString(R.string.duration_title)
        binding.durationContentTv.text = "0"
        binding.caloriesTitleTv.text = getString(R.string.calorie_title)
        binding.caloriesContentTv.text = "0"
    }

    // Update textview which show journey information
    private fun updateJourneyInformation(distance : Double, duration : Double){
        val kilometer = distance / 1000
        val hours = (duration / 3600).toInt()
        val minus = ((duration - hours * 3600) / 60).toInt()
        val second = (duration - hours * 3600 - minus * 60).toInt()
        binding.distanceContentTv.text = baseContext.resources.getString(R.string.distances,kilometer)
        binding.caloriesContentTv.text = baseContext.resources.getString(R.string.calories,kilometer * KILOMETER_TO_CALORIE)
        binding.durationContentTv.text = baseContext.resources.getString(R.string.duration,
            hours,
            minus,
            second)
    }

    private fun updateData(){
        myWeek = databasePreference!!.plusStepSpecificDay(
            myWeek!!,
            today!!,
            totalStep.toInt())

        totalStep = databasePreference!!.getStepSpecifyDay(myWeek!!, today!!).toFloat()
        saveData()
        saveWeekData()
        totalStep = 0f
    }
    override fun onPause() {
        updateData()
        Log.v(TAG, "Activity on pause, data updating!!!")
        super.onPause()
    }
    private fun saveWeekData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        // Save step
        val editor = sharedPreferences.edit()
        Log.v(TAG,"Week save is : $myWeek")
        editor.putString("deviceId", myWeek!!.deviceId)
        editor.putInt("stepPerDay",myWeek!!.stepPerDay!!)
        editor.putInt("monStep",myWeek!!.mon!!)
        editor.putInt("tueStep",myWeek!!.tue!!)
        editor.putInt("wedStep",myWeek!!.wed!!)
        editor.putInt("thuStep",myWeek!!.thu!!)
        editor.putInt("friStep",myWeek!!.fri!!)
        editor.putInt("satStep",myWeek!!.sat!!)
        editor.putInt("sunStep",myWeek!!.sun!!)

        editor.apply()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.v(TAG,"saveInstance :$outState")
        mGoogleMap?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
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