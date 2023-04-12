package com.example.skysave.auth

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.skysave.AuthActivity
import com.example.skysave.R
import com.example.skysave.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream


class Register : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val isInternetConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        binding.registerButton.setOnClickListener {
            val email = binding.registerEmail.text.toString()
            val password = binding.registerPassword.text.toString()
            val alias = binding.registerAlias.text.toString()

            (activity as AuthActivity).hideKeyboard()

            requireActivity().let { activity ->
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(activity) { task ->
                        if (task.isSuccessful) {
                            val user = FirebaseAuth.getInstance().currentUser

                            val date = HashMap<String, Any>()
                            date["email"] = email
                            date["alias"] = alias
                            date["starred_files"] = listOf<String>()

                            if (user != null) {
                                (activity as AuthActivity).getDB().collection("users")
                                    .document(user.uid)
                                    .set(date)
                                    .addOnSuccessListener {
                                        val glide = Glide.with(this)

                                        val requestBuilder = glide.asBitmap()
                                            .load(R.drawable.default_icon)
                                            .apply(RequestOptions().override(75, 75))
                                        requestBuilder.into(object : CustomTarget<Bitmap>() {
                                            override fun onResourceReady(
                                                resource: Bitmap,
                                                transition: Transition<in Bitmap>?
                                            ) {
                                                val folderRef = activity.getStorage().child(user.uid)
                                                val imageRef = folderRef.child("icon.jpg")

                                                val baos = ByteArrayOutputStream()
                                                resource.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                                                val data = baos.toByteArray()

                                                val uploadTask = imageRef.putBytes(data)
                                                uploadTask
                                                    .addOnSuccessListener {
                                                        Log.w(tag, "Created folder for user")

                                                        Log.w(activity.getTag(), "Registered successfully")
                                                        Toast.makeText(activity, "Registered successfully! Go to the login page to log in.", Toast.LENGTH_LONG).show()

                                                        binding.registerEmail.text?.clear()
                                                        binding.registerPassword.text?.clear()
                                                        binding.registerAlias.text?.clear()
                                                    }
                                                    .addOnFailureListener {exception ->
                                                        Log.w(tag, "Error creating folder", exception)
                                                        Toast.makeText(activity, "Couldn't create folder", Toast.LENGTH_SHORT).show()
                                                    }
                                            }

                                            override fun onLoadCleared(placeholder: Drawable?) {
                                                Log.w(tag, "Cancelled load")
                                            }
                                        })
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.w(activity.getTag(), "Error fetching documents", exception)
                                        Toast.makeText(activity, "Couldn't register", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Log.w((activity as AuthActivity).getTag(), "User wasn't created", task.exception)
                                Toast.makeText(activity, "Couldn't register", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (isInternetConnected) {
                                Log.w((activity as AuthActivity).getTag(), "Missing network connection", task.exception)
                                Toast.makeText(activity, "You are not connected to an internet connection", Toast.LENGTH_LONG).show()
                            } else {
                                Log.w((activity as AuthActivity).getTag(), "Missing network connection", task.exception)
                                Toast.makeText(activity, "Couldn't register", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }
        }

        binding.RegisterToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_Register_to_Login)
        }

        binding.RegisterToReset.setOnClickListener {
            findNavController().navigate(R.id.action_Register_to_ForgotPassword)
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ) {
            val layoutParams = binding.registerTitle.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin = 100

            binding.registerTitle.layoutParams = layoutParams
        } else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            val layoutParams = binding.registerTitle.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin = 250

            binding.registerTitle.layoutParams = layoutParams
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}