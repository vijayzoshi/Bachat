package com.example.nearstore

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.os.Bundle
import java.util.Locale

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val languageCode = newBase.getSharedPreferences("Settings", MODE_PRIVATE)
            .getString("Language", "en") ?: "en"
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onResume() {
        super.onResume()
        // Reapply language settings when activity resumes
        applyCurrentLanguage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply the saved language
        applyCurrentLanguage()
    }

    private fun applyCurrentLanguage() {
        // Get language from both preference locations for compatibility
        val settingsLang = getSharedPreferences("Settings", MODE_PRIVATE)
            .getString("Language", "en") ?: "en"
        
        val appPrefsLang = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .getString("language", "en") ?: "en"
        
        // Use settingsLang as the primary source, but ensure both are in sync
        val languageCode = settingsLang
        
        // Sync the preferences if they're different
        if (settingsLang != appPrefsLang) {
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                .putString("language", languageCode)
                .apply()
        }
        
        // Apply the language
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    // Add this method to be called from child activities
    protected fun applyToolbarElevation(toolbar: Toolbar) {
        toolbar.elevation = resources.getDimension(R.dimen.toolbar_elevation)
    }

    private fun setAppLanguage(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
} 