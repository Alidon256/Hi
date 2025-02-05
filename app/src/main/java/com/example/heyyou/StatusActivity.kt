package com.example.heyyou

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.heyyou.adapter.StatusRecyclerAdapter
import com.example.heyyou.model.StatusModel
import com.example.heyyou.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class StatusActivity : AppCompatActivity() {

    private lateinit var statusEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var statusImageView: ImageView
    private lateinit var uploadButton: Button
    private var mediaUri: Uri? = null
    private var mediaType: String? = null  // "image" or "video"
    private var adapter: StatusRecyclerAdapter? = null
    private lateinit var statusVideoView: VideoView

    private val PICK_IMAGE_REQUEST = 1
    private val PICK_VIDEO_REQUEST = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        statusEditText = findViewById(R.id.statusEditText)
        statusImageView = findViewById(R.id.statusImageView)
        uploadButton = findViewById(R.id.uploadButton)
        recyclerView = findViewById(R.id.recyclerView)
        statusVideoView = findViewById(R.id.statusVideo)

        statusImageView.setOnClickListener {
            // Create an options dialog for selecting image or video
            val options = arrayOf("Select Image", "Select Video")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select Media")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> { // If "Select Image" is clicked
                        val intent = Intent(Intent.ACTION_PICK)
                        intent.type = "image/*"
                        startActivityForResult(intent, PICK_IMAGE_REQUEST)
                    }
                    1 -> { // If "Select Video" is clicked
                        val intent = Intent(Intent.ACTION_PICK)
                        intent.type = "video/*"
                        startActivityForResult(intent, PICK_VIDEO_REQUEST)
                    }
                }
            }
            builder.show()
        }


        setupStatusRecyclerView()

        uploadButton.setOnClickListener {
            val statusText = statusEditText.text.toString().trim()

            if (mediaUri != null && mediaType != null) {
                // If it's a video, check the duration before uploading
                if (mediaType == "video" && !isVideoDurationValid(mediaUri!!)) {
                    Toast.makeText(this, "Video must be 30 seconds or less", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                FirebaseUtil.uploadStatus(this@StatusActivity, statusText, mediaUri!!, mediaType!!) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Status uploaded!", Toast.LENGTH_SHORT).show()
                        finish()  // Close activity after uploading
                    } else {
                        Toast.makeText(this, "Failed to upload status", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please select an image or video", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    mediaUri = data.data!!
                    mediaType = "image"
                    statusImageView.setImageURI(mediaUri)
                    statusImageView.visibility = View.VISIBLE
                    statusVideoView.visibility = View.GONE  // Hide VideoView
                }
                PICK_VIDEO_REQUEST -> {
                    mediaUri = data.data!!
                    mediaType = "video"
                    // You can use Glide or MediaMetadataRetriever to show a thumbnail of the video
                    statusImageView.visibility = View.GONE  // Hide ImageView
                    statusVideoView.setVideoURI(mediaUri)  // Set the video URI to the VideoView
                    statusVideoView.visibility = View.VISIBLE  // Show VideoView for video playback
                    statusVideoView.start()  // Start video playback if needed
                }
            }
        }
    }

    private fun setupStatusRecyclerView() {
        val query = FirebaseFirestore.getInstance().collection("status")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<StatusModel>()
            .setQuery(query, StatusModel::class.java)
            .build()

        adapter = StatusRecyclerAdapter(options)
        recyclerView.layoutManager = LinearLayoutManager(this@StatusActivity)
        recyclerView.adapter = adapter
        adapter?.startListening()
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    private fun isVideoDurationValid(videoUri: Uri): Boolean {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, videoUri)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationInMs = durationStr?.toLongOrNull() ?: 0
        retriever.release()
        return durationInMs <= 30000  // 30 seconds in milliseconds
    }
}
