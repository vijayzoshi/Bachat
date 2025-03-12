package com.example.nearstore

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nearstore.data.AppDatabase
import com.example.nearstore.databinding.ActivitySettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "Settings"
            setNavigationOnClickListener { finish() }
        }
    }

    private fun setupClickListeners() {
        binding.languageCard.setOnClickListener {
            startActivity(Intent(this, LanguageActivity::class.java))
        }

        binding.helpCard.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
        
        binding.deleteAllDataCard.setOnClickListener {
            showDeleteAllDataConfirmation()
        }
    }
    
    private fun showDeleteAllDataConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_all_data)
            .setMessage(getString(R.string.are_you_sure_you_want_to_delete_all_customers_and_transactions_this_action_cannot_be_undone))
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteAllData()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun deleteAllData() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Delete all data from the database
            db.customerDao().deleteAllCustomers()
            
            withContext(Dispatchers.Main) {
                Snackbar.make(
                    binding.root,
                    "All data has been deleted",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
}