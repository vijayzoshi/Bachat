package com.example.nearstore.adapter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.nearstore.R
import com.example.nearstore.data.Customer
import com.example.nearstore.databinding.ItemCustomerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

class CustomerAdapter(
    private val onItemClick: (Customer) -> Unit,
    private val getCustomerBalance: suspend (Int) -> Double
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    private var customers = listOf<Customer>()
    private val balanceCache = mutableMapOf<Int, Double>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    inner class CustomerViewHolder(private val binding: ItemCustomerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: Customer) {
            binding.customerNameTextView.text = customer.name
            
            // Set initial letter in the circle
            val initial = customer.name.firstOrNull()?.uppercase() ?: "?"
            
            // Set a different background color based on the initial
            val colors = arrayOf(
                R.color.primary_container,
                R.color.secondary_container,
                R.color.tertiary_container,
                R.color.credit_container,
                R.color.debit_container
            )
            val colorIndex = (initial.hashCode() % colors.size).absoluteValue
            binding.customerInitialIcon.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, colors[colorIndex])
            )
            
            // Set the text directly on the TextView
            binding.customerInitialText.text = initial
            
            // Load and display balance
            val customerId = customer.id
            if (balanceCache.containsKey(customerId)) {
                updateBalanceText(balanceCache[customerId] ?: 0.0)
            } else {
                binding.customerBalanceTextView.text = "..."
                coroutineScope.launch {
                    val balance = getCustomerBalance(customerId)
                    balanceCache[customerId] = balance
                    withContext(Dispatchers.Main) {
                        updateBalanceText(balance)
                    }
                }
            }
            
            binding.root.setOnClickListener { onItemClick(customer) }
        }
        
        private fun updateBalanceText(balance: Double) {
            // Use absolute value to remove negative sign
            val formattedBalance = String.format("₹%.2f", balance.absoluteValue)
            binding.customerBalanceTextView.text = formattedBalance
            
            // Set color based on balance
            val textColor = when {
                balance > 0 -> R.color.credit_green
                balance < 0 -> R.color.debit_red
                else -> android.R.color.darker_gray
            }
            binding.customerBalanceTextView.setTextColor(
                ContextCompat.getColor(binding.root.context, textColor)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(customers[position])
    }

    override fun getItemCount() = customers.size

    fun updateCustomers(newCustomers: List<Customer>) {
        customers = newCustomers
        notifyDataSetChanged()
    }

    fun clearBalanceCache() {
        balanceCache.clear()
        notifyDataSetChanged()
    }
}