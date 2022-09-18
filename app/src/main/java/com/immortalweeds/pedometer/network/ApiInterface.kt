package com.immortalweeds.pedometer.network

import com.immortalweeds.pedometer.BuildConfig
import com.immortalweeds.pedometer.model.gps.Routes
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming


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
// &access_token=access_token

interface ApiInterface {
    @Streaming
    @GET("/directions/v5/mapbox/walking/" +
            "{lonOrigin}%2C" +
            "{latOrigin}%3B" +
            "{lonDes}%2C" +
            "{latDes}?alternatives=false&geometries=geojson&overview=simplified&steps=false&" +
            "access_token=${BuildConfig.ACCESS_TOKEN}")
//        @GET("/{photos}" )
    suspend fun getPlaces(
//              @Path("photos") name : String
        @Path("lonOrigin") lonOrigin : Double,
        @Path("latOrigin") latOrigin : Double,
        @Path("lonDes") lonDes : Double,
        @Path("latDes") latDes : Double,
    ): Routes
}