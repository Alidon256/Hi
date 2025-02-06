package com.example.heyyou.ui.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.heyyou.R
import com.example.heyyou.model.UserModel
import com.example.heyyou.ui.Fragments.ChatActivity
import com.example.heyyou.ui.adapter.SearchUserRecyclerAdapter.UserModelViewHolder
import com.example.heyyou.utils.AndroidUtil
import com.example.heyyou.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.tasks.Task

class SearchUserRecyclerAdapter(
    options: FirestoreRecyclerOptions<UserModel?>,
    var context: Context
) :
    FirestoreRecyclerAdapter<UserModel?, UserModelViewHolder?>(options) {
    override fun onBindViewHolder(holder: UserModelViewHolder, position: Int, model: UserModel) {
        holder.usernameText.text = model.username
        holder.phoneText.text = model.phone
        if (model.userId == FirebaseUtil.currentUserId()) {
            holder.usernameText.text = model.username + " (Me)"
        }

        FirebaseUtil.getOtherProfilePicStorageRef(model.userId).downloadUrl
            .addOnCompleteListener { t: Task<Uri?> ->
                if (t.isSuccessful) {
                    val uri = t.result
                    AndroidUtil.setProfilePic(context, uri, holder.profilePic)
                }
            }

        holder.itemView.setOnClickListener {
            //navigate to chat activity
            val intent = Intent(context, ChatActivity::class.java)
            AndroidUtil.passUserModelAsIntent(intent, model)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserModelViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.search_user_recycler_row, parent, false)
        return UserModelViewHolder(view)
    }

    inner class UserModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var usernameText: TextView = itemView.findViewById(R.id.user_name_text)
        var phoneText: TextView = itemView.findViewById(R.id.phone_text)
        var profilePic: ImageView = itemView.findViewById(R.id.profile_pic_image_view)
    }
}
