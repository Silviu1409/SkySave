package com.example.skysave.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.skysave.MainActivity
import com.example.skysave.databinding.FragmentDownloadedBinding


class Downloaded : Fragment() {
    private var _binding: FragmentDownloadedBinding? = null
    private val binding get() = _binding!!

    private lateinit var mainActivityContext: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadedBinding.inflate(inflater, container, false)

        mainActivityContext = (activity as MainActivity)
        mainActivityContext.setStorageFabVisibility(View.INVISIBLE)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}