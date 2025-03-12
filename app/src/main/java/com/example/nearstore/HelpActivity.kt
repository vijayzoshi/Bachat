package com.example.nearstore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.GridLayout
import com.example.nearstore.databinding.ActivityHelpBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar

class HelpActivity : BaseActivity() {
    private lateinit var binding: ActivityHelpBinding
    private val supportPhone = "+919876543210" // Replace with your support phone number

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = getString(R.string.help)
            setNavigationOnClickListener { finish() }
        }
    }

    private fun setupClickListeners() {
        binding.callCard.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:8377055197")
            }
            startActivity(intent)
        }

        binding.messageCard.setOnClickListener {
           /* val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:8377055197")
                putExtra("sms_body", "Hello, I need help with the Bachat app.")
            }
            startActivity(intent)

            */
            openWhatsApp("8377055197")
        }
    }

    private fun openWhatsApp(phoneNumber: String) {
        try {
            val formattedNumber = if (phoneNumber.startsWith("+")) {
                phoneNumber.substring(1)
            } else if (phoneNumber.startsWith("0")) {
                "91" + phoneNumber.substring(1)
            } else {
                "91$phoneNumber"
            }

            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: Exception) {
            Snackbar.make(binding.root, "WhatsApp not installed", Snackbar.LENGTH_SHORT).show()
        }
    }
} 