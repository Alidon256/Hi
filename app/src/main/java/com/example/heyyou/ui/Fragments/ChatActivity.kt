package com.example.heyyou.ui.Fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.heyyou.R
import com.example.heyyou.ui.adapter.ChatRecyclerAdapter
import com.example.heyyou.model.ChatMessageModel
import com.example.heyyou.model.ChatroomModel
import com.example.heyyou.model.UserModel
import com.example.heyyou.utils.AndroidUtil
import com.example.heyyou.utils.FirebaseUtil
import com.example.mindset.WebpTranscoder
import okhttp3.MediaType.Companion.toMediaType
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ChatActivity : AppCompatActivity() {
    private var otherUser: UserModel? = null
    private var chatroomId: String? = null
    private var chatroomModel: ChatroomModel? = null
    private var adapter: ChatRecyclerAdapter? = null

    private lateinit var messageInput: EditText
    private lateinit var sendMessageBtn: ImageButton
    private lateinit var backBtn: ImageButton
    private lateinit var otherUsername: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageView: ImageView
    private lateinit var menuChatIcon: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Initialize views
        messageInput = findViewById(R.id.chat_message_input)
        sendMessageBtn = findViewById(R.id.message_send_btn)
        backBtn = findViewById(R.id.back_btn)
        otherUsername = findViewById(R.id.other_username)
        recyclerView = findViewById(R.id.chat_recycler_view)
        imageView = findViewById(R.id.profile_pic_image_view)
        menuChatIcon = findViewById(R.id.menu_chat_icon)

        val savedBg = getSavedBackgroundImage()
        savedBg?.let {
            updateChatBackground(Uri.parse(it))
        }
        menuChatIcon.setOnClickListener { showChatMenu(it) }


        // Get UserModel from intent
        otherUser = AndroidUtil.getUserModelFromIntent(intent)
        if (otherUser == null) {
            Log.e("ChatActivity", "Other user data is missing!")
            finish()
            return
        }

        // Set chatroom ID
        chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser!!.userId)


        FirebaseUtil.getOtherProfilePicStorageRef(otherUser!!.userId).downloadUrl
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let { uri ->
                        AndroidUtil.setProfilePic(this, uri, imageView)
                    }
                }
            }

        otherUsername.text = otherUser!!.username

        backBtn.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        sendMessageBtn.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessageToUser(message)
            }
        }

        // Initialize chatroom and recycler view
        getOrCreateChatroomModel {
            setupChatRecyclerView() // Setup RecyclerView only after chatroom model is available
        }
        otherUser!!.userId?.let { listenForOtherUserStatus(it) }
    }
    private fun showChatMenu(view: View) {
        val popupMenu = androidx.appcompat.widget.PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.chat_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.change_theme -> {
                    pickImageFromGallery() // Open gallery when "Change Theme" is clicked
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            saveBackgroundImage(it.toString())  // Save the selected image URI
            updateChatBackground(it)  // Apply the image
        }
    }

    private fun saveBackgroundImage(uri: String) {
        val sharedPreferences = getSharedPreferences("ChatPrefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("chat_bg", uri).apply()
    }

    private fun getSavedBackgroundImage(): String? {
        val sharedPreferences = getSharedPreferences("ChatPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("chat_bg", null)
    }
    private fun updateChatBackground(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .diskCacheStrategy(DiskCacheStrategy.ALL)  // Cache image for better performance
            .apply(RequestOptions.bitmapTransform(WebpTranscoder()))
            .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                override fun onResourceReady(resource: android.graphics.drawable.Drawable, transition: com.bumptech.glide.request.transition.Transition<in android.graphics.drawable.Drawable>?) {
                    recyclerView.background = resource
                    val bottomLayout = findViewById<RelativeLayout>(R.id.bottom_layout)
                    bottomLayout.background = resource
                }
                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                    recyclerView.background = placeholder
                }
            })

    }


    fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun listenForOtherUserStatus(otherUserId: String) {
        FirebaseUtil.listenForUserStatus(otherUserId) { isOnline ->
            updateUserStatusUI(isOnline)
        }

    }
    private fun updateUserStatusUI(isOnline: Boolean) {
        val statusIndicator = findViewById<ImageView>(R.id.status_dot)

        val color = if (isOnline) {
            ContextCompat.getColor(this, R.color.chat_color_sender) // Online
        } else {
            ContextCompat.getColor(this, R.color.light_gray) // Offline
        }

        statusIndicator.setColorFilter(color)
    }



    private fun setupChatRecyclerView() {
        val query = FirebaseUtil.getChatroomMessageReference(chatroomId)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<ChatMessageModel>()
            .setQuery(query, ChatMessageModel::class.java).build()

        adapter = ChatRecyclerAdapter(options, applicationContext)
        val manager = LinearLayoutManager(this).apply { reverseLayout = true }

        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
        adapter?.startListening()

        adapter?.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                recyclerView.smoothScrollToPosition(0)
            }
        })
    }

    private fun sendMessageToUser(message: String) {
        if (chatroomModel == null) {
            Log.e("ChatActivity", "Chatroom model is null! Fetching or creating chatroom...")
            getOrCreateChatroomModel {
                sendMessageToUserAfterChatroomCreated(message) // Send message after chatroom is ready
            }
        } else {
            sendMessageToUserAfterChatroomCreated(message)
        }
    }

    private fun sendMessageToUserAfterChatroomCreated(message: String) {
        if (chatroomModel == null) {
            Log.e("ChatActivity", "Chatroom model is still null!")
            return
        }

        val currentTime = Timestamp.now()

        chatroomModel?.let {
            it.lastMessageTimestamp = currentTime
            it.lastMessageSenderId = FirebaseUtil.currentUserId()
            it.lastMessage = message

            // Update chatroom details in Firestore
            FirebaseUtil.getChatroomReference(chatroomId).set(it)
                .addOnFailureListener { e ->
                    Log.e("ChatActivity", "Failed to update chatroom model: ${e.message}")
                }

            // Create message object
            val chatMessageModel = ChatMessageModel(message, FirebaseUtil.currentUserId(), currentTime)

            // Send message to Firestore
            FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        messageInput.setText("")
                        sendNotification(message)
                    } else {
                        Log.e("ChatActivity", "Message send failed: ${task.exception?.message}")
                    }
                }
        }
    }

    private fun getOrCreateChatroomModel(callback: () -> Unit) {
        FirebaseUtil.getChatroomReference(chatroomId).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    chatroomModel = task.result?.toObject(ChatroomModel::class.java)
                    if (chatroomModel == null) {
                        chatroomModel = ChatroomModel(
                            chatroomId,
                            mutableListOf(FirebaseUtil.currentUserId(), otherUser!!.userId),
                            Timestamp.now(),
                            null,
                            ""
                        )
                        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel!!)
                            .addOnCompleteListener { task1 ->
                                if (task1.isSuccessful) {
                                    Log.d("ChatActivity", "Chatroom created successfully.")
                                    callback() // Call the callback after chatroom is created
                                } else {
                                    Log.e("ChatActivity", "Failed to create chatroom: ${task1.exception?.message}")
                                }
                            }
                    } else {
                        callback() // If chatroom exists, directly call the callback
                    }
                } else {
                    Log.e("ChatActivity", "Failed to check chatroom existence: ${task.exception?.message}")
                }
            }
    }

    private fun sendNotification(message: String) {
        FirebaseUtil.currentUserDetails().get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.toObject(UserModel::class.java)?.let { currentUser ->
                        val jsonObject = JSONObject().apply {
                            put("notification", JSONObject().apply {
                                put("title", currentUser.username)
                                put("body", message)
                            })
                            put("data", JSONObject().apply {
                                put("userId", currentUser.userId)
                            })
                            put("to", otherUser!!.fcmToken)
                        }
                        callApi(jsonObject)
                    }
                }
            }
    }

    private fun callApi(jsonObject: JSONObject) {
        val JSON = "application/json; charset=utf-8".toMediaType()
        val client = OkHttpClient()
        val url = "https://fcm.googleapis.com/fcm/send"
        val body = RequestBody.create(JSON, jsonObject.toString())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer API_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ChatActivity", "Notification API call failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                // Log.d("ChatActivity", "Notification sent successfully: ${response.message()}")
            }
        })
    }
}
