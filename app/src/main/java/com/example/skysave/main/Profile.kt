package com.example.skysave.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.skysave.AuthActivity
import com.example.skysave.MainActivity
import com.example.skysave.databinding.FragmentProfileBinding
import java.io.File


class Profile : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var mainActivityContext: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        mainActivityContext = (activity as MainActivity)
        mainActivityContext.setStorageFabVisibility(View.INVISIBLE)

        val tempLocalFile = File(requireContext().cacheDir, "icon.jpg")

        val imageRef = mainActivityContext.getFolderRef().child("icon.jpg")

        imageRef.metadata
            .addOnSuccessListener {  metadata ->

                if(tempLocalFile.exists() && tempLocalFile.length() == metadata.sizeBytes){
                    Log.w(mainActivityContext.getTag(), "Profile logo is already cached")

                    Glide.with(this)
                        .asBitmap()
                        .load(tempLocalFile)
                        .circleCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(binding.profileIcon)
                } else {
                    Log.w(mainActivityContext.getTag(), "Profile logo is not cached")

                    Glide.with(this)
                        .asBitmap()
                        .load(imageRef)
                        .circleCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(binding.profileIcon)

                    imageRef.getFile(tempLocalFile)
                        .addOnSuccessListener {
                            Log.w(mainActivityContext.getTag(), "Saved file to cache!")
                        }
                        .addOnFailureListener { exception ->
                            Log.w(mainActivityContext.getErrTag(), exception.cause.toString())
                            Log.w(mainActivityContext.getTag(), "Failed to save file to cache!")
                        }
                }
            }
            .addOnFailureListener {  exception ->
                Log.w(mainActivityContext.getErrTag(), exception.cause.toString())
                Log.w(mainActivityContext.getTag(), "Failed to get icon metadata!")
            }

        binding.profileAlias.text = mainActivityContext.getUser()!!.alias
        binding.profileEmail.text = binding.profileEmail.text.toString().plus(" ").plus(mainActivityContext.getUser()!!.email)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.profileLogout.setOnClickListener {
            mainActivityContext.removePreferencesUser()
            mainActivityContext.getAuth().signOut()

            val intent = Intent(activity, AuthActivity::class.java)
            intent.putExtra("logout", true)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}