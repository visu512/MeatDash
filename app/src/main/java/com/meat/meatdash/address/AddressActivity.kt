package com.meat.meatdash.address

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.meat.meatdash.R
import com.meat.meatdash.databinding.ActivityAddressBinding
import com.meat.meatdash.model.Address
import com.meat.meatdash.sharedpref.PrefsHelper

class AddressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddressBinding
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var isDefaultAddress = true
    private var selectedAddressType: String = "Home" // Default selection

    private companion object {
        const val COLLECTION_USERS = "userLocation"
        const val COLLECTION_ADDRESSES = "address"
        const val FIELD_IS_DEFAULT = "isDefault"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_ADDRESS_TYPE = "title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser == null) {
            showToast(R.string.error_not_authenticated)
            finish()
            return
        }

        setupViews()
    }

    private fun setupViews() {
        setupChipGroup()
        setupButtons()
    }

    private fun setupChipGroup() {
        binding.chipGroupAddressType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipHome -> {
                    selectedAddressType = "Home"
                    binding.tilCustomTag.visibility = View.GONE
                }

                R.id.chipWork -> {
                    selectedAddressType = "Work"
                    binding.tilCustomTag.visibility = View.GONE
                }

                R.id.chipFriend -> {
                    selectedAddressType = "Friends & Family"
                    binding.tilCustomTag.visibility = View.GONE
                }

                R.id.chipOther -> {
                    binding.tilCustomTag.visibility = View.VISIBLE
                    binding.etCustomTag.text?.clear()
                }
            }
        }

        // Default selection
        binding.chipHome.isChecked = true

        val chips =
            listOf(binding.chipHome, binding.chipWork, binding.chipFriend, binding.chipOther)
        val chipTextColor = resources.getColorStateList(R.color.chip_text_color_selector, theme)
        chips.forEach { chip ->
            chip.setTextColor(chipTextColor)
        }

    }

    private fun setupButtons() {
        binding.btnSaveProfile.setOnClickListener {
            if (validateForm()) {
                saveAddress()
            }
        }
    }

    private fun validateForm(): Boolean {
        with(binding) {
            tilStreetAddress.error = null
            tilCity.error = null
            tilState.error = null
            tilZipCode.error = null
            tilCustomTag.error = null

            var isValid = true

            if (etStreetAddress.text.isNullOrBlank()) {
                tilStreetAddress.error = getString(R.string.error_street_address)
                isValid = false
            }

            if (etCity.text.isNullOrBlank()) {
                tilCity.error = getString(R.string.error_city)
                isValid = false
            }

            if (etState.text.isNullOrBlank()) {
                tilState.error = getString(R.string.error_state)
                isValid = false
            }

            if (etZipCode.text.isNullOrBlank()) {
                tilZipCode.error = getString(R.string.error_zip_code)
                isValid = false
            }

            if (chipOther.isChecked && etCustomTag.text.isNullOrBlank()) {
                tilCustomTag.error = getString(R.string.error_custom_tag)
                isValid = false
            }

            return isValid
        }
    }

    private fun saveAddress() {
        val userId = auth.currentUser?.uid ?: return

        val addressType = if (binding.chipOther.isChecked) {
            binding.etCustomTag.text.toString().trim()
        } else {
            selectedAddressType
        }

        // Load fullName and phoneNumber from SharedPreferences
        val fullName = PrefsHelper.getString(this, "fullName") ?: ""
        val phoneNumber = PrefsHelper.getString(this, "phoneNumber") ?: ""

        val newAddress = hashMapOf(
            "fullName" to fullName,
            "phoneNumber" to phoneNumber,
            "street" to binding.etStreetAddress.text.toString().trim(),
            "houseApartment" to binding.etHouseApartment.text.toString().trim(),
            "city" to binding.etCity.text.toString().trim(),
            "state" to binding.etState.text.toString().trim(),
            "zipCode" to binding.etZipCode.text.toString().trim(),
            FIELD_ADDRESS_TYPE to addressType,
            FIELD_IS_DEFAULT to isDefaultAddress,
            FIELD_CREATED_AT to FieldValue.serverTimestamp()
        )

        showLoading(true)

        if (isDefaultAddress) {
            updateExistingDefaultAddresses(userId, newAddress)
        } else {
            addNewAddress(userId, newAddress)
        }
    }

    private fun updateExistingDefaultAddresses(userId: String, newAddress: HashMap<String, Any>) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_ADDRESSES)
            .whereEqualTo(FIELD_IS_DEFAULT, true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                querySnapshot.documents.forEach { document ->
                    batch.update(document.reference, FIELD_IS_DEFAULT, false)
                }
                batch.commit()
                    .addOnSuccessListener {
                        addNewAddress(userId, newAddress)
                    }
                    .addOnFailureListener { e ->
                        handleError(e)
                    }
            }
            .addOnFailureListener { e ->
                handleError(e)
            }
    }



    private fun addNewAddress(userId: String, address: HashMap<String, Any>) {
        db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_ADDRESSES)
            .add(address)
            .addOnSuccessListener { documentRef ->
                showToast(R.string.success_address_saved)
                // Build Address object from saved data plus doc id
                val savedAddress = Address(
                    // Assign fields from address map or add as needed
                    title = address[FIELD_ADDRESS_TYPE] as? String ?: "",
                    street = address["street"] as? String ?: "",
                    houseApartment = address["houseApartment"] as? String ?: "",
                    city = address["city"] as? String ?: "",
                    state = address["state"] as? String ?: "",
                    zipCode = address["zipCode"] as? String ?: "",
                    fullName = address["fullName"] as? String ?: "",
                    phoneNumber = address["phoneNumber"] as? String ?: "",
                    isDefault = address[FIELD_IS_DEFAULT] as? Boolean ?: false
                )
                val data = Intent().apply {
                    putExtra("address", savedAddress) // Address must implement Serializable or Parcelable
                }
                setResult(RESULT_OK, data)
                finish()
            }
            .addOnFailureListener { e ->
                handleError(e)
            }
            .addOnCompleteListener {
                showLoading(false)
            }
    }




    private fun handleError(exception: Exception) {
        showToast(getString(R.string.error_saving_address, exception.localizedMessage ?: ""))
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSaveProfile.isEnabled = !show
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    private fun showToast(messageResId: Int, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, getString(messageResId), duration).show()
    }
}