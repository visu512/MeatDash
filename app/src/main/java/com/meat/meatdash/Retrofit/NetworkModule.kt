package com.meat.meatdash.Retrofit

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


object NetworkModule {
    private const val BASE_URL = "https://maps.googleapis.com/maps/api/"
    val mapsApi: GoogleMapsApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(GoogleMapsApi::class.java)
}
