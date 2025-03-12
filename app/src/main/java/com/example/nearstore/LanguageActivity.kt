package com.example.nearstore

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.example.nearstore.databinding.ActivityLanguageBinding
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class LanguageActivity : BaseActivity() {
    private lateinit var binding: ActivityLanguageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupLanguageCards()
        updateSelectedLanguageIndicator()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = getString(R.string.language)
            setNavigationOnClickListener { finish() }
        }
    }

    private fun setupLanguageCards() {
        binding.englishCard.setOnClickListener {
            changeLanguage("en")
            Snackbar.make(binding.root, "Language changed to English", Snackbar.LENGTH_SHORT).show()
        }

        binding.hindiCard.setOnClickListener {
            changeLanguage("hi")
            Snackbar.make(binding.root, "भाषा हिंदी में बदल दी गई है", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun updateSelectedLanguageIndicator() {
        val currentLanguage = getSharedPreferences("Settings", MODE_PRIVATE)
            .getString("Language", "en") ?: "en"
        
        binding.englishCheckmark.visibility = if (currentLanguage == "en") View.VISIBLE else View.GONE
        binding.hindiCheckmark.visibility = if (currentLanguage == "hi") View.VISIBLE else View.GONE
    }

    private fun changeLanguage(languageCode: String) {
        // Save language preference
        getSharedPreferences("Settings", MODE_PRIVATE).edit()
            .putString("Language", languageCode)
            .apply()
        
        // Apply language change
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        
        // Update UI
        updateSelectedLanguageIndicator()
        
        // Restart app to apply changes everywhere
        restartApp()
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
} 