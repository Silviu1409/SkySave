package com.example.skysave.main.files_recyclerview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.skysave.MainActivity
import com.example.skysave.R
import com.example.skysave.main.Files
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.firebase.storage.StorageReference
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt


class FileAdapter(private val context: Context?, private val fragment: Files, private val files: ArrayList<StorageReference>) : RecyclerView.Adapter<FileViewHolder>(), Filterable, Player.Listener {

    var filteredFileItems = files
    private var starredFiles: List<String> = (context as MainActivity).getUser()?.starred_files ?: listOf()

    private val fileDir = (context as MainActivity).getFileDir()

    private val mainActivityContext = (context as MainActivity)

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

        val localFile = File(fileDir, file.name)
        val tempLocalFile = File(context!!.cacheDir, file.name)

        if (file.toString() in starredFiles) {
            holder.fileStarView.setImageResource(R.drawable.icon_starred_filled)
        } else {
            holder.fileStarView.setImageResource(R.drawable.icon_starred_empty)
        }

        file.metadata.addOnSuccessListener { metadata ->

            // check if file has been already downloaded and check if the file size is the same
            if (fileDir.exists() && fileDir.isDirectory) {
                if (localFile.exists() && localFile.length() == metadata.sizeBytes) {
                    //if file has been already downloaded, remove the download button
                    val layoutParams = LinearLayoutCompat.LayoutParams(
                        LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
                        LinearLayoutCompat.LayoutParams.MATCH_PARENT
                    )
                    layoutParams.width = 0
                    holder.fileDownloadView.layoutParams = layoutParams

                    Log.w(mainActivityContext.getTag(), "${file.name} was already downloaded")
                } else {
                    Log.w(mainActivityContext.getTag(), "${file.name} was not downloaded previously")
                }
            } else {
                Log.w(mainActivityContext.getTag(), "Folder does not exist")
            }

            val fileSizeInBytes = metadata.sizeBytes.toDouble()
            val fileSize = getReadableFileSize(fileSizeInBytes)
            holder.fileDownloadSizeView.text = fileSize

            if (metadata.contentType?.startsWith("image/") == true) {
                holder.filePreviewView.visibility = View.VISIBLE
                holder.filePreviewView.setImageResource(R.drawable.image_preview)

                if(tempLocalFile.exists() && tempLocalFile.length() == metadata.sizeBytes){
                    Log.w(mainActivityContext.getTag(), "File is already cached")

                    Glide.with(holder.itemView)
                        .load(tempLocalFile)
                        .into(object : CustomTarget<Drawable>() {
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                val scale = holder.fileContentView.height.toFloat() / resource.intrinsicHeight.toFloat()
                                val scaledWidth = (scale * resource.intrinsicWidth).toInt()

                                val layoutParams = holder.fileContentView.layoutParams
                                layoutParams.width = scaledWidth
                                holder.fileContentView.layoutParams = layoutParams

                                holder.filePreviewView.setImageDrawable(resource)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) { }
                        })
                } else {
                    Glide.with(holder.itemView)
                        .load(file)
                        .into(object : CustomTarget<Drawable>() {
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                val scale = holder.fileContentView.height.toFloat() / resource.intrinsicHeight.toFloat()
                                val scaledWidth = (scale * resource.intrinsicWidth).toInt()

                                val layoutParams = holder.fileContentView.layoutParams
                                layoutParams.width = scaledWidth
                                holder.fileContentView.layoutParams = layoutParams

                                holder.filePreviewView.setImageDrawable(resource)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) { }
                        })

                    file.getFile(tempLocalFile)
                        .addOnSuccessListener {
                            Log.w(mainActivityContext.getTag(), "Saved file to cache!")
                        }
                        .addOnFailureListener { exception ->
                            Log.w(mainActivityContext.getErrTag(), exception.cause.toString())
                            Log.w(mainActivityContext.getTag(), "Failed to save file to cache!")
                        }
                }
            } else if (metadata.contentType?.startsWith("audio/") == true || metadata.contentType?.startsWith("video/") == true) {

                if (metadata.contentType?.startsWith("audio/") == true) {
                    holder.filePreviewView.setImageResource(R.drawable.audio_preview)
                } else if (metadata.contentType?.startsWith("video/") == true) {
                    holder.filePreviewView.setImageResource(R.drawable.video_preview)

                    if(tempLocalFile.exists() && tempLocalFile.length() == metadata.sizeBytes){
                        Log.w(mainActivityContext.getTag(), "File is already cached")

                        Glide.with(holder.itemView)
                            .load(tempLocalFile)
                            .into(holder.filePreviewView)
                    } else {
                        Glide.with(holder.itemView)
                            .load(file)
                            .into(holder.filePreviewView)

                        file.getFile(tempLocalFile)
                            .addOnSuccessListener {
                                Log.w(mainActivityContext.getTag(), "Saved file to cache!")
                            }
                            .addOnFailureListener { exception ->
                                Log.w(mainActivityContext.getErrTag(), exception.cause.toString())
                                Log.w(mainActivityContext.getTag(), "Failed to save file to cache!")
                            }
                    }
                }

                if(tempLocalFile.exists() && tempLocalFile.length() == metadata.sizeBytes){
                    Log.w(mainActivityContext.getTag(), "File is already cached")

                    holder.filePreviewView.visibility = View.GONE
                    holder.filePlayerView.visibility = View.VISIBLE

                    val player = ExoPlayer.Builder(context).build()
                    val mediaItem = MediaItem.fromUri(tempLocalFile.toURI().toString())

                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.addListener(this)

                    holder.filePlayerView.player = player
                } else {
                    Log.w(mainActivityContext.getTag(), "File is not cached")

                    file.getFile(tempLocalFile)
                        .addOnSuccessListener {
                            holder.filePreviewView.visibility = View.GONE
                            holder.filePlayerView.visibility = View.VISIBLE

                            val player = ExoPlayer.Builder(context).build()
                            val mediaItem = MediaItem.fromUri(tempLocalFile.toURI().toString())

                            player.setMediaItem(mediaItem)
                            player.prepare()
                            player.addListener(this)

                            holder.filePlayerView.player = player

                            Log.w(mainActivityContext.getTag(), "File is now cached!")
                        }
                        .addOnFailureListener { exception ->
                            holder.filePreviewView.visibility = View.VISIBLE
                            holder.filePlayerView.visibility = View.GONE

                            Log.w(mainActivityContext.getErrTag(), exception.cause.toString())
                            Log.w(mainActivityContext.getTag(), "Failed to cache file!")
                        }
                }
            } else {
                holder.filePreviewView.visibility = View.VISIBLE
                holder.filePlayerView.visibility = View.GONE

                val layoutParams = holder.fileContentView.layoutParams
                layoutParams.width = 100.dpToPx()
                layoutParams.height = 100.dpToPx()
                holder.fileContentView.layoutParams = layoutParams



                holder.filePreviewView.setImageResource(R.drawable.file_preview)
            }
        }

        holder.fileStarView.setOnClickListener {
            if (file.toString() !in starredFiles) {
                Toast.makeText(context, "File added to starred!", Toast.LENGTH_SHORT).show()

                val aux = starredFiles.toMutableList()
                aux.add(file.toString())
                starredFiles = aux.toList()
                mainActivityContext.getUser()?.starred_files = starredFiles

                mainActivityContext.getDb().collection("users")
                    .document(mainActivityContext.getUser()!!.uid)
                    .update("starred_files", starredFiles)
                    .addOnSuccessListener {
                        Log.w(mainActivityContext.getTag(), "Added starred file ref to db")
                    }
                    .addOnFailureListener { exception ->
                        Log.w(mainActivityContext.getErrTag(), exception.cause.toString())
                        Log.w(mainActivityContext.getTag(), "Failed to add starred file ref to db")
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
                mainActivityContext.getUser()?.starred_files = starredFiles

                mainActivityContext.getDb().collection("users")
                    .document(mainActivityContext.getUser()!!.uid)
                    .update("starred_files", starredFiles)
                    .addOnSuccessListener {
                        Log.w(mainActivityContext.getTag(), "Removed starred file ref from db")
                    }
                    .addOnFailureListener { exception ->
                        Log.w(mainActivityContext.getErrTag(), exception.cause.toString())
                        Log.w(mainActivityContext.getTag(), "Failed to remove starred file ref from db")
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

        holder.fileDownloadIconView.setOnClickListener {
            if (localFile.exists()) {
                localFile.delete()
            }

            if (tempLocalFile.exists() && tempLocalFile.length() > 0) {
                tempLocalFile.copyTo(localFile, true)
                Log.w(mainActivityContext.getTag(), "Got file from cache!")

                val layoutParams = LinearLayoutCompat.LayoutParams(
                    LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
                    LinearLayoutCompat.LayoutParams.MATCH_PARENT
                )
                layoutParams.width = 0
                holder.fileDownloadView.layoutParams = layoutParams
            } else {
                file.getFile(localFile).addOnSuccessListener {
                    Log.w(mainActivityContext.getTag(), "File downloaded successfully!")
                    Toast.makeText(context, "${file.name} downloaded!", Toast.LENGTH_SHORT).show()

                    val layoutParams = LinearLayoutCompat.LayoutParams(
                        LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
                        LinearLayoutCompat.LayoutParams.MATCH_PARENT
                    )
                    layoutParams.width = 0
                    holder.fileDownloadView.layoutParams = layoutParams
                }.addOnFailureListener { exception ->
                    Log.w(mainActivityContext.getErrTag(), exception.cause.toString())
                    Log.w(mainActivityContext.getTag(), "Failed to download file")
                }
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

                    Log.w(mainActivityContext.getTag(), "File shared")
                }
                .addOnFailureListener { exception ->
                    Log.w(mainActivityContext.getErrTag(), exception.cause.toString())
                    Log.w(mainActivityContext.getTag(), "Failed to get file download url")
                }
        }

        holder.fileTrashView.setOnClickListener {
            val newFile = mainActivityContext.getFolderRef().child("trash/${file.name}")

            file.getFile(tempLocalFile)
                .addOnSuccessListener {
                    newFile.putFile(Uri.fromFile(tempLocalFile))
                        .addOnSuccessListener {
                            files.remove(file)
                            notifyItemRemoved(position)

                            tempLocalFile.delete()
                            file.delete()

                            Log.w(mainActivityContext.getTag(), "File moved successfully!")
                            Toast.makeText(context, "${newFile.name} moved to trash!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            tempLocalFile.delete()

                            Log.w(mainActivityContext.getErrTag(), exception.cause.toString())
                            Log.w(mainActivityContext.getTag(), "Failed to move file!")
                        }
                }
                .addOnFailureListener { exception ->
                    tempLocalFile.delete()

                    Log.w(mainActivityContext.getErrTag(), exception.cause.toString())
                    Log.w(mainActivityContext.getTag(), "Failed to move file!")
                }
        }
    }

    fun addItem(newFile: StorageReference){
        files.add(newFile)
        val index = filteredFileItems.size - 1
        notifyItemInserted(index)
    }

    override fun getItemCount() = filteredFileItems.size

    private fun getReadableFileSize(size: Double): String {
        if (size <= 0) {
            return "0 bytes"
        }

        val units = arrayOf("bytes", "KB", "MB")
        val digitGroups = (log10(size) / log10(1024.0)).toInt()
        val sizeFormatted = String.format("%.2f", size / 1024.0.pow(digitGroups.toDouble()))
        val sizeRounded = BigDecimal(sizeFormatted).setScale(2, RoundingMode.HALF_UP).toDouble()

        return String.format("%.2f %s", sizeRounded, units[digitGroups])
    }

    private fun Int.dpToPx(): Int {
        val density = Resources.getSystem().displayMetrics.density
        return (this * density).roundToInt()
    }
}