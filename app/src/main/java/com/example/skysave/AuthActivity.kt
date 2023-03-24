package com.example.skysave

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.skysave.databinding.ActivityAuthBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    val tag = "test"

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            exitApp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this,onBackPressedCallback)
    }

    private fun exitApp() {
        MaterialAlertDialogBuilder(this)
            .setMessage("Do you want to close the app?")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }

    fun hideKeyboard() {
        if(currentFocus != null) {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }
}