package com.example.heyyou.model
import com.google.firebase.Timestamp;

data class ChatroomModel(
    var chatroomId: String? = null,
    var userIds: MutableList<String?>? = mutableListOf(),
    var lastMessageTimestamp: Timestamp? = null,
    var lastMessageSenderId: String? = null,
    var lastMessage: String? = ""
) {

}
