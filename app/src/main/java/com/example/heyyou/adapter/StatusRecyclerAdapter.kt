package com.example.heyyou.adapter

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
import java.util.*

class StatusRecyclerAdapter(options: FirestoreRecyclerOptions<StatusModel>) :
    FirestoreRecyclerAdapter<StatusModel, StatusRecyclerAdapter.StatusViewHolder>(options) {

    private var currentlyPlaying: VideoView? = null

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int, model: StatusModel) {
        // Check if the timestamp is null or if it's older than one day
        val timestamp = model.timestamp?.toDate()
        val oneDayAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        if (timestamp == null || timestamp.before(oneDayAgo)) {
            holder.itemView.visibility = View.GONE  // Hide the item if timestamp is null or older than one day
        } else {
            holder.itemView.visibility = View.VISIBLE
            holder.statusText.text = model.statusText

            // Check media type and load accordingly
            if (model.mediaType == "image") {
                Glide.with(holder.statusImage.context).load(model.mediaUrl).into(holder.statusImage)
                holder.statusImage.visibility = View.VISIBLE
                holder.statusVideo.visibility = View.GONE
            } else if (model.mediaType == "video") {
                // Load the video into the VideoView
                holder.statusVideo.setVideoPath(model.mediaUrl)
                holder.statusVideo.visibility = View.VISIBLE
                holder.statusImage.visibility = View.GONE

                // Show the thumbnail if video is paused or not playing
                if (!holder.statusVideo.isPlaying) {
                    Glide.with(holder.statusImage.context)
                        .load(model.mediaUrl)  // Load the video thumbnail
                        .into(holder.statusImage)
                    holder.statusImage.visibility = View.VISIBLE
                } else {
                    holder.statusImage.visibility = View.GONE
                }

                // Start the video if it's not already playing
                if (!holder.statusVideo.isPlaying) {
                    // Pause any currently playing video
                    currentlyPlaying?.pause()
                    currentlyPlaying = holder.statusVideo
                    holder.statusVideo.start()
                }

                // Optionally, add a listener to stop the video after it finishes
                holder.statusVideo.setOnCompletionListener {
                    // Stop playback when finished
                    holder.statusVideo.resume()
                }
            } else {
                holder.statusImage.visibility = View.GONE
                holder.statusVideo.visibility = View.GONE
            }

            // Format and display the timestamp
            holder.timestampText.text = FirebaseUtil.timestampToString(model.timestamp)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.status_item_layout, parent, false)
        return StatusViewHolder(view)
    }

    override fun onDataChanged() {
        super.onDataChanged()
        notifyDataSetChanged()
    }

    class StatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val statusText: TextView = itemView.findViewById(R.id.statusText)
        val statusImage: ImageView = itemView.findViewById(R.id.statusImage)
        val statusVideo: VideoView = itemView.findViewById(R.id.statusVideo)  // Video view
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
    }

    // Optionally, override this method to stop videos when they go off-screen
    override fun onViewRecycled(holder: StatusViewHolder) {
        super.onViewRecycled(holder)
        // Pause video if it goes off-screen
        if (holder.statusVideo.isPlaying) {
            holder.statusVideo.pause()
        }
    }
}
