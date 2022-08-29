package com.example.pedometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.provider.SettingsSlicesContract.KEY_LOCATION
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pedometer.BuildConfig.ACCESS_TOKEN
import com.example.pedometer.BuildConfig.MAPS_API_KEY
import com.example.pedometer.databinding.ActivityGpsMapBinding
import com.example.pedometer.place.Geometry
import com.example.pedometer.place.Routes
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place.Field
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming


class GpsMap : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityGpsMapBinding
//    private val places: List<Place> by lazy {
//        PlacesReader(this).read()
//    }
    private var markerPoints = ArrayList<LatLng>()
    private var mGoogleMap: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null

    // The entry point to the Places API.
    private lateinit var placesClient: PlacesClient

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private val defaultLocation = LatLng(22.0, 102.0)
    private var locationPermissionGranted = false

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.

    private var lastKnownLocation: Location? = null
    private var likelyPlaceNames: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAddresses: Array<String?> = arrayOfNulls(0)
    private var likelyPlaceAttributions: Array<List<*>?> = arrayOfNulls(0)
    private var likelyPlaceLatLngs: Array<LatLng?> = arrayOfNulls(0)

    private var urlParams = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGpsMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        this.mGoogleMap = googleMap

        // [START_EXCLUDE]
        // [START map_current_place_set_info_window_adapter]
        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
//        this.mGoogleMap?.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
//            // Return null here, so that getInfoContents() is called next.
//            override fun getInfoWindow(arg0: Marker): View? {
//                return null
//            }
//
//            override fun getInfoContents(marker: Marker): View {
//                // Inflate the layouts for the info window, title and snippet.
//                val infoWindow = layoutInflater.inflate(R.layout.custom_info_contents,
//                    findViewById<FrameLayout>(R.id.map), false)
//                val title = infoWindow.findViewById<TextView>(R.id.title)
//                title.text = marker.title
//                val snippet = infoWindow.findViewById<TextView>(R.id.snippet)
//                snippet.text = marker.snippet
//                return infoWindow
//            }
//        })
        // [END map_current_place_set_info_window_adapter]

        // Prompt the user for permission.
        getLocationPermission()
        // [END_EXCLUDE]

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()

        // Get the current location of the device and set the position of the map.
        getDeviceLocation()
        // Add a marker in Sydney and move the camera
//        val kyoto = LatLng(35.00116, 135.7681)
//        mGoogleMap.addMarker(MarkerOptions().position(kyoto).title("Marker in Tokyo"))
//        mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(15f))
//        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(kyoto))
        mGoogleMap!!.setOnMapClickListener {
            if (markerPoints.size > 1){
                markerPoints.clear()
                mGoogleMap!!.clear()
            }
            // Adding new item to the ArrayList
            markerPoints.add(it)
            // Creating MarkerOptions
            val markerOptions = MarkerOptions()
            // Setting the position of the marker
            markerOptions.position(it)
            if (markerPoints.size == 1)
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            else if (markerPoints.size == 2) {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            }
            // Add new marker to the Google Map Android API V2
            mGoogleMap!!.addMarker(markerOptions)
            // Checks, whether start and end locations are captured
            if (markerPoints.size >= 2){
                val origin = markerPoints[0]
                val dest = markerPoints[1]

                val polylineOptions = PolylineOptions()
//                polylineOptions.add(origin,dest)
                // Getting URL to the Google Directions API
//                urlParams = getDirectionsUrl(origin,dest)
                println("https://api.mapbox.com/directions/v5/mapbox/walking/" +
                        "${origin.longitude}%2C" +
                        "${origin.latitude}%3B" +
                        "${dest.longitude}%2C" +
                        "${dest.latitude}?alternatives=false&geometries=geojson&overview=simplified&steps=false&access_token=pk.eyJ1Ijoid2VlZGx5IiwiYSI6ImNsN2VpMW56bjAwa2gzbnBnaHd2MjJmZGYifQ.It2pYYoWNWQ-9Ogs49OUMg"
                )

                val coordinates = getDirection(origin,dest)
                println(coordinates)
                var nextStep = origin
                for (step in coordinates){
                    var lng = LatLng(step[1],step[0])
                    polylineOptions.add(nextStep,lng)
                    nextStep = lng
                }
                polylineOptions.add(nextStep,dest)
                mGoogleMap!!.addPolyline(polylineOptions)
            }
        }
    }
    private val baseUrl = "https://api.mapbox.com"

//    private val baseUrl = "https://android-kotlin-fun-mars-server.appspot.com/"
//
    // https://maps.googleapis.com
    // /maps/api/directions/json?
    // origin=10.803535704644942,106.63210149854422
    // &destination=10.79638842208413,106.63742065429688
    // &sensor=false
    // &mode=walking
    // &key=AIzaSyCm4V0EyWq3WR7Xcq6dplMHwn6qFKHX2o4

    //https://api.mapbox.com/directions/v5/mapbox/driving/-73.98986988331157%2C40.73400799834212%3B-73.9897834800288%2C40.7336951876986?alternatives=true&geometries=geojson&language=en&overview=simplified&steps=true&access_token=pk.eyJ1Ijoid2VlZGx5IiwiYSI6ImNsN2VpNGhzdzAwa3kzcG1rejUyMmx5bWgifQ.yWa9VGg1_-dNypDTcohLCg

    //https://api.mapbox.com/directions/v5/mapbox/driving/
    // -73.98985068258229
    // %2C40.7341607657969
    // %3B-73.9898410822176
    // %2C40.734000723693185
    // ?alternatives=true
    // &geometries=geojson&language=en&overview=simplified&steps=true&access_token=pk.eyJ1Ijoid2VlZGx5IiwiYSI6ImNsN2VpNGhzdzAwa3kzcG1rejUyMmx5bWgifQ.yWa9VGg1_-dNypDTcohLCg

    // https://api.mapbox.com
    // /directions/v5/mapbox/walking/
    // -73.98986028294692
    // %2C40.73354241917485
    // %3B-73.99008109133703
    // %2C40.7336442648961
    // ?alternatives=false
    // &geometries=geojson
    // &overview=simplified
    // &steps=false
    // &access_token=pk.eyJ1Ijoid2VlZGx5IiwiYSI6ImNsN2VpMW56bjAwa2gzbnBnaHd2MjJmZGYifQ.It2pYYoWNWQ-9Ogs49OUMg

    interface ApiInterface {
        @Streaming
        @GET("/directions/v5/mapbox/walking/" +
                "{lonOrigin}%2C" +
                "{latOrigin}%3B" +
                "{lonDes}%2C" +
                "{latDes}?alternatives=false&geometries=geojson&overview=simplified&steps=false&" +
                "access_token=${ACCESS_TOKEN}")
//        @GET("/{photos}" )
        suspend fun getPlaces(
//              @Path("photos") name : String
            @Path("lonOrigin") lonOrigin : Double,
            @Path("latOrigin") latOrigin : Double,
            @Path("lonDes") lonDes : Double,
            @Path("latDes") latDes : Double,
        ): Routes
    }
    private fun getDirection(origin: LatLng ,dest: LatLng) : List<List<Double>>{
        val retrofit = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build()
        val service: ApiInterface by lazy { retrofit.create(ApiInterface::class.java)}
        var  coordinates : List<List<Double>>

        coordinates = runBlocking {
            withContext(Dispatchers.Default) {
                val routes = service.getPlaces(
                    origin.longitude,
                    origin.latitude,
                    dest.longitude,
                    dest.latitude
                )
                coordinates = routes.routes[0]!!.geometry.coordinates
            }
            coordinates
        }
        return coordinates
    }

    //https://maps.googleapis.com/maps/api/directions/json
    //  ?avoid=highways
    //  &destination=Montreal
    //  &mode=bicycling
    //  &origin=Toronto
    //  &key=YOUR_API_KEY

    //"https://maps.googleapis.com/maps/api/directions/json"
    // String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;
    private fun getDirectionsUrl( origin : LatLng, dest : LatLng) : String{
        val strOrigin = "origin=" + origin.latitude + "," + origin.longitude
        val strDest = "destination=" + dest.latitude + "," + dest.longitude
        val sensor = "sensor=false"
        val mode = "mode=driving"
        val key = "key=$MAPS_API_KEY"
        val params = "$strOrigin&$strDest&$sensor&$mode&$key"
        val outputType = "json"
        val defaultLink = "https://maps.googleapis.com/maps/api/directions/"
        return "$defaultLink$outputType?$params"
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
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.current_place_menu, menu)
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
            println("get_divice_location")
            if(locationPermissionGranted){
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this){
                    task ->
                    if(task.isSuccessful){
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null){
                            mGoogleMap?.moveCamera((CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.altitude), DEFAULT_ZOOM.toFloat())))
                        }
                    }

                }
            }
        }
        catch (e : SecurityException){
            Log.e("Exception %e",e.message,e)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.option_get_place){
            println("show_current")
            showCurrentPlace()
        }
        return true
    }
    @SuppressLint("MissingPermission")
    private fun showCurrentPlace(){
        println("show_current")
        if(mGoogleMap == null){
            return
        }
        println(locationPermissionGranted)
        if (locationPermissionGranted){
            // Use fields to define the data types to return.
            val placeFields = listOf(Field.NAME, Field.ADDRESS, Field.LAT_LNG)
            // Use the builder to create a FindCurrentPlaceRequest.
            val request = FindCurrentPlaceRequest.newInstance(placeFields)
            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            val placeResult = placesClient.findCurrentPlace(request)
            placeResult.addOnCompleteListener{task->
                if (task.isSuccessful && task.result != null){
                    val likelyPlaces = task.result
                    val count = if (likelyPlaces != null && likelyPlaces.placeLikelihoods.size < M_MAX_ENTRIES) {
                        likelyPlaces.placeLikelihoods.size
                    }else{
                        M_MAX_ENTRIES
                    }
                    var i = 0
                    likelyPlaceNames = arrayOfNulls(count)
                    likelyPlaceAddresses = arrayOfNulls(count)
                    likelyPlaceAttributions = arrayOfNulls<List<*>?>(count)
                    likelyPlaceLatLngs = arrayOfNulls(count)
                    for (placeLikelihood in likelyPlaces?.placeLikelihoods ?: emptyList()) {
                        // Build a list of likely places to show the user.
                        likelyPlaceNames[i] = placeLikelihood.place.name?.toString()
                        likelyPlaceAddresses[i] = placeLikelihood.place.address
                        likelyPlaceAttributions[i] = placeLikelihood.place.attributions
                        likelyPlaceLatLngs[i] = placeLikelihood.place.latLng
                        println("stt: $i ")
                        print(" ${likelyPlaceLatLngs[i]!!.latitude}")
                        println(" and  ${likelyPlaceLatLngs[i]!!.longitude}")
                        i++
                        if (i > count - 1) {
                            break
                        }
                    }

                    // Show a dialog offering the user the list of likely places, and add a
                    // marker at the selected place.
                    openPlacesDialog()
                } else {
                    Log.e(TAG, "Exception: %s", task.exception)
                }

            }
        }
        else {
            // The user has not granted permission.
            Log.i(TAG, "The user did not grant location permission.")

            // Add a default marker, because the user hasn't selected a place.
            mGoogleMap?.addMarker(MarkerOptions()
                .title(getString(R.string.default_info_title))
                .position(defaultLocation)
                .snippet(getString(R.string.default_info_snippet)))

            // Prompt the user for permission.
            getLocationPermission()
        }
    }
    private fun openPlacesDialog() {

        // Ask the user to choose the place where they are now.
        val listener = DialogInterface.OnClickListener { _, which -> // The "which" argument contains the position of the selected item.
            val markerLatLng = likelyPlaceLatLngs[which]
            var markerSnippet = likelyPlaceAddresses[which]
            if (likelyPlaceAttributions[which] != null) {
                markerSnippet = """
                    $markerSnippet
                    ${likelyPlaceAttributions[which]}
                    """.trimIndent()
            }

            if (markerLatLng == null) {
                return@OnClickListener
            }

            // Add a marker for the selected place, with an info window
            // showing information about that place.

            mGoogleMap?.addMarker(MarkerOptions()
                .title(likelyPlaceNames[which])
                .position(markerLatLng)
                .snippet(markerSnippet))

            // Position the map's camera at the location of the marker.
            mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,
                DEFAULT_ZOOM.toFloat()))
        }

        // Display the dialog.
        AlertDialog.Builder(this)
            .setTitle(R.string.pick_place)
            .setItems(likelyPlaceNames, listener)
            .show()
    }
    companion object {
        private val TAG = GpsMap::class.java.simpleName
        private const val DEFAULT_ZOOM = 0
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        // [START maps_current_place_state_keys]
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
        // [END maps_current_place_state_keys]

        // Used for selecting the current place.
        private const val M_MAX_ENTRIES = 10
    }
}