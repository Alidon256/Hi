package com.example.heyyou.model

import com.google.firebase.Timestamp

class ChatMessageModel {
    @JvmField
    var message: String? = null
    @JvmField
    var senderId: String? = null
    var timestamp: Timestamp? = null

    constructor()

    constructor(message: String?, senderId: String?, timestamp: Timestamp?) {
        this.message = message
        this.senderId = senderId
        this.timestamp = timestamp
    }
}
