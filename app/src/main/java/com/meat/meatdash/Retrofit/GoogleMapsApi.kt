package com.meat.meatdash.Retrofit

import retrofit2.http.GET
import retrofit2.http.Query

// --- Data models for only the fields we care about ---
data class DirectionsResponse(val routes: List<Route>)
data class Route(val legs: List<Leg>)
data class Leg(
    val distance: TextValue,
    val duration: TextValue,
    @get:JvmName("getDurationInTraffic") val duration_in_traffic: TextValue? = null
)

data class TextValue(val text: String, val value: Int)

// Retrofit interface
interface GoogleMapsApi {
    @GET("geocode/json")
    suspend fun geocode(
        @Query("address") address: String,
        @Query("key") apiKey: String     // only one "key" param
    ): GeocodeResponse

    @GET("directions/json")
    suspend fun directions(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving",
        @Query("departure_time") departure: String = "now",
        key: String
    ): DirectionsResponse
}


//  need GeocodeResponse to extract lat/lng:
data class GeocodeResponse(val results: List<GeoResult>)
data class GeoResult(val geometry: Geometry)
data class Geometry(val location: LatLng)
data class LatLng(val lat: Double, val lng: Double)
