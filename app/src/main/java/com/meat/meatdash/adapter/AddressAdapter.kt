package com.meat.meatdash.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.meat.meatdash.R
import com.meat.meatdash.model.Address

class AddressAdapter(
    private var addresses: List<Address>,
    private val onEditClick: (Address) -> Unit,
    private val onDeleteClick: (Address) -> Unit,
    private val onSetDefault: (Address) -> Unit,
    private val onAddressSelected: (Address) -> Unit
) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    fun updateAddresses(newAddresses: List<Address>) {
        this.addresses = newAddresses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.address_item, parent, false)
        return AddressViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(addresses[position])
    }

    override fun getItemCount() = addresses.size

    inner class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvAddressTitle)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddressDetails)
        private val btnSelect: TextView = itemView.findViewById(R.id.btnSelectAddress)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteAddress)

        fun bind(address: Address) {
            // Set title
            tvTitle.text = address.title

            // Build full address string with fullName and phoneNumber at top lines
            tvAddress.text = buildString {
                if (address.fullName.isNotEmpty()) append("${address.fullName}\n")
                if (address.phoneNumber.isNotEmpty()) append("${address.phoneNumber}\n")
                append(address.street)
                if (address.houseApartment.isNotEmpty()) append(", ${address.houseApartment}")
                append(", ${address.city}, ${address.state} ${address.zipCode}")
            }

            // SELECT button triggers onAddressSelected callback
            btnSelect.setOnClickListener {
                onAddressSelected(address)
            }

            // DELETE icon triggers onDeleteClick callback
            btnDelete.setOnClickListener {
                onDeleteClick(address)
            }
        }
    }
}
