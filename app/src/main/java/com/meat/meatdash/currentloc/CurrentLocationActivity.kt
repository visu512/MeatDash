package com.meat.meatdash.currentloc

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.meat.meatdash.R
import com.meat.meatdash.model.Address
import com.meat.meatdash.sharedpref.PrefsHelper
import java.io.Serializable
import java.util.*

class CurrentLocationActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 3001
    }

    private lateinit var etHouseApartment: TextInputEditText
    private lateinit var etStreetAddress: TextInputEditText
    private lateinit var etLandmark: TextInputEditText
    private lateinit var etCity: TextInputEditText
    private lateinit var etState: TextInputEditText
    private lateinit var etZipCode: TextInputEditText

    private lateinit var chipGroupAddressType: ChipGroup
    private lateinit var chipHome: Chip
    private lateinit var chipWork: Chip
    private lateinit var chipFriend: Chip
    private lateinit var chipOther: Chip
    private lateinit var tilCustomTagContainer: View
    private lateinit var etCustomTag: TextInputEditText

    private lateinit var btnSaveProfile: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_current_location)

        // Initialize views
        etHouseApartment = findViewById(R.id.etHouseApartment)
        etStreetAddress = findViewById(R.id.etStreetAddress)
        etLandmark = findViewById(R.id.etLandmark)
        etCity = findViewById(R.id.etCity)
        etState = findViewById(R.id.etState)
        etZipCode = findViewById(R.id.etZipCode)

        chipGroupAddressType = findViewById(R.id.chipGroupAddressType)
        chipHome = findViewById(R.id.chipHome)
        chipWork = findViewById(R.id.chipWork)
        chipFriend = findViewById(R.id.chipFriend)
        chipOther = findViewById(R.id.chipOther)
        tilCustomTagContainer = findViewById(R.id.tilCustomTag)
        etCustomTag = findViewById(R.id.etCustomTag)

        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        progressBar = findViewById(R.id.progressBar)

        // Set chip text colors (optional, keep your color selector)
        listOf(chipHome, chipWork, chipFriend, chipOther).forEach {
            it.setTextColor(resources.getColorStateList(R.color.chip_text_color_selector, theme))
        }

        chipGroupAddressType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.chipOther) {
                tilCustomTagContainer.visibility = View.VISIBLE
                etCustomTag.text?.clear()
            } else {
                tilCustomTagContainer.visibility = View.GONE
                etCustomTag.setText("")
            }
        }

        chipHome.isChecked = true
        tilCustomTagContainer.visibility = View.GONE

        btnSaveProfile.isEnabled = false

        btnSaveProfile.setOnClickListener {
            if (!validateForm()) return@setOnClickListener
            saveAddressToFirestore()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermissionAndFetch()
    }

    private fun validateForm(): Boolean {
        var valid = true

        listOf(etHouseApartment, etStreetAddress, etCity, etState, etZipCode).forEach {
            it.error = null
        }
        etCustomTag.error = null

        if (etHouseApartment.text.isNullOrBlank()) {
            etHouseApartment.error = getString(R.string.house_apartment)
            valid = false
        }
        if (etStreetAddress.text.isNullOrBlank()) {
            etStreetAddress.error = getString(R.string.error_street_address)
            valid = false
        }
        if (etCity.text.isNullOrBlank()) {
            etCity.error = getString(R.string.error_city)
            valid = false
        }
        if (etState.text.isNullOrBlank()) {
            etState.error = getString(R.string.error_state)
            valid = false
        }
        if (etZipCode.text.isNullOrBlank()) {
            etZipCode.error = getString(R.string.error_zip_code)
            valid = false
        }
        if (chipOther.isChecked && etCustomTag.text.isNullOrBlank()) {
            etCustomTag.error = getString(R.string.error_custom_tag)
            valid = false
        }

        return valid
    }

    private fun checkLocationPermissionAndFetch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLastLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLastLocation() {
        progressBar.visibility = View.VISIBLE
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    reverseGeocode(location)
                } else {
                    requestNewLocationData()
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000
            fastestInterval = 2000
            numUpdates = 1
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val loc = result.lastLocation
                if (loc != null) {
                    reverseGeocode(loc)
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@CurrentLocationActivity, "Could not get location data", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }, Looper.getMainLooper())
    }

    private fun reverseGeocode(location: Location) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]

                etHouseApartment.setText(addr.subThoroughfare ?: "")
                etStreetAddress.setText(addr.thoroughfare ?: "")
                etLandmark.setText(addr.subLocality ?: addr.featureName ?: "")
                etCity.setText(addr.locality ?: "")
                etState.setText(addr.adminArea ?: "")
                etZipCode.setText(addr.postalCode ?: "")

                btnSaveProfile.isEnabled = true
                progressBar.visibility = View.GONE
            } else {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Could not decode address", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Reverse geocode failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLastLocation()
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun saveAddressToFirestore() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Saving this address…")
            setCancelable(false)
            show()
        }

        val houseApartmentVal = etHouseApartment.text.toString().trim()
        val streetAddressVal = etStreetAddress.text.toString().trim()
        val landmarkVal = etLandmark.text.toString().trim()
        val cityNameVal = etCity.text.toString().trim()
        val stateNameVal = etState.text.toString().trim()
        val postalCodeVal = etZipCode.text.toString().trim()

        val title = when (chipGroupAddressType.checkedChipId) {
            R.id.chipHome -> "Home"
            R.id.chipWork -> "Work"
            R.id.chipFriend -> "Friends & Family"
            R.id.chipOther -> etCustomTag.text.toString().trim()
            else -> "Home"
        }

        val userId = auth.currentUser?.uid ?: return

        val fullName = PrefsHelper.getString(this, "fullName") ?: ""
        val phoneNumber = PrefsHelper.getString(this, "phoneNumber") ?: ""

        val dataMap = hashMapOf(
            "houseApartment" to houseApartmentVal,
            "street" to streetAddressVal,
            "landmark" to landmarkVal,
            "city" to cityNameVal,
            "state" to stateNameVal,
            "zipCode" to postalCodeVal,
            "title" to title,
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "savedAt" to FieldValue.serverTimestamp(),
            "isDefault" to false
        )

        db.collection("userLocation")
            .document(userId)
            .collection("address")
            .add(dataMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                // Create Address object to return
                val savedAddress = Address(
                    title = title,
                    street = streetAddressVal,
                    houseApartment = houseApartmentVal,
                    city = cityNameVal,
                    state = stateNameVal,
                    zipCode = postalCodeVal,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    isDefault = false
                )
                val data = Intent().apply {
                    putExtra("address", savedAddress as Serializable)
                }
                setResult(RESULT_OK, data)
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to save location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
