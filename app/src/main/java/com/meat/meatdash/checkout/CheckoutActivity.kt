package com.meat.meatdash.checkout

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.meat.meatdash.CartManager
import com.meat.meatdash.R
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

    private val SHARED_PREFS_NAME = "MeatDashPrefs"
    private val SELECTED_ADDRESS_KEY = "selected_address"
    private val SHIPPING_CHARGE = 40.0
    private val GST_PERCENTAGE = 5.0
    private val RAZORPAY_KEY = "rzp_test_tH7nTQabmzh9IE"


    // 1) Launcher for AddressActivity (adding/editing an address)
    private val addressActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedAddress = result.data?.getSerializableExtra("address") as? Address
            updatedAddress?.let { updateSelectedAddress(it) }
        }
    }

    // 2) Launcher for CurrentLocationActivity (fetching current location as an address)
    private val currentLocationActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedAddress = result.data?.getSerializableExtra("address") as? Address
            updatedAddress?.let { updateSelectedAddress(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupProgressDialog()
        loadAddress()
        updateOrderSummary()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        itemList = CartManager.getCartItems()
        adapter = CheckoutItemsAdapter(itemList)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CheckoutActivity)
            adapter = this@CheckoutActivity.adapter
        }
    }

    private fun setupProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setMessage("Placing your order...")
            setCancelable(false)
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { onBackPressed() }

//        binding.btnPlaceOrder.setOnClickListener {
//            if (validateAndSaveDeliveryAddress()) {
//                startRazorpayPayment()
//            }
//        }


        binding.btnPlaceOrder.setOnClickListener {
            // 1) If cart is empty → show toast
            if (itemList.isEmpty()) {
                Toast.makeText(
                    this,
                    "Your cart is empty. Add items before placing an order.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // 2) If no address selected → show toast
            if (selectedAddress == null) {
                Toast.makeText(
                    this,
                    "Please select a delivery address before placing an order.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // 3) Both conditions satisfied → proceed to payment
            startRazorpayPayment()
        }


        // cardview always white
        binding.addressCard.setCardBackgroundColor(Color.WHITE)

        binding.btnChangeAddress.setOnClickListener {
            showAddressBottomSheet()
        }

        binding.btnAddOrSelectAddress.setOnClickListener {
            showAddressBottomSheet()
        }
    }

    private fun showAddressBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.DialogAnimation)
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_address)
        bottomSheetDialog.setCancelable(true)
        bottomSheetDialog.setCanceledOnTouchOutside(true)
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true

        bottomSheetDialog.findViewById<View>(R.id.btnClose)
            ?.setOnClickListener { bottomSheetDialog.dismiss() }

        bottomSheetDialog.findViewById<View>(R.id.btnCurrentAddress)
            ?.setOnClickListener {
                bottomSheetDialog.dismiss()
                // Launch CurrentLocationActivity via the launcher
                currentLocationActivityLauncher.launch(
                    Intent(this, CurrentLocationActivity::class.java)
                )
            }

        bottomSheetDialog.findViewById<View>(R.id.btnAddNewAddress)
            ?.setOnClickListener {
                bottomSheetDialog.dismiss()
                // Launch AddressActivity via the launcher
                addressActivityLauncher.launch(Intent(this, AddressActivity::class.java))
            }

        // RecyclerView of saved addresses inside the bottom sheet
        val recyclerView =
            bottomSheetDialog.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSavedAddresses)
        recyclerView?.layoutManager = LinearLayoutManager(this)

        loadSavedAddresses { addresses ->
            recyclerView?.adapter = AddressAdapter(
                addresses,
                onEditClick = { addressToEdit ->
                    addressActivityLauncher.launch(
                        Intent(this, AddressActivity::class.java).apply {
                            putExtra("address_to_edit", addressToEdit)
                        }
                    )
                    bottomSheetDialog.dismiss()
                },
                onDeleteClick = { addressToDelete ->
                    showDeleteAddressDialog(addressToDelete) {
                        loadSavedAddresses { refreshed ->
                            (recyclerView?.adapter as AddressAdapter).updateAddresses(refreshed)
                        }
                    }
                },
                onSetDefault = { addressToDefault ->
                    setDefaultAddress(addressToDefault) {
                        updateSelectedAddress(addressToDefault)
                        bottomSheetDialog.dismiss()
                    }
                },
                onAddressSelected = { addressSelected ->
                    updateSelectedAddress(addressSelected)
                    bottomSheetDialog.dismiss()
                }
            )
        }

        bottomSheetDialog.show()
    }

    private fun loadAddress() {
        // 1) If this activity was launched with an "address" extra, use that:
        (intent.getSerializableExtra("address") as? Address)?.let { address ->
            updateSelectedAddress(address)
            return
        }

        // 2) Else, load from SharedPreferences if available:
        loadSelectedAddress()?.let { address ->
            updateSelectedAddress(address)
            return
        }

        // 3) Else, try to load from Firestore "selectedAddress/profile"
        val currentUser = auth.currentUser ?: return updateInitialButtonVisibility()
        firestore.collection("userLocation")
            .document(currentUser.uid)
            .collection("selectedAddress")
            .document("profile")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val data = snapshot.data ?: emptyMap()
                    val savedAtTimestamp = data["savedAt"] as? com.google.firebase.Timestamp
                    val savedAtLong = savedAtTimestamp?.toDate()?.time ?: 0L

                    val address = Address(
                        title = data["title"] as? String ?: "",
                        street = data["street"] as? String ?: "",
                        houseApartment = data["houseApartment"] as? String ?: "",
                        city = data["city"] as? String ?: "",
                        state = data["state"] as? String ?: "",
                        zipCode = data["zipCode"] as? String ?: "",
                        fullName = data["fullName"] as? String ?: "",
                        phoneNumber = data["phoneNumber"] as? String ?: "",
                        savedAt = savedAtLong,
                        isDefault = data["isDefault"] as? Boolean ?: false,
                        current = data["current"] as? String ?: ""
                    )
                    updateSelectedAddress(address)
                } else {
                    binding.textAddressDetails.text = currentUser.displayName ?: ""
                    updateInitialButtonVisibility()
                }
            }
            .addOnFailureListener {
                binding.textAddressDetails.text = currentUser.displayName ?: ""
                updateInitialButtonVisibility()
            }
    }

    private fun updateSelectedAddress(address: Address) {
        selectedAddress = address
        saveSelectedAddress(address)
        saveSelectedAddressToFirestore(address)
        displayAddress(address)
        updateInitialButtonVisibility()
    }

    private fun updateInitialButtonVisibility() {
        val hasAddress = selectedAddress != null
        binding.tvDeliveryAddress.visibility = if (hasAddress) View.VISIBLE else View.GONE
        binding.addressCard.visibility = if (hasAddress) View.VISIBLE else View.GONE
        binding.btnChangeAddress.visibility = if (hasAddress) View.VISIBLE else View.GONE
        binding.btnPlaceOrder.visibility = if (hasAddress) View.VISIBLE else View.GONE
        binding.btnAddOrSelectAddress.visibility = if (hasAddress) View.GONE else View.VISIBLE
    }

    private fun displayAddress(address: Address) {
        binding.addressCard.visibility = View.VISIBLE
        binding.btnChangeAddress.visibility = View.VISIBLE
        binding.btnAddOrSelectAddress.visibility = View.GONE

        binding.textAddressTitle.text = address.title
        binding.textAddressDetails.text = buildString {
            if (address.fullName.isNotEmpty()) append("${address.fullName}\n")
            if (address.phoneNumber.isNotEmpty()) append("${address.phoneNumber}\n")
            append(address.street)
            if (address.houseApartment.isNotEmpty()) append(", ${address.houseApartment}")
            append(", ${address.city}, ${address.state} ${address.zipCode}")
        }
    }

    private fun validateAndSaveDeliveryAddress(): Boolean {
        return if (selectedAddress != null) {
            true
        } else {
            Toast.makeText(this, "Please select a delivery address", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun startRazorpayPayment() {
        try {
            val amountInPaise = (orderTotal * 100).toInt()

            val checkout = Checkout()
            checkout.setKeyID(RAZORPAY_KEY)

            val options = JSONObject().apply {
                put("name", "Meat Me Up")
                put("description", "Order Payment")
                put("image", "https://i.postimg.cc/RZ8XgpTD/appIcon.png") // app icon link
                put("theme.color", "#F7A687")
                put("currency", "INR")
                put("amount", amountInPaise)             // customer@example.com
                put("prefill", JSONObject().apply {
                    put("email", auth.currentUser?.email ?: "aiptimus1999@gmail.com")
                    put("name", selectedAddress?.fullName ?: selectedAddress?.title ?: "Customer")
                })
            }

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show()
        processOrder(razorpayPaymentId)
    }

    override fun onPaymentError(errorCode: Int, response: String?) {
        Toast.makeText(this, "Payment Failed! Please try again.", Toast.LENGTH_LONG).show()
    }

    private fun processOrder(paymentId: String?) {
        progressDialog.show()

        val shopName = if (itemList.isNotEmpty()) itemList[0].shopName else ""
        val orderId = generateOrderId()
        val userId = auth.currentUser?.uid ?: "guest"

        val subtotal = itemList.sumOf { it.price * it.weight / 1000.0 }
        val tax = (subtotal * GST_PERCENTAGE) / 100
        val shipping = SHIPPING_CHARGE
        val total = subtotal + tax + shipping

        val orderData = hashMapOf(
            "orderId" to orderId,
            "userId" to userId,
            "title" to (selectedAddress?.title ?: ""),
            "streetAddress" to (selectedAddress?.street ?: ""),
            "houseApartment" to (selectedAddress?.houseApartment ?: ""),
            "city" to (selectedAddress?.city ?: ""),
            "state" to (selectedAddress?.state ?: ""),
            "zipCode" to (selectedAddress?.zipCode ?: ""),
            "fullName" to (selectedAddress?.fullName ?: ""),
            "phoneNumber" to (selectedAddress?.phoneNumber ?: ""),
            "subtotal" to subtotal,
            "shipping" to shipping,
            "tax" to tax,
            "total" to total,
            "shopName" to shopName,
            "paymentId" to paymentId,
            "status" to "paid",
            "timestamp" to System.currentTimeMillis(),
            "deliveryDate" to calculateDeliveryDate(),
            "items" to itemList.map { item ->
                mapOf(
                    "id" to item.id,
                    "name" to item.name,
                    "price" to (item.price * item.weight / 1000.0),
                    "quantity" to item.weight,
                    "image" to item.imageBase64
                )
            }
        )

        firestore.collection("OrderList")
            .document(orderId)
            .set(orderData)
            .addOnSuccessListener {
                progressDialog.dismiss()
                CartManager.clearCart()
                navigateToOrderSuccess(orderId)
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to place order: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun generateOrderId(): String {
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        val randomNum = (1000..9999).random()
        return "ORD-${dateFormat.format(Date())}-$randomNum"
    }

    private fun calculateDeliveryDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 3)
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun navigateToOrderSuccess(orderId: String) {
        val intent = Intent(this, OrderSuccessActivity::class.java).apply {
            putExtra("orderId", orderId)
            putExtra("deliveryDate", calculateDeliveryDate())
            putExtra("address", selectedAddress)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun loadSavedAddresses(onComplete: (List<Address>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("userLocation")
            .document(userId)
            .collection("address")
            .get()
            .addOnSuccessListener { snapshot ->
                val addresses = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
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
                        savedAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
                            ?: 0L,
                        isDefault = data["isDefault"] as? Boolean ?: false
                    )
                }
                onComplete(addresses)
            }
            .addOnFailureListener {
                onComplete(emptyList())
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
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("userLocation")
            .document(userId)
            .collection("address")
            .whereEqualTo("street", address.street)
            .whereEqualTo("city", address.city)
            .whereEqualTo("zipCode", address.zipCode)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(this, "Address not found to delete", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val batch = firestore.batch()
                for (document in querySnapshot.documents) {
                    batch.delete(document.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Address deleted", Toast.LENGTH_SHORT).show()
                        onDeleted()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete address", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error deleting address", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setDefaultAddress(address: Address, onComplete: () -> Unit) {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("userLocation")
                .document(userId)
                .update("defaultAddress", address)
                .addOnSuccessListener { onComplete() }
                .addOnFailureListener {
                    Toast.makeText(this, "Error setting default address", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveSelectedAddress(address: Address) {
        val prefs: SharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(SELECTED_ADDRESS_KEY, Gson().toJson(address)).apply()
    }

    private fun saveSelectedAddressToFirestore(address: Address) {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("userLocation")
            .document(userId)
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
            .addOnSuccessListener {
                // Optional: log success
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save selected address", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSelectedAddress(): Address? {
        val prefs: SharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(SELECTED_ADDRESS_KEY, null)
        return if (!jsonString.isNullOrEmpty()) {
            Gson().fromJson(jsonString, Address::class.java)
        } else {
            null
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateOrderSummary() {
        val subtotal = CartManager.getCartItems()
            .sumOf { it.price * it.weight / 1000.0 }
        val tax = (subtotal * GST_PERCENTAGE) / 100
        val shipping = SHIPPING_CHARGE
        orderTotal = subtotal + tax + shipping

        binding.textSubtotal.text = "₹%.2f".format(subtotal)
        binding.textTax.text = "₹%.2f".format(tax)
        binding.textShipping.text = "₹%.2f".format(shipping)
        binding.textTotal.text = "₹%.2f".format(orderTotal)
    }
}
