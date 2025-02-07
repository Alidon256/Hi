package com.example.heyyou.ui.adapter

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
        val timestamp = model.timestamp?.toDate()
        val oneDayAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time

        if (timestamp == null || timestamp.before(oneDayAgo)) {
            holder.itemView.visibility = View.GONE
        } else {
            holder.itemView.visibility = View.VISIBLE
            holder.statusText.text = model.statusText

            FirebaseUtil.getUserName(model.userId, object : FirebaseUtil.UserNameCallback {
                override fun onSuccess(name: String) {
                    holder.userName.text = name
                }

                override fun onFailure() {
                    holder.userName.text = "Unknown"
                }
            })


            // Load Profile Picture
            FirebaseUtil.getOtherProfilePicStorageRef(model.userId).downloadUrl
                .addOnSuccessListener { uri ->
                    Glide.with(holder.userImage.context)
                        .load(uri)
                        .placeholder(R.drawable.person_icon)
                        .into(holder.userImage)
                }
                .addOnFailureListener {
                    holder.userImage.setImageResource(R.drawable.person_icon)
                }

            // Handle media display
            when (model.mediaType) {
                "image" -> {
                    holder.statusImage.visibility = View.VISIBLE
                    holder.statusVideo.visibility = View.GONE
                    Glide.with(holder.statusImage.context).load(model.mediaUrl).into(holder.statusImage)
                }
                "video" -> {
                    holder.statusVideo.setVideoPath(model.mediaUrl)
                    holder.statusVideo.visibility = View.VISIBLE
                    holder.statusImage.visibility = View.GONE

                    if (!holder.statusVideo.isPlaying) {
                        Glide.with(holder.statusImage.context)
                            .load(model.mediaUrl) // Load thumbnail
                            .into(holder.statusImage)
                        holder.statusImage.visibility = View.VISIBLE
                    } else {
                        holder.statusImage.visibility = View.GONE
                    }

                    if (!holder.statusVideo.isPlaying) {
                        currentlyPlaying?.pause()
                        currentlyPlaying = holder.statusVideo
                        holder.statusVideo.start()
                    }

                    holder.statusVideo.setOnCompletionListener {
                        holder.statusVideo.resume()
                    }
                }
                else -> {
                    holder.statusImage.visibility = View.GONE
                    holder.statusVideo.visibility = View.GONE
                }
            }

            // Display timestamp
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
        val statusVideo: VideoView = itemView.findViewById(R.id.statusVideo)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        val userImage: ImageView = itemView.findViewById(R.id.me)
        val userName: TextView = itemView.findViewById(R.id.userName)
    }

    override fun onViewRecycled(holder: StatusViewHolder) {
        super.onViewRecycled(holder)
        if (holder.statusVideo.isPlaying) {
            holder.statusVideo.pause()
        }
    }
}
