package com.meat.meatdash.checkout

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.meat.meatdash.CartManager
import com.meat.meatdash.R
import com.meat.meatdash.activity.MainActivity
import com.meat.meatdash.adapter.AddressAdapter
import com.meat.meatdash.adapter.CheckoutItemsAdapter
import com.meat.meatdash.address.AddressActivity
import com.meat.meatdash.currentloc.CurrentLocationActivity
import com.meat.meatdash.databinding.ActivityCheckoutBinding
import com.meat.meatdash.model.Address
import com.meat.meatdash.model.FoodItem
import com.meat.meatdash.success.OrderSuccessActivity
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class CheckoutActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var binding: ActivityCheckoutBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: CheckoutItemsAdapter
    private lateinit var itemList: List<FoodItem>
    private lateinit var progressDialog: ProgressDialog

    private var selectedAddress: Address? = null
    private var orderTotal: Double = 0.0

    // SharedPrefs keys


    companion object {
        private const val PREFS_NAME = "MeatDashPrefs"
        private const val KEY_SHOP_LOCATION = "SHOP_LOCATION"
        private const val KEY_USER_LOCATION = "USER_LOCATION"
        private const val KEY_DISTANCE = "LAST_DISTANCE"
        private const val KEY_DURATION = "LAST_DURATION"
        private const val KEY_SHIPPING = "LAST_SHIPPING"
        private const val KEY_FINISH_PREFIX = "FINISH_TIME_"
        private val SELECTED_ADDRESS_KEY = "selected_address"

    }


    private var lastBufferedMins: Double = 0.0

    // weather api
    private val WEATHER_BASE_URL = "http://api.weatherapi.com/v1"
    private val WEATHER_API_KEY = "cde4c7f6d8b44bd8a4253643251506"

    // Default GST
    private val GST_PERCENTAGE = 5.0

    private var rainCharge: Int = 0
    private var isRaining: Boolean = false

    // Razorpay
    private val RAZORPAY_KEY = "rzp_test_tH7nTQabmzh9IE"

    // Routes API
    private val apiKey = "AIzaSyAjSkkstE4u1JZDUbSiPEVyCGr2u5l7638"
    private val queue by lazy { Volley.newRequestQueue(this) }
    private var currentRequest: JsonObjectRequest? = null

    // Address pickers
    private val addressActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            (result.data?.getSerializableExtra("address") as? Address)
                ?.let { updateSelectedAddress(it) }
        }
    }
    private val currentLocationActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            (result.data?.getSerializableExtra("address") as? Address)
                ?.let { updateSelectedAddress(it) }
        }
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityCheckoutBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        CartManager.init(this)
//
//        // 1) Load cart items & RecyclerView
//        itemList = CartManager.getCartItems()
//        adapter = CheckoutItemsAdapter(itemList)
//        binding.recyclerView.apply {
//            adapter = this@CheckoutActivity.adapter
//            layoutManager = LinearLayoutManager(this@CheckoutActivity)
//        }
//
//        val shopPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//        val shopLoc   = shopPrefs.getString(KEY_SHOP_LOCATION, "").orEmpty()
//        binding.etStart.setText(shopLoc)
//
//
//        // 2) Prefill shop & user location
//        val shopLoc = itemList.firstOrNull()?.shopLocation.orEmpty()
//        binding.etStart.setText(shopLoc)
//
//        val prefs = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
//        val userLoc = prefs.getString(KEY_USER_LOCATION, "").orEmpty()
//        binding.etEnd.setText(userLoc)
//
//
//        // 3) Auto-calculate route if both fields set
//        autoCalculateRouteIfPossible()
//
//        // 4) Load any saved distance/time/shipping
//        binding.tvRouteInfo.text = prefs.getString(KEY_DISTANCE, "")
//        binding.textDeliveryTimer.text = prefs.getString(KEY_DURATION, "")
//        binding.textShipping.text = "₹${prefs.getInt(KEY_SHIPPING, 0)}"
//
//
//        // 5) Remaining setup
//        setupProgressDialog()
//        loadAddress()
//        updateOrderSummary()  // initial summary (uses 0 shipping until route computed)
//        setupClickListeners()
//
//        // back button
//        binding.backButton.setOnClickListener {
//            onBackPressed()
//        }
//    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) Initialize CartManager (loads from prefs)
        CartManager.init(this)

        // 2) Grab items and build RecyclerView
        itemList = CartManager.getCartItems()
        adapter = CheckoutItemsAdapter(itemList)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CheckoutActivity)
            adapter = this@CheckoutActivity.adapter
        }

        // 3) Prefill “From” from shop–detail prefs
        val shopPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val shopLoc = shopPrefs.getString(KEY_SHOP_LOCATION, "").orEmpty()
        binding.etStart.setText(shopLoc)

        // 4) Prefill “To” from previous user–address prefs
        val userLoc = shopPrefs.getString(KEY_USER_LOCATION, "").orEmpty()
        binding.etEnd.setText(userLoc)

        // 5) If both are set, calculate route immediately
        autoCalculateRouteIfPossible()

        // 6) Load any saved distance/time/shipping into UI
        binding.tvRouteInfo.text = shopPrefs.getString(KEY_DISTANCE, "")
        binding.textDeliveryTimer.text = shopPrefs.getString(KEY_DURATION, "")
        binding.textShipping.text = "₹${shopPrefs.getInt(KEY_SHIPPING, 0)}"

        // 7) Remaining setup
        setupProgressDialog()
        loadAddress()
        updateOrderSummary()
        setupClickListeners()

        // Back button
        binding.backButton.setOnClickListener { onBackPressed() }
    }


    // updateOrderSummary() to include the ₹30 rain surcharge only when isRaining == true:
//    @SuppressLint("SetTextI18n")
//    private fun updateOrderSummary() {
//        val subtotal = itemList.sumOf { it.price * (it.weight / 1000.0) }
//        val tax = subtotal * GST_PERCENTAGE / 100
//
//        val prefs = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
//        val shippingFee = prefs.getInt(KEY_SHIPPING, 0)
//
//        // reset rain charge
//        rainCharge = if (isRaining) 30 else 0
//
//        // compute total
//        orderTotal = subtotal + tax + shippingFee + rainCharge
//
//        // update UI
//        binding.textSubtotal.text = "₹%.2f".format(subtotal)
//        binding.textTax.text = "₹%.2f".format(tax)
//        binding.textShipping.text = "₹$shippingFee"
//
//        if (isRaining) {
//            binding.rainRow.visibility = View.VISIBLE
//            binding.RainTextPrice.text = "₹$rainCharge"
//        } else {
//            binding.rainRow.visibility = View.GONE
//        }
//
//        binding.textTotal.text = "₹%.2f".format(orderTotal)
//    }

    @SuppressLint("SetTextI18n")
    private fun updateOrderSummary() {
        val subtotal = itemList.sumOf { it.price * it.weight / 1000.0 }
        val tax = subtotal * 5.0 / 100
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val shipping = prefs.getInt(KEY_SHIPPING, 0)

        rainCharge = if (isRaining) 30 else 0
        orderTotal = subtotal + tax + shipping + rainCharge

        binding.textSubtotal.text = "₹%.2f".format(subtotal)
        binding.textTax.text = "₹%.2f".format(tax)
        binding.textShipping.text = "₹$shipping"

        binding.rainRow.visibility = if (rainCharge > 0) View.VISIBLE else View.GONE
        binding.RainTextPrice.text = "₹$rainCharge"
        binding.textTotal.text = "₹%.2f".format(orderTotal)
    }


    private fun autoCalculateRouteIfPossible() {
        val shop = binding.etStart.text.toString().trim()
        val user = binding.etEnd.text.toString().trim()
        if (shop.isNotEmpty() && user.isNotEmpty()) {
            calculateRoute(shop, user)
        }
    }

    private fun setupProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setMessage("Order placing...")
            setCancelable(false)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupClickListeners() {
        binding.btnCalc.setOnClickListener {
            val shop = binding.etStart.text.toString().trim()
            val user = binding.etEnd.text.toString().trim()
            if (shop.isEmpty() || user.isEmpty()) {
                Toast.makeText(this, "Enter both addresses", Toast.LENGTH_SHORT).show()
            } else
                calculateRoute(shop, user)

        }

        binding.btnPlaceOrder.setOnClickListener {
            if (itemList.isEmpty()) {
                Toast.makeText(this, "Your cart is empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            itemList.firstOrNull { it.weight < 100 }?.let {
                Toast.makeText(
                    this,
                    "Minimum weight for ${it.name} is 100g",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            if (selectedAddress == null) {
                Toast.makeText(this, "Select a delivery address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startRazorpayPayment()
        }

        binding.btnChangeAddress.setOnClickListener { showAddressBottomSheet() }
        binding.btnAddOrSelectAddress.setOnClickListener { showAddressBottomSheet() }
    }


    // shipping charges
    private fun computeShippingFee(distanceKm: Double): Int {
        return when {
            distanceKm <= 2.0 -> {
                22   // below 2 km charge
            }

            distanceKm <= 4.0 -> {
                // ₹22 for first 2 km + ₹8/km beyond
                22 + ((distanceKm - 2.0) * 8).toInt()
            }

            distanceKm <= 7.0 -> {
                // ₹38 for first 4 km + ₹9/km beyond
                38 + ((distanceKm - 4.0) * 9).toInt()
            }

            else -> {
                // ₹65 for first 7 km + ₹11/km beyond
                65 + ((distanceKm - 7.0) * 10).toInt()
            }
        }
    }

    // calculate route distance and time
    @SuppressLint("SetTextI18n")
    private fun calculateRoute(originAddr: String, destAddr: String) {
        val geo = Geocoder(this, Locale.getDefault())
        val oLoc: android.location.Address?
        val dLoc: android.location.Address?

        //  Wrap geocoder lookups in IOException catch
        try {
            oLoc = geo.getFromLocationName(originAddr, 1)?.firstOrNull()
            dLoc = geo.getFromLocationName(destAddr, 1)?.firstOrNull()
        } catch (ioe: IOException) {
            binding.tvRouteInfo.text = "Unable to geocode"
            binding.textDeliveryTimer.text = ""
            return
        }

        if (oLoc == null || dLoc == null) {
            binding.tvRouteInfo.text = "Addresses not found"
            binding.textDeliveryTimer.text = ""
            return
        }

        // 2) Permission check
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            binding.tvRouteInfo.text = "Grant location permission"
            return
        }

        binding.tvRouteInfo.text = "Calculating…"
        binding.textDeliveryTimer.text = ""

        val depart = System.currentTimeMillis() / 1000 + 30
        val body = JSONObject().apply {
            put("origin", JSONObject().put("location",
                JSONObject().put("latLng",
                    JSONObject().apply {
                        put("latitude", oLoc.latitude)
                        put("longitude", oLoc.longitude)
                    }
                )
            ))
            put("destination", JSONObject().put("location",
                JSONObject().put("latLng",
                    JSONObject().apply {
                        put("latitude", dLoc.latitude)
                        put("longitude", dLoc.longitude)
                    }
                )
            ))
            put("travelMode", "TWO_WHEELER")
            put("routingPreference", "TRAFFIC_AWARE")
            put("departureTime", JSONObject().put("seconds", depart))
            put("computeAlternativeRoutes", false)
        }

        val url = "https://routes.googleapis.com/directions/v2:computeRoutes"
        currentRequest?.cancel()

        //  Wrap Volley call in general try/catch to avoid unexpected crashes
        try {
            currentRequest = object : JsonObjectRequest(
                Request.Method.POST, url, body,
                { resp ->
                    val legs = resp.optJSONArray("routes")
                        ?.getJSONObject(0)
                        ?.optJSONArray("legs")
                    if (legs != null && legs.length() > 0) {
                        val leg = legs.getJSONObject(0)
                        val distM = leg.optInt("distanceMeters", 0)
                        val rawD = leg.optString("duration", "0s")
                        val secs = rawD.removeSuffix("s").toDoubleOrNull() ?: 0.0
                        val mins = secs / 60.0

                        //add your 3-minute buffer extra time:
                        lastBufferedMins = mins + 3.0

                        val distKm = distM / 1000.0
                        val distStr = "Distance: %.2f km".format(distKm)
                        val durStr = "Time: %.1f min".format(lastBufferedMins)

                        // Show distance & time
                        binding.tvRouteInfo.text = distStr
                        binding.textDeliveryTimer.text = durStr

                        // Compute & show shipping
                        val shippingFee = computeShippingFee(distKm)
                        binding.textShipping.text = "₹$shippingFee"

                        // Recompute summary
                        val subtotal = itemList.sumOf { it.price * it.weight / 1000.0 }
                        val tax = subtotal * GST_PERCENTAGE / 100
                        binding.textSubtotal.text = "₹%.2f".format(subtotal)
                        binding.textTax.text = "₹%.2f".format(tax)
                        binding.textTotal.text = "₹%.2f".format(subtotal + tax + shippingFee)

                        // Persist for next time
                        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                            .putString(KEY_DISTANCE, distStr)
                            .putString(KEY_DURATION, durStr)
                            .putInt(KEY_SHIPPING, shippingFee)
                            .apply()
                    } else {
                        binding.tvRouteInfo.text = "No route found"
                    }
                },
                { err ->
                    binding.tvRouteInfo.text = "Error: ${err.message}"
                }
            ) {
                override fun getHeaders() = mapOf(
                    "X-Goog-Api-Key" to apiKey,
                    "Content-Type" to "application/json",
                    "X-Goog-FieldMask" to "routes.legs.distanceMeters,routes.legs.duration"
                ).also { setShouldCache(false) }
            }
            queue.add(currentRequest)
        } catch (e: Exception) {
            // Catch any unexpected issues building/sending the request
            binding.tvRouteInfo.text = "Route error"
            binding.textDeliveryTimer.text = ""
        }
    }


    //  Address selection sheet & CRUD —
    private fun showAddressBottomSheet() {
        val bs = BottomSheetDialog(this, R.style.DialogAnimation)
        bs.setContentView(R.layout.bottom_sheet_address)
        bs.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bs.behavior.skipCollapsed = true
        bs.setCancelable(true)
        bs.findViewById<View>(R.id.btnClose)?.setOnClickListener { bs.dismiss() }
        bs.findViewById<View>(R.id.btnCurrentAddress)
            ?.setOnClickListener {
                bs.dismiss()
                currentLocationActivityLauncher.launch(
                    Intent(this, CurrentLocationActivity::class.java)
                )
            }
        bs.findViewById<View>(R.id.btnAddNewAddress)
            ?.setOnClickListener {
                bs.dismiss()
                addressActivityLauncher.launch(
                    Intent(this, AddressActivity::class.java)
                )
            }
        val rv = bs.findViewById<androidx.recyclerview.widget.RecyclerView>(
            R.id.rvSavedAddresses
        )
        rv?.layoutManager = LinearLayoutManager(this)
        loadSavedAddresses { list ->
            rv?.adapter = AddressAdapter(
                list,
                onEditClick = { a ->
                    addressActivityLauncher.launch(
                        Intent(this, AddressActivity::class.java)
                            .apply { putExtra("address_to_edit", a) }
                    )
                    bs.dismiss()
                },
                onDeleteClick = { a ->
                    showDeleteAddressDialog(a) {
                        loadSavedAddresses { refreshed ->
                            (rv?.adapter as AddressAdapter).updateAddresses(refreshed)
                        }
                    }
                },
                onSetDefault = { a ->
                    setDefaultAddress(a) {
                        updateSelectedAddress(a)
                        bs.dismiss()
                    }
                },
                onAddressSelected = { a ->
                    updateSelectedAddress(a)
                    bs.dismiss()
                }
            )
        }
        bs.show()
    }

    private fun loadAddress() {
        (intent.getSerializableExtra("address") as? Address)?.let {
            updateSelectedAddress(it); return
        }
        loadSelectedAddress()?.let {
            updateSelectedAddress(it); return
        }
        val user = auth.currentUser ?: run {
            updateInitialButtonVisibility(); return
        }
        firestore.collection("userLocation")
            .document(user.uid)
            .collection("selectedAddress")
            .document("profile")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    val d = snap.data ?: emptyMap<String, Any>()
                    val sa = (d["savedAt"] as? Timestamp)?.toDate()?.time ?: 0L
                    val a = Address(
                        title = d["title"] as? String ?: "",
                        street = d["street"] as? String ?: "",
                        houseApartment = d["houseApartment"] as? String ?: "",
                        city = d["city"] as? String ?: "",
                        state = d["state"] as? String ?: "",
                        zipCode = d["zipCode"] as? String ?: "",
                        fullName = d["fullName"] as? String ?: "",
                        phoneNumber = d["phoneNumber"] as? String ?: "",
                        savedAt = sa,
                        isDefault = d["isDefault"] as? Boolean ?: false,
                        current = d["current"] as? String ?: ""
                    )
                    updateSelectedAddress(a)
                } else updateInitialButtonVisibility()
            }
            .addOnFailureListener { updateInitialButtonVisibility() }
    }

    private fun saveLocationsToPrefs() {
        val p = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val e = p.edit()
        selectedAddress?.let { addr ->
            val userLoc = "${addr.city}, ${addr.state} ${addr.zipCode}"
            e.putString(KEY_USER_LOCATION, userLoc)
        }
        itemList.firstOrNull()?.shopLocation?.let { shopLoc ->
            e.putString(KEY_SHOP_LOCATION, shopLoc)
        }
        e.apply()
    }


    @SuppressLint("SetTextI18n")
    private fun updateSelectedAddress(addr: Address) {
        selectedAddress = addr
        saveSelectedAddress(addr)
        saveSelectedAddressToFirestore(addr)
        saveLocationsToPrefs()
        displayAddress(addr)
        updateInitialButtonVisibility()

        // Fill etEnd & recall route immediately
        val userLoc = "${addr.city}, ${addr.state} ${addr.zipCode}"
        binding.etEnd.setText(userLoc)

//        val shopLocs = "${}"
        val shopLoc = binding.etStart.text.toString().trim()
        if (shopLoc.isNotEmpty()) calculateRoute(shopLoc, userLoc)


        // for weather calculation only city
        val cityOnly = addr.city.trim()
        binding.etWeather.setText(cityOnly)

        fetchWeather(cityOnly)
    }


    // weather api calling
    @SuppressLint("SetTextI18n")
    private fun fetchWeather(city: String) {
        val apiKey = "f3549eaf34e9b13bbd59e7bf288d74b6"
        val url = "https://api.openweathermap.org/data/2.5/weather" +
                "?q=${URLEncoder.encode(city, "UTF-8")}" +
                "&appid=$apiKey" +
                "&units=metric"

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { resp ->
                try {
                    val weatherObj = resp
                        .getJSONArray("weather")
                        .getJSONObject(0)
                    val main = weatherObj.getString("main")
                    val desc = weatherObj.getString("description")

                    val isRaining = listOf("Rain", "Drizzle", "Thunderstorm")
                        .any { it.equals(main, ignoreCase = true) }

                    // set surcharge
                    rainCharge = if (isRaining) 30 else 0

                    // show/hide the rain row
                    binding.rainRow.visibility = if (rainCharge > 0) View.VISIBLE else View.GONE
                    binding.RainTextPrice.text = "₹$rainCharge"

                    // rebuild total with surcharge
                    val finalTotal = orderTotal + rainCharge
                    binding.textTotal.text = "₹%.2f".format(finalTotal)

                    // optionally show a bit more weather info
                    binding.tvRainWeather.text = buildString {
                        append("Location: $city\n")
                        append("Condition: $desc\n")
                        append(if (isRaining) "Yes – it’s currently raining." else "No rain right now.")
                    }
                } catch (e: Exception) {
                    Log.e("WeatherFetch", "JSON parse error", e)
                    binding.tvRainWeather.text = "Weather parse error"
                }
            },
            { error ->
                Log.e("WeatherFetch", "Volley error: ${error.networkResponse?.statusCode}", error)
                binding.tvRainWeather.text = "Weather API error"
            }
        ).apply {
            retryPolicy = DefaultRetryPolicy(
                5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        }

        queue.add(request)
    }


    // button
    private fun updateInitialButtonVisibility() {
        val has = selectedAddress != null
        binding.tvDeliveryAddress.visibility = if (has) View.VISIBLE else View.GONE
        binding.addressCard.visibility = if (has) View.VISIBLE else View.GONE
        binding.btnChangeAddress.visibility = if (has) View.VISIBLE else View.GONE
        binding.btnAddOrSelectAddress.visibility =
            if (has) View.GONE else View.VISIBLE
        binding.btnPlaceOrder.visibility = if (has) View.VISIBLE else View.GONE
    }

    private fun displayAddress(a: Address) {
        binding.textAddressTitle.text = a.title
        binding.textAddressDetails.text = buildString {
            if (a.fullName.isNotEmpty()) append("${a.fullName}\n")
            if (a.phoneNumber.isNotEmpty()) append("${a.phoneNumber}\n")
            append(a.street)
            if (a.houseApartment.isNotEmpty())
                append(", ${a.houseApartment}")
            append(", ${a.city}, ${a.state} ${a.zipCode}")
        }
    }

    private fun validateAndSaveDeliveryAddress(): Boolean =
        (selectedAddress != null).also {
            if (!it) Toast.makeText(
                this, "Select a delivery address", Toast.LENGTH_SHORT
            ).show()
        }


    /// payment gateway razorpay
    private fun startRazorpayPayment() {
        // orderTotal is up-to-date
        updateOrderSummary()

        try {
            val amountInPaise = (orderTotal * 100).toInt()  // Razorpay expects amount in paise

            val options = JSONObject().apply {
                put("name", "Meat Me Up")
                put("description", "Order Payment")
                put("theme.color", "#F7A687")
                put("currency", "INR")
                put("amount", amountInPaise)

                // Display the exact total amount to user
                put("display_amount", orderTotal)
                put("display_currency", "INR")

                // Prefill customer details
                put("prefill", JSONObject().apply {
                    put("email", auth.currentUser?.email ?: "")
                    put("name", selectedAddress?.fullName ?: "Customer")
                    put("contact", selectedAddress?.phoneNumber ?: "")
                })

                // Other options
                put("image", "https://i.postimg.cc/4dhPSbRg/app-Icon-logo.png") // app logo link
                put("modal.timeout", 300) // 5 minutes timeout
            }

            // Initialize Razorpay
            val checkout = Checkout()
            checkout.setKeyID(RAZORPAY_KEY)

            // Open payment dialog
            checkout.open(this@CheckoutActivity, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show()
        processOrder(razorpayPaymentId)
    }

    override fun onPaymentError(errorCode: Int, response: String?) {
        Toast.makeText(this, "Payment Failed! Try again.", Toast.LENGTH_LONG).show()
    }


    private fun processOrder(paymentId: String?) {
        progressDialog.show()
        val shopName = itemList.firstOrNull()?.shopName ?: ""
        val shopLocation = itemList.firstOrNull()?.shopLocation ?: ""


        val orderId = generateOrderId()
        val userId = auth.currentUser?.uid ?: "guest"
        val subtotal = itemList.sumOf { it.price * it.weight / 1000.0 }
        val tax = subtotal * GST_PERCENTAGE / 100
        val shipping = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SHIPPING, 0)
        val total = subtotal + tax + shipping


        val finishTs = System.currentTimeMillis() + (lastBufferedMins * 60_000).toLong()
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_FINISH_PREFIX + orderId, finishTs)
            .apply()

        val data = hashMapOf(
            "orderId" to orderId,
            "userId" to userId,
            "subtotal" to subtotal,
            "shipping" to shipping,
            "tax" to tax,
            "total" to total,
            "shopName" to shopName,
            "shopLocation" to shopLocation,
            "paymentId" to paymentId,
            "status" to "Paid",
            "timestamp" to System.currentTimeMillis(),
            "fullName" to (selectedAddress?.fullName ?: ""),
            "phoneNumber" to (selectedAddress?.phoneNumber ?: ""),
            "streetAddress" to (selectedAddress?.street ?: ""),
            "houseApartment" to (selectedAddress?.houseApartment ?: ""),
            "city" to (selectedAddress?.city ?: ""),
            "state" to (selectedAddress?.state ?: ""),
            "zipCode" to (selectedAddress?.zipCode ?: ""),
            "deliveryDate" to calculateDeliveryDate(),

            "items" to itemList.map { it ->
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "price" to it.price * it.weight / 1000.0,
                    "quantity" to it.weight,
                    "image" to it.imageBase64
                )


            }

        )

        firestore.collection("OrderList").document(orderId)
            .set(data)
            .addOnSuccessListener {
                progressDialog.dismiss()
                CartManager.clearCart()
                navigateToOrderSuccess(orderId)
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun generateOrderId(): String =
        "ORD-" + SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
            .format(Date()) + "-${(1000..9999).random()}"

    private fun calculateDeliveryDate(): String =
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }.time
        )

    private fun navigateToOrderSuccess(orderId: String) {
        Intent(this, OrderSuccessActivity::class.java).apply {
            putExtra("orderId", orderId)
            putExtra("deliveryDate", calculateDeliveryDate())
            putExtra("address", selectedAddress)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }.also { startActivity(it) }
        finish()
    }

    private fun loadSavedAddresses(onComplete: (List<Address>) -> Unit) {
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("userLocation")
                .document(uid)
                .collection("address")
                .get()
                .addOnSuccessListener { snap ->
                    val list = snap.documents.mapNotNull { doc ->
                        doc.data?.let { d ->
                            Address(
                                fullName = d["fullName"] as? String ?: "",
                                phoneNumber = d["phoneNumber"] as? String ?: "",
                                street = d["street"] as? String ?: "",
                                houseApartment = d["houseApartment"] as? String ?: "",
                                city = d["city"] as? String ?: "",
                                state = d["state"] as? String ?: "",
                                zipCode = d["zipCode"] as? String ?: "",
                                current = d["current"] as? String ?: "",
                                title = d["title"] as? String ?: "",
                                savedAt = (d["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L,
                                isDefault = d["isDefault"] as? Boolean ?: false
                            )
                        }
                    }
                    onComplete(list)
                }
                .addOnFailureListener { onComplete(emptyList()) }
        } ?: onComplete(emptyList())
    }

    private fun showDeleteAddressDialog(address: Address, onDeleted: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Delete Address")
            .setMessage("Are you sure you want to delete this address?")
            .setPositiveButton("Delete") { _, _ -> deleteAddress(address, onDeleted) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAddress(address: Address, onDeleted: () -> Unit) {
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("userLocation")
                .document(uid)
                .collection("address")
                .whereEqualTo("street", address.street)
                .whereEqualTo("city", address.city)
                .whereEqualTo("zipCode", address.zipCode)
                .get()
                .addOnSuccessListener { qs ->
                    if (qs.isEmpty) {
                        Toast.makeText(this, "Not found", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    val batch = firestore.batch()
                    qs.documents.forEach { batch.delete(it.reference) }
                    batch.commit()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                            onDeleted()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error deleting", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setDefaultAddress(address: Address, onComplete: () -> Unit) {
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("userLocation")
                .document(uid)
                .update("defaultAddress", address)
                .addOnSuccessListener { onComplete() }
                .addOnFailureListener {
                    Toast.makeText(this, "Error setting default", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveSelectedAddress(address: Address) {
        val p = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        p.edit().putString(SELECTED_ADDRESS_KEY, Gson().toJson(address)).apply()
    }

    private fun saveSelectedAddressToFirestore(address: Address) {
        auth.currentUser?.uid?.let { uid ->
            firestore.collection("userLocation")
                .document(uid)
                .collection("selectedAddress")
                .document("profile")
                .set(
                    hashMapOf(
                        "title" to address.title,
                        "street" to address.street,
                        "houseApartment" to address.houseApartment,
                        "city" to address.city,
                        "state" to address.state,
                        "zipCode" to address.zipCode,
                        "fullName" to address.fullName,
                        "phoneNumber" to address.phoneNumber,
                        "savedAt" to FieldValue.serverTimestamp(),
                        "isDefault" to address.isDefault,
                        "current" to address.current
                    )
                )
        }

    }

    private fun loadSelectedAddress(): Address? {
        val p = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return p.getString(SELECTED_ADDRESS_KEY, null)?.let {
            Gson().fromJson(it, Address::class.java)
        }
    }

}


