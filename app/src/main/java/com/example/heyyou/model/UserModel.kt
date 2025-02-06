package com.example.heyyou.model

import com.google.firebase.Timestamp

data class UserModel(
    var phone: String? = null,
    var username: String? = null,
    var createdTimestamp: Timestamp? = null,
    var userId: String? = null,
    var fcmToken: String? = null,
    var isOnline: Boolean = false,
    var searchKeywords: List<String> = listOf()
) {
    // Custom function to update search keywords
    private fun updateSearchKeywords() {
        // You can add logic here to create a list of keywords based on user information
        searchKeywords = listOfNotNull(
            username?.lowercase()?.trim(),
            phone?.take(3) // Or use a part of the phone number or any other data
        )
    }

    // Constructor that can be used to initialize the data and auto-update search keywords
    constructor(
        phone: String?,
        username: String?,
        createdTimestamp: Timestamp?,
        userId: String?,
        isOnline: Boolean
    ) : this(phone, username, createdTimestamp, userId, null, isOnline) {
        updateSearchKeywords()  // Automatically update search keywords when an instance is created
    }
}
