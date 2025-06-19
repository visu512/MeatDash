////package com.meat.meatdash.adapter
////
////import android.content.Context
////import android.os.CountDownTimer
////import android.view.LayoutInflater
////import android.view.View
////import android.view.ViewGroup
////import android.widget.LinearLayout
////import android.widget.TextView
////import androidx.core.content.ContextCompat
////import androidx.core.text.HtmlCompat
////import androidx.core.view.isVisible
////import androidx.recyclerview.widget.LinearLayoutManager
////import androidx.recyclerview.widget.RecyclerView
////import com.meat.meatdash.R
////import com.meat.meatdash.model.Order
////import com.meat.meatdash.orders.OrderActivity
////
////class OrderAdapter(
////    private val orders: MutableList<Order>,
////    private val onDelivered: (Order) -> Unit,
////    orderActivity: OrderActivity
////) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {
////
////    companion object {
////        private const val SHARED_PREFS_NAME = "MeatDashPrefs"
////        private const val KEY_DURATION = "LAST_DURATION"
////    }
////
////    private val expandedPositions = mutableSetOf<Int>()
////
////    fun clearExpanded() {
////        expandedPositions.clear()
////        notifyDataSetChanged()
////    }
////
////    fun expandAll() {
////        expandedPositions.clear()
////        orders.indices.forEach { expandedPositions.add(it) }
////        notifyDataSetChanged()
////    }
////
////    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
////        val view = LayoutInflater.from(parent.context)
////            .inflate(R.layout.item_order, parent, false)
////        return OrderViewHolder(view)
////    }
////
////    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
////        // Ensure default expansion: if none explicitly set, expand all
////        if (expandedPositions.isEmpty() && orders.isNotEmpty()) {
////            orders.indices.forEach { expandedPositions.add(it) }
////        }
////        holder.bind(orders[position], position)
////    }
////
////    override fun onViewRecycled(holder: OrderViewHolder) {
////        super.onViewRecycled(holder)
////        holder.clearTimer()
////    }
////
////    override fun getItemCount(): Int = orders.size
////
////    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
////        private val textOrderId = itemView.findViewById<TextView>(R.id.textOrderId)
////        private val textShopName = itemView.findViewById<TextView>(R.id.textShopName)
////        private val textOrderDate = itemView.findViewById<TextView>(R.id.textPalceingOrderDate)
////        private val textOrderTotal = itemView.findViewById<TextView>(R.id.textOrderTotal)
////        private val textPaymentStatus = itemView.findViewById<TextView>(R.id.textPaymentStatus)
////        private val textToggleDetails = itemView.findViewById<TextView>(R.id.textToggleDetails)
////        private val containerDetails = itemView.findViewById<LinearLayout>(R.id.containerDetails)
////        private val recyclerItems = itemView.findViewById<RecyclerView>(R.id.recyclerViewOrderItems)
////        private val textDeliveryTimer = itemView.findViewById<TextView>(R.id.textDeliveryTimer)
////
////        private var countDownTimer: CountDownTimer? = null
////        private lateinit var currentOrder: Order
////
////        fun bind(order: Order, position: Int) {
////            currentOrder = order
////
////            // Basic info
////            textOrderId.text = "Order ID: ${order.orderId}"
////            textShopName.text = order.shopName
////            textOrderDate.text = if (order.timestamp > 0L) {
////                java.text.SimpleDateFormat("MMM d, hh:mm a", java.util.Locale.getDefault())
////                    .format(java.util.Date(order.timestamp))
////            } else "N/A"
////            textOrderTotal.text = "Total: ₹%.2f".format(order.total)
////            textPaymentStatus.text = order.status
////
////            recyclerItems.apply {
////                layoutManager = LinearLayoutManager(itemView.context)
////                adapter = OrderItemAdapter(order.items)
////                isNestedScrollingEnabled = false
////            }
////
////            // Delivered state
////            if (order.status.equals("Delivered", true)) {
////                showDelivered()
////            }
////
////            // Expand/collapse state
////            val isExpanded = expandedPositions.contains(position)
////            containerDetails.isVisible = isExpanded
////            textToggleDetails.text = if (isExpanded) "Less…" else "More…"
////            textDeliveryTimer.visibility = if (isExpanded) View.VISIBLE else View.GONE
////
////            if (isExpanded && !order.status.equals("Delivered", true)) {
////                startTimerFromPrefs()
////            }
////
////            textToggleDetails.setOnClickListener {
////                val expand = !containerDetails.isVisible
////                containerDetails.isVisible = expand
////
////                if (expand) {
////                    expandedPositions.add(position)
////                    textToggleDetails.text = "Less…"
////                    textDeliveryTimer.visibility = View.VISIBLE
////                    if (!order.status.equals("Delivered", true)) {
////                        startTimerFromPrefs()
////                    } else {
////                        showDelivered()
////                    }
////                } else {
////                    expandedPositions.remove(position)
////                    clearTimer()
////                    textDeliveryTimer.visibility = View.GONE
////                    textToggleDetails.text = "More…"
////                }
////            }
////        }
////
////        private fun startTimerFromPrefs() {
////            clearTimer()
////            val prefs = itemView.context
////                .getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
////            val durStr = prefs.getString(KEY_DURATION, null) ?: run {
////                textDeliveryTimer.text = "No ETA"
////                return
////            }
////
////            // Expecting format "Time: X.X min"
////            val minutes = durStr
////                .substringAfter("Time: ")
////                .substringBefore(" min")
////                .toDoubleOrNull() ?: 0.0
////
////            val msLeft = (minutes * 60 * 1_000).toLong().coerceAtLeast(0L)
////            if (msLeft == 0L) {
////                deliverNow()
////                return
////            }
////
////            countDownTimer = object : CountDownTimer(msLeft, 1_000) {
////                override fun onTick(ms: Long) {
////                    val minsLeft = (ms / 1_000) / 60
////                    val padded = minsLeft.toString().padStart(2, '0')
////                    val html = "Deliver in: " +
////                            "<font color=\"#FF5722\"><b>$padded mins</b></font>"
////                    textDeliveryTimer.text = HtmlCompat.fromHtml(
////                        html, HtmlCompat.FROM_HTML_MODE_LEGACY
////                    )
////                    textDeliveryTimer.setTextColor(
////                        ContextCompat.getColor(itemView.context, R.color.orderActive)
////                    )
////                }
////
////                override fun onFinish() = deliverNow()
////            }.start()
////        }
////
////        private fun showDelivered() {
////            clearTimer()
////            textDeliveryTimer.text = "Delivered"
////            textDeliveryTimer.setTextColor(
////                ContextCompat.getColor(itemView.context, R.color.green)
////            )
////        }
////
////        private fun deliverNow() {
////            showDelivered()
////            currentOrder.status = "Delivered"
////            onDelivered(currentOrder)
////        }
////
////        fun clearTimer() {
////            countDownTimer?.cancel()
////            countDownTimer = null
////        }
////    }
////}
////
//package com.meat.meatdash.adapter
//
//import android.content.Context
//import android.os.CountDownTimer
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.LinearLayout
//import android.widget.TextView
//import androidx.core.text.HtmlCompat
//import androidx.core.view.isVisible
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.meat.meatdash.R
//import com.meat.meatdash.model.Order
//import com.meat.meatdash.orders.OrderActivity
//
//class OrderAdapter(
//    private val orders: MutableList<Order>,
//    private val onDelivered: (Order) -> Unit,
//    orderActivity: OrderActivity
//) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {
//
//    companion object {
//        private const val SHARED_PREFS_NAME = "MeatDashPrefs"
//        private const val KEY_DURATION = "LAST_DURATION"
//    }
//
//    private val expandedPositions = mutableSetOf<Int>()
//
//    fun clearExpanded() {
//        expandedPositions.clear()
//        notifyDataSetChanged()
//    }
//
//    fun expandAll() {
//        expandedPositions.clear()
//        orders.indices.forEach { expandedPositions.add(it) }
//        notifyDataSetChanged()
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_order, parent, false)
//        return OrderViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
//        if (expandedPositions.isEmpty() && orders.isNotEmpty()) {
//            orders.indices.forEach { expandedPositions.add(it) }
//        }
//        holder.bind(orders[position], position)
//    }
//
//    override fun onViewRecycled(holder: OrderViewHolder) {
//        super.onViewRecycled(holder)
//        holder.clearTimer()
//    }
//
//    override fun getItemCount(): Int = orders.size
//
//    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val tvOrderId       = itemView.findViewById<TextView>(R.id.textOrderId)
//        private val tvShopName      = itemView.findViewById<TextView>(R.id.textShopName)
//        private val tvOrderDate     = itemView.findViewById<TextView>(R.id.textPalceingOrderDate)
//        private val tvOrderTotal    = itemView.findViewById<TextView>(R.id.textOrderTotal)
//        private val tvPaymentStatus = itemView.findViewById<TextView>(R.id.textPaymentStatus)
//        private val tvToggleDetails = itemView.findViewById<TextView>(R.id.textToggleDetails)
//        private val containerDetails= itemView.findViewById<LinearLayout>(R.id.containerDetails)
//        private val rvItems         = itemView.findViewById<RecyclerView>(R.id.recyclerViewOrderItems)
//        private val tvDeliveryTimer = itemView.findViewById<TextView>(R.id.textDeliveryTimer)
//
//        private var countDownTimer: CountDownTimer? = null
//        private lateinit var currentOrder: Order
//
//        fun bind(order: Order, position: Int) {
//            currentOrder = order
//
//            // Basic info
//            tvOrderId.text       = "Order ID: ${order.orderId}"
//            tvShopName.text      = order.shopName
//            tvOrderDate.text     = order.timestamp
//                .takeIf { it > 0L }
//                ?.let {
//                    java.text.SimpleDateFormat(
//                        "MMM d, hh:mm a",
//                        java.util.Locale.getDefault()
//                    ).format(java.util.Date(it))
//                } ?: "N/A"
//            tvOrderTotal.text    = "Total: ₹%.2f".format(order.total)
//            tvPaymentStatus.text = order.status
//
//            // Items list
//            rvItems.apply {
//                layoutManager             = LinearLayoutManager(itemView.context)
//                adapter                   = OrderItemAdapter(order.items)
//                isNestedScrollingEnabled  = false
//            }
//
//            val isDelivered = order.status.equals("Delivered", true)
//            val isExpanded  = expandedPositions.contains(position)
//
//            containerDetails.isVisible = isExpanded
//            tvToggleDetails.text       = if (isExpanded) "Less…" else "More…"
//            tvDeliveryTimer.isVisible  = isExpanded
//
//            if (isExpanded) {
//                if (isDelivered) showDelivered()
//                else               startTimerForOrder()
//            }
//
//            tvToggleDetails.setOnClickListener {
//                val expand = !containerDetails.isVisible
//                containerDetails.isVisible = expand
//
//                if (expand) {
//                    expandedPositions.add(position)
//                    tvToggleDetails.text      = "Less…"
//                    tvDeliveryTimer.isVisible = true
//                    if (isDelivered) showDelivered() else startTimerForOrder()
//                } else {
//                    expandedPositions.remove(position)
//                    clearTimer()
//                    tvDeliveryTimer.isVisible = false
//                    tvToggleDetails.text      = "More…"
//                }
//            }
//        }
//
//        private fun startTimerForOrder() {
//            clearTimer()
//            val prefs    = itemView.context.getSharedPreferences(
//                SHARED_PREFS_NAME,
//                Context.MODE_PRIVATE
//            )
//            val key      = KEY_DURATION + currentOrder.orderId
//            val finishTs = prefs.getLong(key, -1L)
//
//            if (finishTs < 0L) {
//                tvDeliveryTimer.text = "No ETA"
//                return
//            }
//
//            val msLeft = finishTs - System.currentTimeMillis()
//            if (msLeft <= 0L) {
//                deliverNow()
//                return
//            }
//
//            countDownTimer = object : CountDownTimer(msLeft, 1_000) {
//                override fun onTick(ms: Long) {
//                    val mins   = (ms / 1_000) / 60
//                    val padded = mins.toString().padStart(2, '0')
//                    val html   = "Deliver in: <font color='#FF5722'><b>$padded mins</b></font>"
//                    tvDeliveryTimer.text = HtmlCompat.fromHtml(
//                        html,
//                        HtmlCompat.FROM_HTML_MODE_LEGACY
//                    )
//                }
//                override fun onFinish() = deliverNow()
//            }.start()
//        }
//
//        private fun showDelivered() {
//            clearTimer()
//            tvDeliveryTimer.text = "Delivered"
//        }
//
//        private fun deliverNow() {
//            showDelivered()
//            currentOrder.status = "Delivered"
//            onDelivered(currentOrder)
//        }
//
//        fun clearTimer() {
//            countDownTimer?.cancel()
//            countDownTimer = null
//        }
//    }
//}


package com.meat.meatdash.adapter

import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.meat.meatdash.R
import com.meat.meatdash.model.Order
import com.meat.meatdash.orders.OrderActivity

class OrderAdapter(
    private val orders: MutableList<Order>,
    private val onDelivered: (Order) -> Unit,
    orderActivity: OrderActivity
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    companion object {
        private const val SHARED_PREFS_NAME = "MeatDashPrefs"
        private const val KEY_FINISH_PREFIX = "FINISH_TIME_"
    }

    private val expandedPositions = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        OrderViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_order, parent, false)
        )

    override fun getItemCount() = orders.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        if (expandedPositions.isEmpty() && orders.isNotEmpty()) {
            orders.indices.forEach { expandedPositions.add(it) }
        }
        holder.bind(orders[position], position)
    }

    override fun onViewRecycled(holder: OrderViewHolder) {
        super.onViewRecycled(holder)
        holder.clearTimer()
    }

    fun clearExpanded() {
        expandedPositions.clear()
        notifyDataSetChanged()
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderId = itemView.findViewById<TextView>(R.id.textOrderId)
        private val tvShopName = itemView.findViewById<TextView>(R.id.textShopName)
        private val tvOrderDate = itemView.findViewById<TextView>(R.id.textPalceingOrderDate)
        private val tvOrderTotal = itemView.findViewById<TextView>(R.id.textOrderTotal)
        private val tvPaymentStatus = itemView.findViewById<TextView>(R.id.textPaymentStatus)
        private val tvToggleDetails = itemView.findViewById<TextView>(R.id.textToggleDetails)
        private val containerDetails = itemView.findViewById<LinearLayout>(R.id.containerDetails)
        private val rvItems = itemView.findViewById<RecyclerView>(R.id.recyclerViewOrderItems)
        private val tvDeliveryTimer = itemView.findViewById<TextView>(R.id.textDeliveryTimer)

        private var countDownTimer: CountDownTimer? = null
        private lateinit var currentOrder: Order

        fun bind(order: Order, position: Int) {
            currentOrder = order
            // set up basic fields...
            tvOrderId.text = "Order ID: ${order.orderId}"
            tvShopName.text = order.shopName
            tvOrderDate.text = order.timestamp
                .takeIf { it > 0L }
                ?.let {
                    java.text.SimpleDateFormat(
                        "MMM d, hh:mm a",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date(it))
                } ?: "N/A"
            tvOrderTotal.text = "Total: ₹%.2f".format(order.total)
            tvPaymentStatus.text = order.status

            rvItems.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = OrderItemAdapter(order.items)
                isNestedScrollingEnabled = false
            }

            val isDelivered = order.status.equals("Delivered", true)
            val isExpanded = expandedPositions.contains(position)

            containerDetails.isVisible = isExpanded
            tvToggleDetails.text = if (isExpanded) "Less…" else "More…"
            tvDeliveryTimer.isVisible = isExpanded

            if (isExpanded) {
                if (isDelivered) showDelivered()
                else startTimerForOrder()
            }

            tvToggleDetails.setOnClickListener {
                val expand = !containerDetails.isVisible
                containerDetails.isVisible = expand
                if (expand) {
                    expandedPositions.add(position)
                    tvToggleDetails.text = "Less…"
                    tvDeliveryTimer.isVisible = true
                    if (isDelivered) showDelivered() else startTimerForOrder()
                } else {
                    expandedPositions.remove(position)
                    clearTimer()
                    tvDeliveryTimer.isVisible = false
                    tvToggleDetails.text = "More…"
                }
            }
        }

        private fun startTimerForOrder() {
            clearTimer()
            val prefs = itemView.context.getSharedPreferences(
                SHARED_PREFS_NAME, Context.MODE_PRIVATE
            )
            val finishTs = prefs.getLong(
                KEY_FINISH_PREFIX + currentOrder.orderId,
                -1L
            )
            if (finishTs < 0L) {
                tvDeliveryTimer.text = "No ETA"
                return
            }
            val msLeft = finishTs - System.currentTimeMillis()
            if (msLeft <= 0L) {
                deliverNow()
                return
            }
            countDownTimer = object : CountDownTimer(msLeft, 1_000) {
                override fun onTick(ms: Long) {
                    val mins = (ms / 1_000) / 60
                    val padded = mins.toString().padStart(2, '0')
                    val html = "Deliver in: <font color='#FF5722'><b>$padded mins</b></font>"
                    tvDeliveryTimer.text = HtmlCompat.fromHtml(
                        html, HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                }

                override fun onFinish() = deliverNow()
            }.start()
        }

        private fun showDelivered() {
            clearTimer()
            tvDeliveryTimer.text = "Delivered"
        }

        private fun deliverNow() {
            showDelivered()
            currentOrder.status = "Delivered"
            onDelivered(currentOrder)
        }

        fun clearTimer() {
            countDownTimer?.cancel()
            countDownTimer = null
        }
    }
}
