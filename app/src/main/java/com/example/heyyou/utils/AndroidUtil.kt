package com.example.heyyou.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.heyyou.model.UserModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.LocalCacheSettings

object AndroidUtil {
    @JvmStatic
    fun showToast(context: Context?, message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    @JvmStatic
    fun passUserModelAsIntent(intent: Intent, model: UserModel) {
        intent.putExtra("username", model.username)
        intent.putExtra("phone", model.phone)
        intent.putExtra("userId", model.userId)
        intent.putExtra("fcmToken", model.fcmToken)
    }

    fun getUserModelFromIntent(intent: Intent): UserModel {
        val userModel = UserModel()
        userModel.username = intent.getStringExtra("username")
        userModel.phone = intent.getStringExtra("phone")
        userModel.userId = intent.getStringExtra("userId")
        userModel.fcmToken = intent.getStringExtra("fcmToken")
        return userModel
    }

    @JvmStatic
    fun setProfilePic(context: Context, imageUri: Uri?, imageView: ImageView) {
        FirebaseFirestore.getInstance().firestoreSettings.cacheSettings
        Glide
            .with(context)
            .load(imageUri)
            .apply(RequestOptions.circleCropTransform())
            .into(imageView)
    }
}
