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
import com.example.skysave.datatypes.User
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth
    private var user: User? = null
    private lateinit var folderRef: StorageReference

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
                R.id.nav_files, R.id.nav_starred, R.id.nav_shared, R.id.nav_profile
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.menu.findItem(R.id.add_placeholder).isEnabled = false

        supportActionBar?.hide()

        onBackPressedDispatcher.addCallback(this,onBackPressedCallback)

        auth = FirebaseAuth.getInstance()

        @Suppress("DEPRECATION")
        user = intent.getSerializableExtra("user") as? User

        if (user != null) {
            Log.w("test", user!!.uid)
            Log.w("test", user!!.email)
            Log.w("test", user!!.alias)
            Log.w("test", user!!.files.toString())
        }

        folderRef = Firebase.storage.reference.child(user!!.uid)
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

    fun getUser(): User?{
        return user
    }

    fun getFolderRef(): StorageReference{
        return folderRef
    }
}