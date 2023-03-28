package com.example.skysave.main

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.skysave.AuthActivity
import com.example.skysave.MainActivity
import com.example.skysave.databinding.FragmentProfileBinding


class Profile : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        val folderRef = (activity as MainActivity).getStorage().child((activity as MainActivity).getUser()!!.uid)
        val imageRef = folderRef.child("icon.jpg")

        val glide = Glide.with(this)
        val requestBuilder = glide.asBitmap().load(imageRef).circleCrop()
        requestBuilder.into(binding.profileIcon)

        binding.profileAlias.text = (activity as MainActivity).getUser()!!.alias
        binding.profileEmail.text = binding.profileEmail.text.toString().plus(" ").plus((activity as MainActivity).getUser()!!.email)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.profileLogout.setOnClickListener {
            (activity as MainActivity).getAuth().signOut()

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