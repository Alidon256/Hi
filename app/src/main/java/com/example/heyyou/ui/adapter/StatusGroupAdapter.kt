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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.util.*

class StatusGroupAdapter(
    private var statusList: List<StatusModel>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<StatusGroupAdapter.StatusGroupViewHolder>() {

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

            // Load status image/video
            Glide.with(itemView.context)
                .load(model.mediaUrl)
                .placeholder(R.drawable.person_icon_white)
                .into(imageBackStatus)

            // Real-time status count update
            val oneDayAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
            val oneDayAgoTimestamp = Timestamp(oneDayAgo)

            FirebaseFirestore.getInstance()
                .collection("status")
                .whereEqualTo("userId", model.userId)
                .whereGreaterThan("timestamp", oneDayAgoTimestamp)
                .addSnapshotListener { result, _ ->
                    if (result != null) {
                        val statusCount = result.size()
                        statusCountTextView.text = "$statusCount"
                    }
                }

            itemView.setOnClickListener {
                onItemClick(model.userId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusGroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_status_group, parent, false)
        return StatusGroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatusGroupViewHolder, position: Int) {
        holder.bind(statusList[position], onItemClick)
    }

    override fun getItemCount(): Int = statusList.size

    fun updateStatusList(newList: List<StatusModel>) {
        statusList = newList
        notifyDataSetChanged()
    }
}
