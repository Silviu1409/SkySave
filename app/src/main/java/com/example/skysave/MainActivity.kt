package com.example.skysave

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.skysave.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            exitApp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navMenu

        val navController = findNavController(R.id.activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_files, R.id.nav_starred, R.id.nav_profile
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        supportActionBar?.hide()

        onBackPressedDispatcher.addCallback(this,onBackPressedCallback)

        auth = FirebaseAuth.getInstance()

        val user2 = FirebaseAuth.getInstance().currentUser

        if (user2 != null) {
            user2.email?.let { Log.w("test", it) }
            user2.displayName?.let { Log.w("test", it) }
            user2.photoUrl?.let { Log.w("test", it.toString()) }
        }
    }

    private fun exitApp() {
        MaterialAlertDialogBuilder(this)
            .setMessage("Do you want to close the app?")
            .setPositiveButton("Yes") { _, _ -> finishAffinity() }
            .setNegativeButton("No", null)
            .show()
    }

    fun getAuth(): FirebaseAuth{
        return auth
    }
}