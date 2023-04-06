package com.example.skysave.main.files_recyclerview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skysave.MainActivity
import com.example.skysave.R
import com.example.skysave.main.Files
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.*


class FileAdapter(private val context: Context?, private val fragment: Files, private val files: ArrayList<StorageReference>) : RecyclerView.Adapter<FileViewHolder>(), Filterable, Player.Listener {

    var filteredFileItems = files
    private var starredFiles: List<String> = (context as MainActivity).getUser()?.starred_files ?: listOf()

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

    @SuppressLint("NotifyDataSetChanged")
    fun filterStarred(isStarred: Boolean){
        var filteredList = mutableListOf<StorageReference>()

        if (!isStarred){
            filteredList = files
        } else {
            for (item in files){
                if (item.toString() in starredFiles){
                    filteredList.add(item)
                }
            }
        }

        filteredFileItems = filteredList as ArrayList<StorageReference>
        fragment.updateText()
        notifyDataSetChanged()
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

        if (file.toString() in starredFiles) {
            holder.fileStarView.setImageResource(R.drawable.icon_starred_filled)
        } else {
            holder.fileStarView.setImageResource(R.drawable.icon_starred_empty)
        }

        file.metadata.addOnSuccessListener { metadata ->
            if (metadata.contentType?.startsWith("image/") == true) {
                holder.filePreviewView.visibility = View.VISIBLE
                holder.filePreviewView.setImageResource(R.drawable.image_preview)

                Glide.with(holder.itemView)
                    .load(file)
                    .into(holder.filePreviewView)
            } else if (metadata.contentType?.startsWith("audio/") == true || metadata.contentType?.startsWith("video/") == true) {

                if (metadata.contentType?.startsWith("audio/") == true) {
                    holder.filePreviewView.setImageResource(R.drawable.audio_preview)
                } else if (metadata.contentType?.startsWith("video/") == true) {
                    holder.filePreviewView.setImageResource(R.drawable.video_preview)

                    Glide.with(holder.itemView)
                        .load(file)
                        .into(holder.filePreviewView)
                }

                val localFile = File.createTempFile(file.name.substringBeforeLast("."), file.name.substringAfterLast(".", ""))

                file.getFile(localFile)
                    .addOnSuccessListener {
                        holder.filePreviewView.visibility = View.GONE
                        holder.filePlayerView.visibility = View.VISIBLE

                        val player = ExoPlayer.Builder(context!!).build()
                        val mediaItem = MediaItem.fromUri(localFile.toURI().toString())

                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.addListener(this)

                        holder.filePlayerView.player = player
                    }
                    .addOnFailureListener { exception ->
                        holder.filePreviewView.visibility = View.VISIBLE
                        holder.filePlayerView.visibility = View.GONE

                        Log.w((context as MainActivity).getErrTag(), exception.cause.toString())
                        Log.w(context.getTag(), "Failed to download file!")
                    }
            } else {
                holder.filePreviewView.visibility = View.VISIBLE
                holder.filePlayerView.visibility = View.GONE
                holder.filePreviewView.setImageResource(R.drawable.file_preview)
            }
        }

        holder.fileStarView.setOnClickListener {
            if(context != null) {
                if (file.toString() !in starredFiles) {
                    Toast.makeText(context, "File added to starred!", Toast.LENGTH_SHORT).show()

                    val aux = starredFiles.toMutableList()
                    aux.add(file.toString())
                    starredFiles = aux.toList()
                    (context as MainActivity).getUser()?.starred_files = starredFiles

                    context.getDb().collection("users")
                        .document(context.getUser()!!.uid)
                        .update("starred_files", starredFiles)
                        .addOnSuccessListener {
                            Log.w(context.getTag(), "Added starred file ref to db")
                        }
                        .addOnFailureListener { exception ->
                            Log.w(context.getErrTag(), exception.cause.toString())
                            Log.w(context.getTag(), "Failed to add starred file ref to db")
                        }

                    val emptyStar = ContextCompat.getDrawable(context, R.drawable.icon_starred_empty)
                    val fullStar = ContextCompat.getDrawable(context, R.drawable.icon_starred_filled)

                    holder.fileStarView.setImageDrawable(emptyStar)

                    val anim = ObjectAnimator.ofPropertyValuesHolder(
                        holder.fileStarView,
                        PropertyValuesHolder.ofFloat("scaleX", 0f, 1f),
                        PropertyValuesHolder.ofFloat("scaleY", 0f, 1f)
                    )
                    anim.duration = 300
                    anim.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            holder.fileStarView.setImageDrawable(fullStar)
                        }
                    })
                    anim.start()
                } else {
                    Toast.makeText(context, "File removed from starred!", Toast.LENGTH_SHORT).show()

                    val aux = starredFiles.toMutableList()
                    aux.remove(file.toString())
                    starredFiles = aux.toList()
                    (context as MainActivity).getUser()?.starred_files = starredFiles

                    context.getDb().collection("users")
                        .document(context.getUser()!!.uid)
                        .update("starred_files", starredFiles)
                        .addOnSuccessListener {
                            Log.w(context.getTag(), "Removed starred file ref from db")
                        }
                        .addOnFailureListener { exception ->
                            Log.w(context.getErrTag(), exception.cause.toString())
                            Log.w(context.getTag(), "Failed to remove starred file ref from db")
                        }

                    val emptyStar = ContextCompat.getDrawable(context, R.drawable.icon_starred_empty)
                    val fullStar = ContextCompat.getDrawable(context, R.drawable.icon_starred_filled)

                    holder.fileStarView.setImageDrawable(fullStar)

                    val anim = ObjectAnimator.ofPropertyValuesHolder(
                        holder.fileStarView,
                        PropertyValuesHolder.ofFloat("scaleX", 0f, 1f),
                        PropertyValuesHolder.ofFloat("scaleY", 0f, 1f)
                    )
                    anim.duration = 300
                    anim.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            holder.fileStarView.setImageDrawable(emptyStar)
                        }
                    })
                    anim.start()
                }
            }
        }

        holder.fileDownloadView.setOnClickListener {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appName = context!!.getString(R.string.app_name)
            val appDir = File(downloadsDir, appName)
            if (!appDir.exists()) {
                appDir.mkdir()
            }

            val localFile = File(appDir, file.name)
            if (localFile.exists()) {
                localFile.delete()
            }

            file.getFile(localFile).addOnSuccessListener {
                Log.w((context as MainActivity).getTag(), "File downloaded successfully!")
                Toast.makeText(context, "${file.name} downloaded!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                Log.w((context as MainActivity).getErrTag(), exception.cause.toString())
                Log.w(context.getTag(), "Failed to download file")
            }
        }

        holder.fileShareView.setOnClickListener {
            file.downloadUrl
                .addOnSuccessListener { uri ->
                    val fileUrl = uri.toString()
                    val fileType = "text/html"


                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = fileType
                        putExtra(Intent.EXTRA_TITLE, file.name)
                        putExtra(Intent.EXTRA_TEXT, fileUrl)
                    }

                    val chooser = Intent.createChooser(sendIntent, "Share file...")
                    holder.itemView.context.startActivity(chooser)

                    Log.w((context as MainActivity).getTag(), "File shared")
                }
                .addOnFailureListener { exception ->
                    Log.w((context as MainActivity).getErrTag(), exception.cause.toString())
                    Log.w(context.getTag(), "Failed to get file download url")
                }
        }

        holder.fileTrashView.setOnClickListener {
            val newFile = (context as MainActivity).getFolderRef().child("trash/${file.name}")

            val localFile = File.createTempFile(file.name.substringBeforeLast("."), file.name.substringAfterLast(".", ""))
            file.getFile(localFile)
                .addOnSuccessListener {
                    newFile.putFile(Uri.fromFile(localFile))
                        .addOnSuccessListener {
                            files.remove(file)
                            notifyItemRemoved(position)

                            localFile.delete()
                            file.delete()

                            Log.w(context.getTag(), "File moved successfully!")
                            Toast.makeText(context, "${newFile.name} moved to trash!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            localFile.delete()

                            Log.w(context.getErrTag(), exception.cause.toString())
                            Log.w(context.getTag(), "Failed to move file!")
                        }
                }
                .addOnFailureListener { exception ->
                    localFile.delete()

                    Log.w(context.getErrTag(), exception.cause.toString())
                    Log.w(context.getTag(), "Failed to move file!")
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