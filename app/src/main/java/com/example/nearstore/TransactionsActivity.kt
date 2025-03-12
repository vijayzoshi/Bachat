package com.example.nearstore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nearstore.adapter.TransactionAdapter
import com.example.nearstore.data.AppDatabase
import com.example.nearstore.data.Customer
import com.example.nearstore.data.Transaction
import com.example.nearstore.data.TransactionType
import com.example.nearstore.databinding.ActivityTransactionsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.ContextCompat
import kotlin.math.absoluteValue
import android.app.DatePickerDialog
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

class TransactionsActivity : BaseActivity() {
    private lateinit var binding: ActivityTransactionsBinding
    private lateinit var adapter: TransactionAdapter
    private lateinit var db: AppDatabase
    private var customerId: Int = 0
    private var customerName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customerId = intent.getIntExtra("CUSTOMER_ID", 0)
        customerName = intent.getStringExtra("CUSTOMER_NAME") ?: ""

        db = AppDatabase.getDatabase(this)
        setupToolbar(customerName)
        setupRecyclerView()
        setupClickListeners()
        loadTransactions()
    }

    private fun setupToolbar(customerName: String) {
        binding.toolbar.title = customerName
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        binding.toolbar.elevation = resources.getDimension(R.dimen.toolbar_elevation)

        binding.toolbar.inflateMenu(R.menu.menu_transactions)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_whatsapp -> {
                    handleWhatsAppAction()
                    true
                }
                R.id.action_call -> {
                    handleCallAction()
                    true
                }
                R.id.action_edit_phone -> {
                    showEditPhoneDialog()
                    true
                }
                R.id.action_delete_all_transactions -> {
                    showDeleteAllTransactionsConfirmation()
                    true
                }
                R.id.action_delete_customer -> {
                    showDeleteCustomerConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onEditClick = { showEditTransactionDialog(it) },
            onDeleteClick = { showDeleteConfirmationDialog(it) }
        )
        binding.transactionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@TransactionsActivity)
            adapter = this@TransactionsActivity.adapter
        }
    }

    private fun setupClickListeners() {
        binding.addTransactionFab.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    private fun showAddTransactionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)

        val amountInputLayout = dialogView.findViewById<TextInputLayout>(R.id.amountInputLayout)
        val descriptionInputLayout = dialogView.findViewById<TextInputLayout>(R.id.descriptionInputLayout)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.typeRadioGroup)
        val dateInputLayout = dialogView.findViewById<TextInputLayout>(R.id.dateInputLayout)
        val dateEditText = dateInputLayout.editText!!

        val amountEditText = amountInputLayout.editText!!
        val descriptionEditText = descriptionInputLayout.editText!!

        // Set current date as default
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        dateEditText.setText(dateFormat.format(calendar.time))

        // Show date picker when clicking on the date field
        dateEditText.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(selectedYear, selectedMonth, selectedDay)
                    dateEditText.setText(dateFormat.format(calendar.time))
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_transaction))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val amountStr = amountEditText.text.toString()
                val description = descriptionEditText.text.toString()
                val type = if (radioGroup.checkedRadioButtonId == R.id.creditRadioButton) 
                    TransactionType.CREDIT else TransactionType.DEBIT

                if (amountStr.isNotEmpty()) {
                    val amount = amountStr.toDouble()
                    val finalAmount = if (type == TransactionType.DEBIT) -amount else amount
                    
                    // Create transaction with the selected date
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val timestamp = calendar.timeInMillis
                    
                    val transaction = Transaction(
                        customerId = customerId,
                        amount = finalAmount,  // Already negative for debit
                        description = description,
                        type = type,
                        timestamp = timestamp
                    )
                    
                    addTransaction(transaction)
                } else {
                    Snackbar.make(binding.root, getString(R.string.please_enter_amount), Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)

        val amountInputLayout = dialogView.findViewById<TextInputLayout>(R.id.amountInputLayout)
        val descriptionInputLayout = dialogView.findViewById<TextInputLayout>(R.id.descriptionInputLayout)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.typeRadioGroup)
        val dateInputLayout = dialogView.findViewById<TextInputLayout>(R.id.dateInputLayout)
        val dateEditText = dateInputLayout.editText!!

        val amountEditText = amountInputLayout.editText!!
        val descriptionEditText = descriptionInputLayout.editText!!

        // Pre-fill the existing transaction data
        amountEditText.setText(transaction.amount.absoluteValue.toString())
        descriptionEditText.setText(transaction.description)
        radioGroup.check(if (transaction.type == TransactionType.CREDIT) R.id.creditRadioButton else R.id.debitRadioButton)

        // Set the existing date
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = transaction.timestamp
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        dateEditText.setText(dateFormat.format(calendar.time))

        // Show date picker when clicking on the date field
        dateEditText.setOnClickListener {
            DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(selectedYear, selectedMonth, selectedDay)
                    dateEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.edit_transaction))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val amountStr = amountEditText.text.toString()
                val description = descriptionEditText.text.toString()
                val type = if (radioGroup.checkedRadioButtonId == R.id.creditRadioButton) 
                    TransactionType.CREDIT else TransactionType.DEBIT

                if (amountStr.isNotEmpty()) {
                    val amount = amountStr.toDouble()
                    val finalAmount = if (type == TransactionType.DEBIT) -amount else amount
                    
                    // Update transaction with the selected date
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    
                    val updatedTransaction = transaction.copy(
                        amount = finalAmount,  // Already negative for debit
                        description = description,
                        type = type,
                        timestamp = calendar.timeInMillis
                    )
                    
                    updateTransaction(updatedTransaction)
                } else {
                    Snackbar.make(binding.root, getString(R.string.please_enter_amount), Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_transaction))
            .setMessage(getString(R.string.delete_transaction_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteTransaction(transaction)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateTransaction(transaction: Transaction) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.transactionDao().updateTransaction(transaction)
            
            // Refresh transactions list
            val transactions = db.transactionDao().getTransactionsForCustomer(customerId)
            val balance = db.transactionDao().getBalanceForCustomer(customerId) ?: 0.0
            
            withContext(Dispatchers.Main) {
                adapter.updateTransactions(transactions)
                updateBalance(balance)
                Snackbar.make(binding.root, getString(R.string.transaction_updated), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun addTransaction(transaction: Transaction) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.transactionDao().insertTransaction(transaction)
            
            // Refresh transactions list
            val transactions = db.transactionDao().getTransactionsForCustomer(customerId)
            val balance = db.transactionDao().getBalanceForCustomer(customerId) ?: 0.0
            
            withContext(Dispatchers.Main) {
                adapter.updateTransactions(transactions)
                updateBalance(balance)
                Snackbar.make(binding.root, getString(R.string.transaction_added), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteTransaction(transaction: Transaction) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.transactionDao().deleteTransaction(transaction)
            
            // Refresh transactions list
            val transactions = db.transactionDao().getTransactionsForCustomer(customerId)
            val balance = db.transactionDao().getBalanceForCustomer(customerId) ?: 0.0
            
            withContext(Dispatchers.Main) {
                adapter.updateTransactions(transactions)
                updateBalance(balance)
                Snackbar.make(binding.root, getString(R.string.transaction_deleted), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTransactions() {
        lifecycleScope.launch(Dispatchers.IO) {
            val transactions = db.transactionDao().getTransactionsForCustomer(customerId)
            val balance = db.transactionDao().getBalanceForCustomer(customerId) ?: 0.0
            
            withContext(Dispatchers.Main) {
                adapter.updateTransactions(transactions)
                updateBalance(balance)
            }
        }
    }

    private fun updateBalance(balance: Double) {
        // Update balance amount without negative sign
        binding.balanceTextView.text = String.format("₹%.2f", kotlin.math.abs(balance))
        
        // Set balance text color
        val colorRes = when {
            balance > 0 -> R.color.credit_green
            balance < 0 -> R.color.debit_red
            else -> android.R.color.darker_gray
        }
        binding.balanceTextView.setTextColor(ContextCompat.getColor(this, colorRes))

        // Update payment status chip
        binding.paymentStatusChip.apply {
            when {
                balance < 0 -> {
                    text = getString(R.string.they_will_pay)
                    setChipBackgroundColorResource(R.color.debit_red)
                    visibility = View.VISIBLE
                }
                balance > 0 -> {
                    text = getString(R.string.you_will_pay)
                    setChipBackgroundColorResource(R.color.credit_green)
                    visibility = View.VISIBLE
                }
                else -> {
                    visibility = View.GONE
                }
            }
        }
    }

    private fun handleWhatsAppAction() {
        lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getCustomerById(customerId)
            withContext(Dispatchers.Main) {
                customer?.let {
                    if (it.phone.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://wa.me/91${it.phone}")
                        }
                        startActivity(intent)
                    } else {
                        Snackbar.make(binding.root, getString(R.string.no_phone_number), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun handleCallAction() {
        lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getCustomerById(customerId)
            withContext(Dispatchers.Main) {
                customer?.let {
                    if (it.phone.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${it.phone}")
                        }
                        startActivity(intent)
                    } else {
                        Snackbar.make(binding.root, getString(R.string.no_phone_number), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showEditPhoneDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getCustomerById(customerId)
            withContext(Dispatchers.Main) {
                customer?.let { showEditPhoneDialogUI(it) }
            }
        }
    }

    private fun showEditPhoneDialogUI(customer: Customer) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_phone, null)
        val phoneInputLayout = dialogView.findViewById<TextInputLayout>(R.id.phoneInputLayout)
        val phoneEditText = phoneInputLayout.editText!!
        
        // Pre-fill current phone number
        phoneEditText.setText(customer.phone)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.edit_phone))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newPhone = phoneEditText.text.toString().trim()
                updateCustomerPhone(customer, newPhone)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateCustomerPhone(customer: Customer, newPhone: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val updatedCustomer = customer.copy(phone = newPhone)
            db.customerDao().updateCustomer(updatedCustomer)
            withContext(Dispatchers.Main) {
                Snackbar.make(binding.root, getString(R.string.phone_updated), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteAllTransactionsConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_all_transactions))
            .setMessage(getString(R.string.delete_all_transactions_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteAllTransactions()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteAllTransactions() {
        lifecycleScope.launch(Dispatchers.IO) {
            db.transactionDao().deleteAllTransactionsForCustomer(customerId)
            
            withContext(Dispatchers.Main) {
                loadTransactions()
                Snackbar.make(binding.root, getString(R.string.all_transactions_deleted), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteCustomerConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_customer))
            .setMessage(getString(R.string.delete_customer_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteCustomer()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteCustomer() {
        lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getCustomerById(customerId)
            customer?.let {
                db.customerDao().deleteCustomer(it)
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }
}