package com.example.heyyou.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.heyyou.R
import com.example.heyyou.model.StatusModel
import com.example.heyyou.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class StatusGroupAdapter(
    options: FirestoreRecyclerOptions<StatusModel>,
    private val onItemClick: (String) -> Unit
) : FirestoreRecyclerAdapter<StatusModel, StatusGroupAdapter.StatusGroupViewHolder>(options) {

    class StatusGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageBackStatus: ImageView = itemView.findViewById(R.id.imageBackStatus)
        private val imageStatusUser: ImageView = itemView.findViewById(R.id.me_top)
        private val statusCountTextView: TextView = itemView.findViewById(R.id.statusCountTextView)

        fun bind(model: StatusModel, onItemClick: (String) -> Unit) {
            FirebaseUtil.getOtherProfilePicStorageRef(model.userId).downloadUrl
                .addOnSuccessListener { uri ->
                    Glide.with(itemView.context)
                        .load(uri)
                        .placeholder(R.drawable.person_icon_white)
                        .into(imageStatusUser)
                }.addOnFailureListener {
                    imageStatusUser.setImageResource(R.drawable.person_icon_white)
                }

            when (model.mediaType) {
                "image" -> {
                    Glide.with(itemView.context)
                        .load(model.mediaUrl)
                        .placeholder(R.drawable.person_icon_white)
                        .into(imageBackStatus)
                }

                "video" -> {
                    Glide.with(itemView.context)
                        .load(model.mediaUrl)
                        .placeholder(R.drawable.person_icon_white)
                        .into(imageBackStatus)
                }
            }

            // Fetch and count statuses within the last 24 hours for this user
            val oneDayAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
            val oneDayAgoTimestamp = Timestamp(oneDayAgo)

            FirebaseFirestore.getInstance()
                .collection("status")
                .whereEqualTo("userId", model.userId)  // Filter by userId
                .whereGreaterThan("timestamp", oneDayAgoTimestamp)  // Filter by timestamp (within last 24 hours)
                .get()
                .addOnSuccessListener { result ->
                    val statusCount = result.size()
                    statusCountTextView.text ="$statusCount"
                    /*"$statusCount status${if (statusCount > 1) "es" else ""}"*/
                }
            itemView.setOnClickListener {
                onItemClick(model.userId)  // Trigger the click callback
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusGroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_status_group, parent, false)
        return StatusGroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatusGroupViewHolder, position: Int, model: StatusModel) {
        holder.bind(model, onItemClick)  // Pass the lambda to bind
    }

    override fun onDataChanged() {
        super.onDataChanged()
        notifyDataSetChanged()
    }
}
