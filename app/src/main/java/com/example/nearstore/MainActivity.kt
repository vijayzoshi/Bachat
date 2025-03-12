package com.example.nearstore

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nearstore.adapter.CustomerAdapter
import com.example.nearstore.data.AppDatabase
import com.example.nearstore.data.Customer
import com.example.nearstore.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.SearchView
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.snackbar.Snackbar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.nearstore.data.TransactionType
import kotlin.math.absoluteValue

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CustomerAdapter
    private lateinit var db: AppDatabase
    private var allCustomers: List<Customer> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        setupToolbar()
        setupRecyclerView()
        setupSearchView()
        setupClickListeners()
        loadCustomers()
    }

    override fun onResume() {
        super.onResume()
        loadCustomers()
        adapter.clearBalanceCache()
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = CustomerAdapter(
            onItemClick = { customer ->
                val intent = Intent(this, TransactionsActivity::class.java).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                    putExtra("CUSTOMER_NAME", customer.name)
                }
                startActivity(intent)
            },
            getCustomerBalance = { customerId ->
                db.transactionDao().getBalanceForCustomer(customerId) ?: 0.0
            }
        )
        
        binding.customersRecyclerView.adapter = adapter
        binding.customersRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterCustomers(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterCustomers(newText)
                return true
            }
        })
    }

    private fun filterCustomers(query: String?) {
        if (query.isNullOrBlank()) {
            adapter.updateCustomers(allCustomers)
        } else {
            val filteredList = allCustomers.filter { customer ->
                customer.name.contains(query, ignoreCase = true) ||
                customer.phone.contains(query, ignoreCase = true)
            }
            adapter.updateCustomers(filteredList)
        }
    }

    private fun setupClickListeners() {
        binding.addCustomerFab.setOnClickListener {
            showAddCustomerDialog()
        }
    }

    private fun showAddCustomerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_customer, null)
        
        val nameInputLayout = dialogView.findViewById<TextInputLayout>(R.id.nameInputLayout)
        val phoneInputLayout = dialogView.findViewById<TextInputLayout>(R.id.phoneInputLayout)
        
        val nameEditText = nameInputLayout.editText!!
        val phoneEditText = phoneInputLayout.editText!!
        
        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val name = nameEditText.text.toString().trim()
                val phone = phoneEditText.text.toString().trim()
                
                if (name.isNotBlank()) {
                    addCustomer(name, phone)
                } else {
                    Snackbar.make(binding.root, "Please enter a customer name", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun addCustomer(name: String, phone: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.customerDao().insertCustomer(
                Customer(name = name, phone = phone)
            )
            loadCustomers()
        }
    }

    private fun updateNetAmount() {
        lifecycleScope.launch(Dispatchers.IO) {
            val netAmount = db.customerDao().getTotalBalance() ?: 0.0
            
            withContext(Dispatchers.Main) {
                // Format the amount with proper color and sign
                val formattedAmount = String.format("₹%.2f", kotlin.math.abs(netAmount))
                binding.netAmountTextView.text = formattedAmount
                
                // Set color based on net amount
                val textColor = when {
                    netAmount > 0 -> R.color.credit_green
                    netAmount < 0 -> R.color.debit_red
                    else -> android.R.color.darker_gray
                }
                binding.netAmountTextView.setTextColor(ContextCompat.getColor(this@MainActivity, textColor))
            }
        }
    }

    private fun loadCustomers() {
        lifecycleScope.launch(Dispatchers.IO) {
            val customers = db.customerDao().getAllCustomers()
            withContext(Dispatchers.Main) {
                allCustomers = customers
                adapter.updateCustomers(customers)
                updateNetAmount()
            }
        }
    }
}