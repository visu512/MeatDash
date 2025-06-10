package com.meat.meatdash.adapter

import android.graphics.Color
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.meat.meatdash.R
import com.meat.meatdash.model.Order
import com.meat.meatdash.model.OrderItem

class OrderAdapter(
    private val orders: MutableList<Order>,
    private val onDelivered: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    fun clearExpanded() {
        expandedPositions.clear()
        notifyDataSetChanged()
    }

    fun expandAll() {
        expandedPositions.clear()
        orders.indices.forEach { expandedPositions.add(it) }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        // Ensure default expansion: if none explicitly set, expand all
        if (expandedPositions.isEmpty() && orders.isNotEmpty()) {
            orders.indices.forEach { expandedPositions.add(it) }
        }
        holder.bind(orders[position], position)
    }

    override fun onViewRecycled(holder: OrderViewHolder) {
        super.onViewRecycled(holder)
        holder.clearTimer()
    }

    override fun getItemCount(): Int = orders.size

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textOrderId       = itemView.findViewById<TextView>(R.id.textOrderId)
        private val textShopName      = itemView.findViewById<TextView>(R.id.textShopName)
        private val textOrderDate     = itemView.findViewById<TextView>(R.id.textOrderDate)
        private val textOrderTotal    = itemView.findViewById<TextView>(R.id.textOrderTotal)
        private val textPaymentStatus = itemView.findViewById<TextView>(R.id.textPaymentStatus)
        private val textToggleDetails = itemView.findViewById<TextView>(R.id.textToggleDetails)
        private val containerDetails  = itemView.findViewById<LinearLayout>(R.id.containerDetails)
        private val recyclerItems     = itemView.findViewById<RecyclerView>(R.id.recyclerViewOrderItems)
        private val textDeliveryTimer = itemView.findViewById<TextView>(R.id.textDeliveryTimer)
//        private val cardView = itemView.findViewById<CardView>(R.id.cardView)

        private val fusedClient = LocationServices.getFusedLocationProviderClient(itemView.context)
        private var countDownTimer: CountDownTimer? = null
        private lateinit var currentOrder: Order

        fun bind(order: Order, position: Int) {
            currentOrder = order

            // Basic info
            textOrderId.text       = "Order ID: ${order.orderId}"
            textShopName.text      = order.shopName
            textOrderDate.text     = if (order.timestamp > 0L) {
                java.text.SimpleDateFormat("MMM d, hh:mm a", java.util.Locale.getDefault())
                    .format(java.util.Date(order.timestamp))
            } else "N/A"
            textOrderTotal.text    = "Total: ₹%.2f".format(order.total)
            textPaymentStatus.text = order.status

            recyclerItems.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = OrderItemAdapter(order.items)
                isNestedScrollingEnabled = false
            }

            // Delivered state
            if (order.status.equals("Delivered", true)) {
                textDeliveryTimer.text = "Delivered"
                textDeliveryTimer.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.green)
                )
            }

            // Expand/collapse state
            val isExpanded = expandedPositions.contains(position)
            containerDetails.isVisible   = isExpanded
            textToggleDetails.text       = if (isExpanded) "Less…" else "More…"
            textDeliveryTimer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            if (isExpanded && !order.status.equals("Delivered", true)) {
                startTimer(order.timestamp)
            }

            textToggleDetails.setOnClickListener {
                val expand = !containerDetails.isVisible
                containerDetails.isVisible = expand

                if (expand) {
                    expandedPositions.add(position)
                    textToggleDetails.text = "Less…"
                    textDeliveryTimer.visibility = View.VISIBLE
                    if (!order.status.equals("Delivered", true)) {
                        textDeliveryTimer.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.orderActive)
                        )
                        startTimer(order.timestamp)
                    } else {
                        textDeliveryTimer.text = "Delivered"
                    }
                } else {
                    expandedPositions.remove(position)
                    clearTimer()
                    textDeliveryTimer.visibility = View.GONE
                    textToggleDetails.text = "More…"
                }
            }

//            cardView.setCardBackgroundColor(Color.WHITE)
        }

        private fun startTimer(orderTimestamp: Long) {
            clearTimer()
            val windowMs = 2 * 60 * 1000L
            val elapsed  = System.currentTimeMillis() - orderTimestamp
            val msLeft   = (windowMs - elapsed).coerceAtLeast(0L)
            if (msLeft == 0L) {
                deliverNow()
                return
            }

            countDownTimer = object : CountDownTimer(msLeft, 1000) {
                override fun onTick(ms: Long) {
                    val mins   = (ms / 1000) / 60
                    val padded = mins.toString().padStart(2, '0')
                    val html   = "Order will be delivered in: " +
                            "<font color=\"#FF5722\"><b>$padded mins</b></font>"
                    textDeliveryTimer.text = HtmlCompat.fromHtml(
                        html, HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    textDeliveryTimer.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.orderActive)
                    )
                }
                override fun onFinish() = deliverNow()
            }.start()
        }

        private fun deliverNow() {
            textDeliveryTimer.text = "Delivered"
            textDeliveryTimer.setTextColor(
                ContextCompat.getColor(itemView.context, R.color.green)
            )
            currentOrder.status = "Delivered"
            onDelivered(currentOrder)
        }

        fun clearTimer() {
            countDownTimer?.cancel()
            countDownTimer = null
        }
    }
}

//
//
//
//package com.meat.meatdash.adapter
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.location.Geocoder
//import android.location.Location
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import androidx.core.view.isVisible
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.google.android.gms.location.LocationServices
//import com.meat.meatdash.R
//import com.meat.meatdash.databinding.ItemOrderBinding
//import com.meat.meatdash.model.Order
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.io.IOException
//
//class OrderAdapter(
//    private val orders: MutableList<Order>,
//    private val onDelivered: (Order) -> Unit
//) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {
//
//    // all items expanded by default
//    private val expandedPositions = orders.indices.toMutableSet()
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
//        val binding = ItemOrderBinding.inflate(
//            LayoutInflater.from(parent.context), parent, false
//        )
//        return OrderViewHolder(binding, parent.context)
//    }
//
//    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
//        holder.bind(orders[position], position)
//    }
//
//    override fun getItemCount(): Int = orders.size
//
//    fun expandAll() {
//        expandedPositions.clear()
//        orders.indices.forEach { expandedPositions.add(it) }
//        notifyDataSetChanged()
//    }
//
//    fun clearExpanded() {
//        expandedPositions.clear()
//        notifyDataSetChanged()
//    }
//
//    inner class OrderViewHolder(
//        private val binding: ItemOrderBinding,
//        private val ctx: Context
//    ) : RecyclerView.ViewHolder(binding.root) {
//
//        private val fusedClient = LocationServices
//            .getFusedLocationProviderClient(ctx)
//
//        @SuppressLint("MissingPermission")
//        fun bind(order: Order, position: Int) {
//            // basic order info
//            binding.textOrderId.text = "Order ID: ${order.orderId}"
//            binding.textShopName.text = order.shopName
//            binding.textOrderDate.text = if (order.timestamp > 0L) {
//                java.text.SimpleDateFormat(
//                    "MMM d, hh:mm a", java.util.Locale.getDefault()
//                ).format(java.util.Date(order.timestamp))
//            } else "N/A"
//            binding.textOrderTotal.text = "Total: ₹%.2f".format(order.total)
//            binding.textPaymentStatus.text = order.status
//
//            // items list
//            binding.recyclerViewOrderItems.apply {
//                layoutManager = LinearLayoutManager(ctx)
//                adapter = OrderItemAdapter(order.items)
//                isNestedScrollingEnabled = false
//            }
//
//            // delivered label
//            if (order.status.equals("Delivered", true)) {
//                binding.textDeliveryTimer.text = "Delivered"
//                binding.textDeliveryTimer.setTextColor(ctx.getColor(R.color.green))
//            }
//
//            // prefill current location into start input
//            fusedClient.lastLocation
//                .addOnSuccessListener { loc ->
//                    val address = loc?.let { reverseGeocode(it.latitude, it.longitude) }
//                    binding.inputStartPoint.setText(address ?: "Current location")
//                }
//                .addOnFailureListener {
//                    binding.inputStartPoint.setText("Current location")
//                }
//
//            // expand/collapse logic
//            val expanded = expandedPositions.contains(position)
//            binding.containerDetails.isVisible = expanded
//            binding.textToggleDetails.text = if (expanded) "Less…" else "More…"
//
//            if (expanded) {
//                binding.buttonCalcRoute.setOnClickListener {
//                    val start = binding.inputStartPoint.text.toString().trim()
//                    val end = binding.inputEndPoint.text.toString().trim()
//                    if (start.isNotEmpty() && end.isNotEmpty()) {
//                        calculateRoute(start, end)
//                    }
//                }
//            } else binding.buttonCalcRoute.setOnClickListener(null)
//
//            binding.textToggleDetails.setOnClickListener {
//                if (expanded) expandedPositions.remove(position)
//                else expandedPositions.add(position)
//                notifyItemChanged(position)
//            }
//        }
//
//        private fun calculateRoute(start: String, end: String) {
//            CoroutineScope(Dispatchers.Main).launch {
//                val origin = geocode(start)
//                val dest = geocode(end)
//                if (origin == null || dest == null) {
//                    binding.textDistance.text = "Distance: —"
//                    binding.textEta.text = "Bike ETA: —"
//                    binding.textFuelCost.text = "Fuel cost: —"
//                    return@launch
//                }
//                val results = FloatArray(1)
//                Location.distanceBetween(
//                    origin.latitude, origin.longitude,
//                    dest.latitude, dest.longitude, results
//                )
//                val distKm = results[0] / 1000.0
//                val etaMin = ((distKm / 15.0) * 60).toInt().coerceAtLeast(1)
//                val fuelCost = (distKm / 40.0) * 100.0
//
//                binding.textDistance.text = "Distance: %.2f km".format(distKm)
//                binding.textEta.text = "Bike ETA: $etaMin min"
//                binding.textFuelCost.text = "Fuel cost: ₹%.2f".format(fuelCost)
//            }
//        }
//
//        private fun reverseGeocode(lat: Double, lng: Double): String? {
//            return try {
//                val list = Geocoder(ctx).getFromLocation(lat, lng, 1)
//                list?.firstOrNull()?.getAddressLine(0)
//            } catch (_: IOException) {
//                null
//            }
//        }
//
//        private suspend fun geocode(addr: String): Location? =
//            withContext(Dispatchers.IO) {
//                try {
//                    val list = Geocoder(ctx).getFromLocationName(addr, 1)
//                    list?.firstOrNull()?.let {
//                        Location("").apply {
//                            latitude = it.latitude
//                            longitude = it.longitude
//                        }
//                    }
//                } catch (_: IOException) {
//                    null
//                }
//            }
//    }
//}
//
//
//
