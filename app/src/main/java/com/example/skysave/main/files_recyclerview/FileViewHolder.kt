package com.example.skysave.main.files_recyclerview

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.skysave.R


class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val fileNameView: TextView = itemView.findViewById(R.id.file_name)
    val filePreviewView: ImageView = itemView.findViewById(R.id.file_preview)
}