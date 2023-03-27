package com.example.skysave.auth

import android.content.Context
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
import com.example.skysave.AuthActivity
import com.example.skysave.R
import com.example.skysave.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


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
                            val db = FirebaseFirestore.getInstance()
                            val user = FirebaseAuth.getInstance().currentUser

                            val date = hashMapOf(
                                "email" to email,
                                "alias" to alias
                            )

                            if (user != null) {
                                db.collection("users")
                                    .document(user.uid)
                                    .set(date)
                                    .addOnSuccessListener {
                                        Log.w((activity as AuthActivity).getTag(), "Registered successfully")
                                        Toast.makeText(activity, "Registered successfully! Go to the login page to log in.", Toast.LENGTH_LONG).show()

                                        binding.registerEmail.text.clear()
                                        binding.registerPassword.text.clear()
                                        binding.registerAlias.text.clear()
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.w((activity as AuthActivity).getTag(), "Error fetching documents", exception)
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

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}