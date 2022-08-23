package com.example.pedometer

import Place
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.pedometer.databinding.ActivityGpsMapBinding
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.codelabs.buildyourfirstmap.place.PlacesReader


class GpsMap : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mGoogleMap: GoogleMap
    private lateinit var binding: ActivityGpsMapBinding
    private val places: List<Place> by lazy {
        PlacesReader(this).read()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGpsMapBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync (this)

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
        mGoogleMap = googleMap

        // Add a marker in Sydney and move the camera
        val kyoto = LatLng(35.00116, 135.7681)
        mGoogleMap.addMarker(MarkerOptions().position(kyoto).title("Marker in Tokyo"))
        mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(15f))
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(kyoto))

    }

    private fun addMarkers( googleMap: GoogleMap){
        places.forEach{ place ->
            println(place.name)
            val market = googleMap.addMarker(
                MarkerOptions().title(place.name).position(place.latLng)
            )
        }
    }
}