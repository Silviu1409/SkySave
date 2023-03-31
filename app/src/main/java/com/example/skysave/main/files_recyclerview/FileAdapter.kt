package com.example.skysave.main.files_recyclerview

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skysave.R
import com.example.skysave.main.Files
import com.google.firebase.storage.StorageReference
import java.util.*
import kotlin.collections.ArrayList


class FileAdapter(private val fragment: Files, private val files: ArrayList<StorageReference>) : RecyclerView.Adapter<FileViewHolder>(), Filterable {

    var filteredFileItems = files

    private val filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList = if (constraint.isNullOrBlank()) {
                files
            } else {
                val filterPattern = constraint.toString().lowercase(Locale.getDefault())

                files.filter { item ->
                    item.name.lowercase(Locale.getDefault()).contains(filterPattern)
                }
            }

            val results = FilterResults()
            results.values = filteredList
            return results
        }

        @Suppress("UNCHECKED_CAST")
        @SuppressLint("NotifyDataSetChanged")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            filteredFileItems = results?.values as ArrayList<StorageReference>
            fragment.updateText()
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter {
        return filter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = filteredFileItems[position]
        holder.fileNameView.text = file.name

        file.metadata.addOnSuccessListener { metadata ->
            if (metadata.contentType?.startsWith("image/") == true) {
                holder.filePreviewView.setImageResource(R.drawable.image_preview)

                Glide.with(holder.itemView)
                    .load(file)
                    .into(holder.filePreviewView)
            } else if (metadata.contentType?.startsWith("audio/") == true) {
                holder.filePreviewView.setImageResource(R.drawable.audio_preview)
            } else if (metadata.contentType?.startsWith("video/") == true) {
                holder.filePreviewView.setImageResource(R.drawable.video_preview)

                Glide.with(holder.itemView)
                    .load(file)
                    .into(holder.filePreviewView)
            } else {
                holder.filePreviewView.setImageResource(R.drawable.file_preview)
            }
        }
    }

    fun addItem(newFile: StorageReference){
        files.add(newFile)
        val index = filteredFileItems.size - 1
        notifyItemInserted(index)
    }

    override fun getItemCount() = filteredFileItems.size
}