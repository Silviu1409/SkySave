package com.example.skysave.main

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.skysave.MainActivity
import com.example.skysave.R
import com.example.skysave.databinding.FragmentFilesBinding
import com.example.skysave.main.files_recyclerview.FileAdapter
import com.google.firebase.storage.StorageReference


class Files : Fragment() {
    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FileAdapter
    private lateinit var searchView: SearchView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)

        recyclerView = binding.filesList
        searchView = binding.searchBar
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = FileAdapter(context, this, arrayListOf())
        recyclerView.adapter = adapter

        val filesRef = (activity as MainActivity).getFolderRef().child("files")
        val query = filesRef.listAll()

        query.addOnSuccessListener { result ->
            adapter = FileAdapter(context, this, result.items as ArrayList<StorageReference>)

            if  (adapter.itemCount == 0){
                binding.noFilesText.visibility = View.VISIBLE
            } else {
                binding.noFilesText.visibility = View.INVISIBLE
            }

            recyclerView.adapter = adapter
        }.addOnFailureListener {
            Log.w("test", "Cannot display RecyclerView")
        }

        binding.searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                (activity as MainActivity).hideKeyboard()
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if(newText == ""){
                    this.onQueryTextSubmit("")
                }

                adapter.filter.filter(newText)
                return false
            }
        })

        return binding.root
    }

    fun updateText(){
        if (adapter.itemCount == 0){
            binding.noFilesText.text = getString(R.string.files_no_files_found)

            binding.noFilesText.visibility = View.VISIBLE
        } else {
            binding.noFilesText.text = getString(R.string.files_no_files)

            binding.noFilesText.visibility = View.INVISIBLE
        }
    }

    fun refreshRecyclerView(newFile: StorageReference) {
        if  (adapter.itemCount==0){
            binding.noFilesText.visibility = View.INVISIBLE
        }

        adapter.addItem(newFile)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}