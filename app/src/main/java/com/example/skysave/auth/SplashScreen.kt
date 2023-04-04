package com.example.skysave.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.navigation.fragment.findNavController
import com.example.skysave.AuthActivity
import com.example.skysave.MainActivity
import com.example.skysave.R
import com.example.skysave.databinding.FragmentSplashScreenBinding
import com.example.skysave.datatypes.User


@SuppressLint("CustomSplashScreen")
class SplashScreen : Fragment() {
    private var _binding: FragmentSplashScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashScreenBinding.inflate(inflater, container, false)

        val motionLayout = binding.SplashScreen
        motionLayout.addTransitionListener(object: MotionLayout.TransitionListener{
            override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {
                if ((activity as AuthActivity).getLogout()){
                    findNavController().navigate(R.id.action_SplashScreen_to_Login)
                }
            }

            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {

            }

            override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {

            }

            @Suppress("UNCHECKED_CAST")
            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                val act = activity as AuthActivity

                if (act.getUser() != null) {
                    act.getDB().collection("users")
                        .document(act.getUser()!!.uid)
                        .get()
                        .addOnSuccessListener {document ->
                            if (document != null && document.exists()) {
                                val dateUser = User(act.getUser()!!.uid,
                                    "" + document.getString("email"),
                                    "" + document.getString("alias"),
                                    document.get("starred_files") as? List<String> ?: listOf()
                                )

                                val intent = Intent(activity, MainActivity::class.java)
                                intent.putExtra("user", dateUser)
                                startActivity(intent)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w(tag, "Error fetching documents", exception)
                            Toast.makeText(context, "Couldn't automatically log in", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    findNavController().navigate(R.id.action_SplashScreen_to_Login)
                }
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}