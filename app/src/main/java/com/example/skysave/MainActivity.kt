package com.example.skysave

import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.skysave.databinding.ActivityMainBinding
import com.example.skysave.datatypes.User
import com.example.skysave.main.Files
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth
    private var user: User? = null
    private lateinit var folderRef: StorageReference

    private lateinit var selectedFileUri: Uri
    private lateinit var fileName: String

    private val getDocumentContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedFileUri = uri

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    fileName = cursor.getString(nameIndex)
                }

                uploadFile()
            }
        }

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

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_files, R.id.nav_trash, R.id.nav_profile
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

        folderRef = Firebase.storage.reference.child(user!!.uid)

        binding.addFab.setOnClickListener {
            getDocumentContent.launch("*/*")
        }
    }

    private fun uploadFile() {
        notificationChannel("upload_notification_channel", NotificationManager.IMPORTANCE_LOW)

        val fileRef = folderRef.child("files/$fileName")
        val uploadTask = fileRef.putFile(selectedFileUri)

        val notificationId = 0

        val notificationBuilder = NotificationCompat.Builder(this, "upload_notification_channel")
            .setContentTitle("Uploading $fileName")
            .setSmallIcon(R.drawable.icon_notification)
            .setProgress(100, 0, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notificationBuilder.build())

        uploadTask
            .addOnProgressListener { snapshot ->
                val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
                notificationBuilder.setProgress(100, progress, false).setContentText("$progress%")
                notificationManager.notify(notificationId, notificationBuilder.build())
            }
            .addOnSuccessListener {snapshot ->
                notificationBuilder.setContentText("Upload complete")
                    .setProgress(0, 0, false)
                notificationManager.notify(notificationId, notificationBuilder.build())

                notificationChannel("upload_notification_channel_2", NotificationManager.IMPORTANCE_HIGH)
                val completedNotificationBuilder = NotificationCompat.Builder(this, "upload_notification_channel_2")
                    .setSmallIcon(R.drawable.icon_notification)
                    .setContentTitle("File Upload Complete")
                    .setContentText("Your file has been uploaded successfully.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                notificationManager.notify(notificationId, completedNotificationBuilder.build())

                val fragment = supportFragmentManager.fragments.first()
                val wantedFragment = fragment.childFragmentManager.fragments.first()

                if (wantedFragment is Files) {
                    val newFile = snapshot.metadata?.reference

                    if (newFile != null) {
                        wantedFragment.refreshRecyclerView(newFile)
                    } else {
                        Log.w("test", "RecycleView not updated.")
                    }
                }

                Log.w("test", "File uploaded")

            }
            .addOnFailureListener { exception ->
                notificationBuilder.setContentText("Upload failed: ${exception.message}")
                    .setProgress(0, 0, false)
                notificationManager.notify(notificationId, notificationBuilder.build())

                Log.w("test", "Failed to upload file")
            }
    }

    private fun notificationChannel(channelId: String, importance: Int) {
        val channelName = "Upload Notification Channel"
        val channelDescription = "Shows upload progress while a file is uploaded to Firebase Storage"
        val channel = NotificationChannel(channelId, channelName, importance)
        channel.description = channelDescription
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
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

    fun hideKeyboard() {
        if(currentFocus != null) {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }
}