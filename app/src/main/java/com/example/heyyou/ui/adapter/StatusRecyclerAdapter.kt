package com.example.heyyou.ui.adapter

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.heyyou.R
import com.example.heyyou.model.StatusModel
import com.example.heyyou.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
class StatusRecyclerAdapter(options: FirestoreRecyclerOptions<StatusModel>) :
    FirestoreRecyclerAdapter<StatusModel, StatusRecyclerAdapter.StatusViewHolder>(options) {

    private var currentlyPlaying: VideoView? = null

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int, model: StatusModel) {
        // Check if the timestamp is null or if it's older than one day
        val timestamp = model.timestamp?.toDate()
        val oneDayAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time

        // Check if the status is expired or invalid
        if (timestamp == null || timestamp.before(oneDayAgo)) {
            if (!model.statusId.isNullOrEmpty()) {
                // Remove expired status from Firestore
                FirebaseUtil.getFireStoreInstance().collection("status")
                    .document(model.statusId)
                    .delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Status deleted successfully")
                            // Remove item from RecyclerView (only if deleted successfully)
                            notifyItemRemoved(position)
                        } else {
                            Log.e(TAG, "Error deleting status: ${task.exception?.message}")
                        }
                    }
            } else {
                Log.e(TAG, "Invalid status ID, cannot delete.")
            }
        } else {
            // If the status is not expired, display the status normally
            holder.itemView.visibility = View.VISIBLE
            holder.statusText.text = model.statusText

            // Load image or video based on media type
            if (model.mediaType == "image") {
                Glide.with(holder.statusImage.context).load(model.mediaUrl).into(holder.statusImage)
                holder.statusImage.visibility = View.VISIBLE
                holder.statusVideo.visibility = View.GONE
            } else if (model.mediaType == "video") {
                holder.statusVideo.setVideoPath(model.mediaUrl)
                holder.statusVideo.visibility = View.VISIBLE
                holder.statusImage.visibility = View.GONE

                // Show thumbnail if video is paused
                if (!holder.statusVideo.isPlaying) {
                    Glide.with(holder.statusImage.context)
                        .load(model.mediaUrl) // Load thumbnail
                        .into(holder.statusImage)
                    holder.statusImage.visibility = View.VISIBLE
                } else {
                    holder.statusImage.visibility = View.GONE
                }

                // Play video if not already playing
                if (!holder.statusVideo.isPlaying) {
                    currentlyPlaying?.pause()
                    currentlyPlaying = holder.statusVideo
                    holder.statusVideo.start()
                }

                holder.statusVideo.setOnCompletionListener {
                    holder.statusVideo.resume()
                }
            } else {
                holder.statusImage.visibility = View.GONE
                holder.statusVideo.visibility = View.GONE
            }

            // Format timestamp
            holder.timestampText.text = FirebaseUtil.timestampToString(model.timestamp)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.status_item_layout, parent, false)
        return StatusViewHolder(view)
    }

    // Optional: Handle pausing videos when they go off-screen
    override fun onViewRecycled(holder: StatusViewHolder) {
        super.onViewRecycled(holder)
        if (holder.statusVideo.isPlaying) {
            holder.statusVideo.pause()
        }
    }

    class StatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val statusText: TextView = itemView.findViewById(R.id.statusText)
        val statusImage: ImageView = itemView.findViewById(R.id.statusImage)
        val statusVideo: VideoView = itemView.findViewById(R.id.statusVideo)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
    }

    companion object {
        private const val TAG = "StatusRecyclerAdapter"
    }
}
