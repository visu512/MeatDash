package com.meat.meatdash.Retrofit

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.meat.meatdash.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DistanceActivity : AppCompatActivity() {
    private val apiKey by lazy { getString(R.string.google_maps_key) }  /// api key
    private val mapsApi = NetworkModule.mapsApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_distance)

        val startEt = findViewById<EditText>(R.id.etStart)
        val endEt = findViewById<EditText>(R.id.etEnd)
        val btn = findViewById<Button>(R.id.btnCalc)
        val result = findViewById<TextView>(R.id.tvResult)

        btn.setOnClickListener {
            val a = startEt.text.toString().trim()
            val b = endEt.text.toString().trim()
            if (a.isEmpty() || b.isEmpty()) {
                result.text = "Enter both addresses"
            } else {
                fetchDistance(a, b, result)
            }
        }
    }

    private fun fetchDistance(
        originAddr: String,
        destAddr: String,
        resultView: TextView
    ) {
        resultView.text = "Calculating…"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1) Geocode addresses to lat,lng
                val g1 = mapsApi.geocode(originAddr, apiKey)
                val g2 = mapsApi.geocode(destAddr, apiKey)
                val loc1 = g1.results.first().geometry.location
                val loc2 = g2.results.first().geometry.location

                // 2) Call Directions API
                val origin = "${loc1.lat},${loc1.lng}"
                val dest = "${loc2.lat},${loc2.lng}"
                val resp = mapsApi.directions(origin, dest, key = apiKey)

                val leg = resp.routes.first().legs.first()

                // 3) Extract distance & time
                val distanceText = leg.distance.text
                val durationText = leg.duration_in_traffic?.text ?: leg.duration.text

                withContext(Dispatchers.Main) {
                    resultView.text = "Distance: $distanceText\nETA: $durationText"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultView.text = "Error: ${e.message}"
                }
            }
        }
    }
}
