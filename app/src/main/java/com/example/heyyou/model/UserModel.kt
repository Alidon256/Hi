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
    init {
        updateSearchKeywords() // Call in the init block
    }

    // Generate all possible substrings of the username for search
    private fun updateSearchKeywords() {
        val name = username?.lowercase()?.trim() ?: return
        val keywords = mutableListOf<String>()

        for (i in name.indices) {
            for (j in i..<name.length) { // Corrected line
                keywords.add(name.substring(i, j + 1)) // Corrected line
            }
        }

        searchKeywords = keywords.filter { it.isNotEmpty() }.distinct()
    }

    // Auto-generate search keywords when instance is created
    constructor(
        phone: String?,
        username: String?,
        createdTimestamp: Timestamp?,
        userId: String?,
        isOnline: Boolean
    ) : this(phone, username, createdTimestamp, userId, null, isOnline) {
        //updateSearchKeywords() // No need to call here anymore
    }
}