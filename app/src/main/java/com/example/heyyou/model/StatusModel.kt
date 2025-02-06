package com.example.heyyou.model

import com.google.firebase.Timestamp

data class StatusModel(
    val userId: String = "",
    val mediaUrl: String = "",  // Can be an image or video URL
    val statusText: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val mediaType: String = "",  // "image" or "video"
    val statusId: String = ""
)

