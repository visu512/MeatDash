package com.meat.meatdash.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.meat.meatdash.CartManager
import com.meat.meatdash.R
import com.meat.meatdash.adapter.AddressAdapter
import com.meat.meatdash.adapter.OrderAdapter
import com.meat.meatdash.adapter.ShopAdapter
import com.meat.meatdash.address.AddressActivity
import com.meat.meatdash.currentloc.CurrentLocationActivity
import com.meat.meatdash.databinding.ActivityMainBinding
import com.meat.meatdash.model.Address
import com.meat.meatdash.model.Order
import com.meat.meatdash.model.Shop
import com.meat.meatdash.orders.OrderActivity
import com.meat.meatdash.profile.ProfileActivity
import com.meat.meatdash.user.LoginActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Track-order bottom sheet fields
    private var trackSheet: BottomSheetDialog? = null
    private var trackTimer: CountDownTimer? = null
    private var activeOrder: Order? = null


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val REQUEST_CURRENT_LOCATION = 1002
    }

    // Drawer header
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private var fabHideRunnable: Runnable? = null


    private val SHARED_PREFS_NAME = "MeatDashPrefs"
    private val SELECTED_ADDRESS_KEY = "selected_address"
    private val KEY_FINISH_PREFIX = "FINISH_TIME_"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


//        val nav = findViewById<BottomNavigationView>(R.id.)
//        nav.setOnNavigationItemSelectedListener { item ->
//            when(item.itemId) {
//                R.id.navigation_home -> { /* already here */ true }
//                R.id.navigation_orders -> { startActivity(Intent(this, OrderActivity::class.java)); true }
//                R.id.navigation_profile -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
//                else -> false
//            }
//        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        CartManager.init(applicationContext)

        setupUI()
        setupAddressBar()
        initializeNavigationDrawerHeader()
        setupNavigationDrawer()

        // Load saved address
        loadSelectedAddress()?.let { updateAddressViews(it) }

        // Fetch shops
        fetchShops()

        // go to order activity
        binding.fabNewOrder.setOnClickListener() {
            val intent = Intent(this, OrderActivity::class.java)
            startActivity(intent)
        }

    }


    override fun onResume() {
        super.onResume()
        updateBottomCartBar()
        checkLocationPermissionAndFetch()
        updateUserInfoInDrawer()
        loadSelectedAddress()?.let { updateAddressViews(it) }
        updateFabVisibility()
    }


    // order tracking button
    private fun updateFabVisibility() {
        val prefs = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        val remaining = prefs.all
            .filterKeys { it.startsWith(KEY_FINISH_PREFIX) }
            .mapNotNull { (_, value) ->
                (value as? Long)?.let { finishTs ->
                    val diff = finishTs - now
                    if (diff > 0) diff else null
                }
            }

        fabHideRunnable?.let { binding.fabNewOrder.removeCallbacks(it) }

        if (remaining.isNotEmpty()) {
            binding.fabNewOrder.visibility = View.VISIBLE

            val minLeft = remaining.minOrNull()!!
            fabHideRunnable = Runnable {
                binding.fabNewOrder.visibility = View.GONE
            }
            binding.fabNewOrder.postDelayed(fabHideRunnable, minLeft)
        } else {
            binding.fabNewOrder.visibility = View.GONE
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    private fun setupUI() {
        // Image slider
        val imageList = listOf(
            SlideModel(R.drawable.slider1, ScaleTypes.FIT),
            SlideModel(R.drawable.slider2, ScaleTypes.FIT),
            SlideModel(R.drawable.slider3, ScaleTypes.FIT),
            SlideModel(R.drawable.slider4, ScaleTypes.FIT),
            SlideModel(R.drawable.banner5, ScaleTypes.FIT)
        )
        binding.imageSlider.setImageList(imageList)

        // RecyclerView placeholder
        binding.shopRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyTextView.visibility = View.GONE

        // Cart bottom bar
        binding.bottomBar.btnViewCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
    }

    private fun fetchShops() {
        db.collection("Shops").get()
            .addOnSuccessListener { result ->
                binding.progressBar.visibility = View.GONE
                val shopList = result.map { doc ->
                    doc.toObject(Shop::class.java).copy(id = doc.id)
                }
                if (shopList.isNotEmpty()) {
                    binding.shopRecyclerView.adapter = ShopAdapter(shopList) { shop ->
                        val intent = Intent(this, ShopDetailActivity::class.java).apply {
                            putExtra("shopRegId", shop.id)
                            putExtra("shopName", shop.shopName)
                            putExtra("shopDescription", shop.shopDescription ?: "No description")
                            putExtra("shopRating", shop.rating)
                            putExtra("shopReviewCount", shop.reviewCount)
                            putExtra("shopCategory", shop.category)
                            putExtra("shopLocation", shop.shopLocation)
                        }
                        startActivity(intent)
                    }
                    binding.shopRecyclerView.visibility = View.VISIBLE
                } else {
                    binding.emptyTextView.apply {
                        visibility = View.VISIBLE
                        text = "No shops found"
                    }
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.emptyTextView.apply {
                    visibility = View.VISIBLE
                    text = "Failed to load shops: ${e.message}"
                }
            }
    }


    private fun initializeNavigationDrawerHeader() {
        val headerView = binding.navView.getHeaderView(0)
        profileImage = headerView.findViewById(R.id.profileImage)
        userName = headerView.findViewById(R.id.userName)
        userEmail = headerView.findViewById(R.id.userEmail)
    }

    private fun updateUserInfoInDrawer() {
        val user = auth.currentUser
        user?.let {
            userName.text = it.displayName ?: "User"
            userEmail.text = it.email ?: "No Email"
            profileImage.setImageBitmap(
                generateInitialBitmap(it.email?.firstOrNull()?.uppercaseChar() ?: 'U')
            )
        } ?: run {
            userName.text = "Guest"
            userEmail.text = "Not logged in"
            profileImage.setImageResource(R.drawable.placeholder)
        }
    }

    private fun generateInitialBitmap(letter: Char): Bitmap {
        val size = 200
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.colorPrimary)
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = size * 0.5f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        canvas.drawText(
            letter.toString(),
            size / 2f,
            size / 2f - (textPaint.ascent() + textPaint.descent()) / 2,
            textPaint
        )
        return bitmap
    }

    private fun setupNavigationDrawer() {
        binding.profileButton.setOnClickListener {
            updateUserInfoInDrawer()
            with(binding.drawerLayout) {
                if (isDrawerOpen(GravityCompat.START)) closeDrawer(GravityCompat.START) else openDrawer(
                    GravityCompat.START
                )
            }
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            val handled = when (menuItem.itemId) {

                R.id.nav_info -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }

                R.id.nav_orders -> {
                    startActivity(Intent(this, OrderActivity::class.java))
                    true
                }

                R.id.nav_help -> {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:aiptimus1999@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Help Request")
                    }
                    if (intent.resolveActivity(packageManager) != null) startActivity(intent)
                    else Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_privicy -> {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://privicy-policies.netlify.app/")
                        )
                    )
                    true
                }

                R.id.nav_share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Check out this cool app!")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Hey, check out this awesome app: https://play.google.com/store/apps/details?id=$packageName"
                        )
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share via"))
                    true
                }

                R.id.nav_rate -> {
                    val uri = Uri.parse("market://details?id=$packageName")
                    val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    try {
                        startActivity(goToMarket)
                    } catch (e: Exception) {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                            )
                        )
                    }
                    true
                }

                R.id.nav_logout -> {
                    // Show confirmation dialog instead of logging out immediately
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes") { _, _ ->
                            // User confirmed — proceed with logout
                            performLogout()
                        }
                        .setNegativeButton("No", null)
                        .show()
                    true
                }


                else -> false
            }
            if (handled) binding.drawerLayout.closeDrawer(GravityCompat.START)
            handled
        }
    }

    private fun performLogout() {
        // 1) Remove saved address from SharedPreferences
        val prefs = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(SELECTED_ADDRESS_KEY)
            .apply()

        // 2) Remove the “selectedAddress” field in Firestore (optional cleanup)
        auth.currentUser?.uid?.let { uid ->
            db.collection("userLocation")
                .document(uid)
                .update("selectedAddress", FieldValue.delete())
        }

        // 3) Sign out from FirebaseAuth
        auth.signOut()

        // 4) Clear the in-memory & persisted cart
        CartManager.clearCart()

        // 5) Launch LoginActivity and clear the back stack
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })

        // 6) Finish MainActivity
        finish()
    }


    private fun setupAddressBar() {
        binding.openLocationBottomBar.setOnClickListener {
            showAddressBottomSheet()
        }
    }

    private fun showAddressBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.DialogAnimation)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_address)
        bottomSheetDialog.setCancelable(false)
        bottomSheetDialog.setCanceledOnTouchOutside(false)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true

        bottomSheetDialog.findViewById<View>(R.id.btnClose)
            ?.setOnClickListener { bottomSheetDialog.dismiss() }
        bottomSheetDialog.findViewById<View>(R.id.btnAddNewAddress)?.setOnClickListener {
            startActivity(Intent(this, AddressActivity::class.java))
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.findViewById<View>(R.id.btnCurrentAddress)?.setOnClickListener {
            bottomSheetDialog.dismiss()
            startActivityForResult(
                Intent(this, CurrentLocationActivity::class.java),
                REQUEST_CURRENT_LOCATION
            )
        }

        val recyclerView = bottomSheetDialog.findViewById<RecyclerView>(R.id.rvSavedAddresses)
        recyclerView?.layoutManager = LinearLayoutManager(this)

        loadAllAddresses { allAddresses ->
            recyclerView?.adapter = AddressAdapter(
                allAddresses,
                onEditClick = { address ->
                    val intent = Intent(this, AddressActivity::class.java).apply {
                        putExtra("address_to_edit", address)
                    }
                    startActivity(intent)
                    bottomSheetDialog.dismiss()
                },
                onDeleteClick = { address ->
                    showDeleteAddressDialog(address) {
                        loadAllAddresses { refreshed ->
                            (recyclerView?.adapter as? AddressAdapter)?.updateAddresses(refreshed)
                        }
                    }
                },
                onSetDefault = { address ->
                    setDefaultAddress(address) {
                        saveSelectedAddress(address)
                        updateAddressViews(address)
                        bottomSheetDialog.dismiss()
                    }
                },
                onAddressSelected = { address ->
                    saveSelectedAddress(address)
                    updateAddressViews(address)
                    bottomSheetDialog.dismiss()
                }
            )
        }

        bottomSheetDialog.show()
    }

    private fun loadAllAddresses(onComplete: (List<Address>) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            onComplete(emptyList())
            return
        }

        val addresses = mutableListOf<Address>()

        db.collection("userLocation")
            .document(userId)
            .collection("selectedAddress")
            .document("profile")
            .get()
            .addOnSuccessListener { doc ->
                doc?.let {
                    val data = it.data
                    if (data != null) {
                        val savedAtTimestamp = data["savedAt"] as? com.google.firebase.Timestamp
                        val savedAtLong = savedAtTimestamp?.toDate()?.time ?: 0L

                        val profileAddress = Address(
                            fullName = data["fullName"] as? String ?: "",
                            phoneNumber = data["phoneNumber"] as? String ?: "",
                            street = data["street"] as? String ?: "",
                            houseApartment = data["houseApartment"] as? String ?: "",
                            city = data["city"] as? String ?: "",
                            state = data["state"] as? String ?: "",
                            zipCode = data["zipCode"] as? String ?: "",
                            current = data["current"] as? String ?: "",
                            title = data["title"] as? String ?: "",
                            savedAt = savedAtLong,
                            isDefault = data["isDefault"] as? Boolean ?: false
                        )
                        addresses.add(profileAddress)
                    }
                }

                db.collection("userLocation")
                    .document(userId)
                    .collection("address")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val savedAddresses = snapshot.documents.mapNotNull { doc ->
                            val data = doc.data
                            if (data != null) {
                                val savedAtTimestamp =
                                    data["savedAt"] as? com.google.firebase.Timestamp
                                val savedAtLong = savedAtTimestamp?.toDate()?.time ?: 0L

                                Address(
                                    fullName = data["fullName"] as? String ?: "",
                                    phoneNumber = data["phoneNumber"] as? String ?: "",
                                    street = data["street"] as? String ?: "",
                                    houseApartment = data["houseApartment"] as? String ?: "",
                                    city = data["city"] as? String ?: "",
                                    state = data["state"] as? String ?: "",
                                    zipCode = data["zipCode"] as? String ?: "",
                                    current = data["current"] as? String ?: "",
                                    title = data["title"] as? String ?: "",
                                    savedAt = savedAtLong,
                                    isDefault = data["isDefault"] as? Boolean ?: false
                                )
                            } else null
                        }
                        addresses.addAll(savedAddresses)
                        onComplete(addresses)
                    }
                    .addOnFailureListener {
                        onComplete(addresses)
                    }
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }

    private fun saveSelectedAddress(address: Address) {
        val sharedPref = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(SELECTED_ADDRESS_KEY, Gson().toJson(address))
            apply()
        }
        updateAddressViews(address)
    }

    private fun loadSelectedAddress(): Address? {
        val sharedPref = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPref.getString(SELECTED_ADDRESS_KEY, null)
        return if (json != null) Gson().fromJson(json, Address::class.java) else null
    }

    private fun updateAddressViews(address: Address) {
        binding.textAddressTitle.text = address.title
        binding.textAddressDetails.text = buildString {
            if (address.fullName.isNotEmpty()) append("${address.fullName}\n")
            if (address.phoneNumber.isNotEmpty()) append("${address.phoneNumber}\n")
            append(address.street)
            if (address.houseApartment.isNotEmpty()) append(", ${address.houseApartment}")
            append(", ${address.city}, ${address.state} ${address.zipCode}")
        }
    }

    private fun setDefaultAddress(address: Address, onComplete: () -> Unit) {
        auth.currentUser?.uid?.let { userId ->
            db.collection("userLocation").document(userId)
                .update("defaultAddress", address)
                .addOnSuccessListener { onComplete() }
                .addOnFailureListener {
                    Toast.makeText(this, "Error setting default address", Toast.LENGTH_SHORT).show()
                }
        }
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
        auth.currentUser?.uid?.let { userId ->
            db.collection("userLocation")
                .document(userId)
                .collection("address")
                .whereEqualTo("streetAddress", address.street)
                .whereEqualTo("city", address.city)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.delete()
                    }
                    onDeleted()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error deleting address", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun logoutUser() {
        val sharedPref = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove(SELECTED_ADDRESS_KEY)
            apply()
        }

        auth.currentUser?.uid?.let { userId ->
            db.collection("userLocation").document(userId)
                .update("selectedAddress", FieldValue.delete())
                .addOnCompleteListener {
                    auth.signOut()
                    CartManager.clearCart()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
        }
    }

    private fun updateBottomCartBar() {
        val cartSize = CartManager.getCartItems().size
        binding.bottomBar.root.visibility = if (cartSize > 0) View.VISIBLE else View.GONE
        binding.bottomBar.tvItemCount.text = "$cartSize Item${if (cartSize > 1) "s" else ""}"
    }

    private fun checkLocationPermissionAndFetch() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchUserLocation()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
                requestLocationPermission()
            }

            else -> requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocation()
        } else {
            setDefaultAddress()
        }
    }

    private fun fetchUserLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) return

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { getAddressFromLocation(it) } ?: requestNewLocationData()
            }
        } catch (e: SecurityException) {
            setDefaultAddress()
        }
    }

    private fun getAddressFromLocation(location: Location) {
        try {
            Geocoder(this, Locale.getDefault()).getFromLocation(
                location.latitude,
                location.longitude,
                1
            )?.firstOrNull()?.let { address ->
                updateAddressTextViews(
                    locality = address.locality ?: "Unknown",
                    area = listOfNotNull(address.subLocality, address.adminArea).joinToString(", "),
                    postalCode = address.postalCode ?: ""
                )
            } ?: setDefaultAddress()
        } catch (e: Exception) {
            setDefaultAddress()
        }
    }

    private fun requestNewLocationData() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 5000
            numUpdates = 1
        }
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        fusedLocationClient.removeLocationUpdates(this)
                        locationResult.lastLocation?.let { getAddressFromLocation(it) }
                            ?: setDefaultAddress()
                    }
                },
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            setDefaultAddress()
        }
    }

    private fun updateAddressTextViews(locality: String, area: String, postalCode: String) {
        binding.textAddressTitle.text = locality
        binding.textAddressDetails.text =
            listOf(area, postalCode).filter { it.isNotEmpty() }.joinToString(", ")
    }

    private fun setDefaultAddress() {
        binding.textAddressTitle.text = "Select Address"
        binding.textAddressDetails.text = "Tap to set delivery location"
    }
}




