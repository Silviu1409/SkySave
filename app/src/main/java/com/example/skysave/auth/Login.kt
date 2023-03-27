package com.example.skysave.auth

import android.content.Context
import android.content.Intent
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
import com.example.licenta.datatypes.User
import com.example.skysave.AuthActivity
import com.example.skysave.MainActivity
import com.example.skysave.R
import com.example.skysave.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class Login : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButtonGoogle.setOnClickListener {
            (activity as AuthActivity).signIn()
        }

        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val isInternetConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString()
            val password = binding.loginPassword.text.toString()

            (activity as AuthActivity).hideKeyboard()

            requireActivity().let { activity ->
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(activity) { task ->
                        if (task.isSuccessful) {
                            val user = FirebaseAuth.getInstance().currentUser
                            val db = FirebaseFirestore.getInstance()

                            if (user != null) {
                                db.collection("users")
                                    .document(user.uid)
                                    .get()
                                    .addOnSuccessListener {document ->
                                        if (document != null && document.exists()) {
                                            val date = User("" + document.getString("email"),
                                                "" + document.getString("alias"))

                                            val intent = Intent(activity, MainActivity::class.java)
                                            intent.putExtra("user", date)
                                            startActivity(intent)
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.w((activity as AuthActivity).getTag(), "Error fetching documents", exception)
                                        Toast.makeText(activity, "Couldn't log in", Toast.LENGTH_SHORT).show()
                                    }

                            } else {
                                Log.w((activity as AuthActivity).getTag(), "User does not exist", task.exception)
                                Toast.makeText(activity, "Couldn't log in", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (isInternetConnected) {
                                Log.w((activity as AuthActivity).getTag(), "Missing network connection", task.exception)
                                Toast.makeText(activity, "You are not connected to an internet connection", Toast.LENGTH_LONG).show()
                            } else {
                                Log.w((activity as AuthActivity).getTag(), "Error logging in", task.exception)
                                Toast.makeText(activity, "Couldn't log in", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }
        }

        binding.LoginToRegister.setOnClickListener{
            findNavController().navigate(R.id.action_Login_to_Register)
        }

        binding.LoginToReset.setOnClickListener{
            findNavController().navigate(R.id.action_Login_to_ForgotPassword)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}