package com.example.heyyou.ui.Fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.heyyou.R
import com.example.heyyou.ui.adapter.StatusRecyclerAdapter
import com.example.heyyou.model.StatusModel
import com.example.heyyou.ui.adapter.StatusGroupAdapter
import com.example.heyyou.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Calendar

class StatusFragment : Fragment() {

    private lateinit var statusEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var statusImageView: ImageView
    private lateinit var uploadButton: ImageButton
    private var mediaUri: Uri? = null
    private var mediaType: String? = null  // "image" or "video"
    private var adapter: StatusRecyclerAdapter? = null
    private lateinit var statusVideoView: VideoView
    private lateinit var constraintStatus: ConstraintLayout
    private lateinit var fab: FloatingActionButton
    private lateinit var backStatus: ImageButton
    private lateinit var statusGroupAdapter: StatusGroupAdapter
    private lateinit var statusGroupRecyclerView: RecyclerView

    private val PICK_IMAGE_REQUEST = 1
    private val PICK_VIDEO_REQUEST = 2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_status, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        statusEditText = view.findViewById(R.id.statusEditText)
        statusImageView = view.findViewById(R.id.statusImageView)
        uploadButton = view.findViewById(R.id.uploadButton)
        statusVideoView = view.findViewById(R.id.statusVideo)
        constraintStatus = view.findViewById(R.id.constraintStatus)
        fab = view.findViewById(R.id.fabStatus)
        backStatus = view.findViewById(R.id.back_btn_status)
        statusGroupRecyclerView = view.findViewById(R.id.recyclerViewStatus)
        fab.isVisible = true

        setupClickListeners()
        setupStatusRecyclerView()
        setupStatusGroupRecyclerView()

        return view
    }

    private fun setupStatusGroupRecyclerView() {
        val oneDayAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        val query = FirebaseFirestore.getInstance()
            .collection("status")
            .whereGreaterThan("timestamp", Timestamp(oneDayAgo)) // Filter out old statuses
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)  // Limit the query to just one document

        val options = FirestoreRecyclerOptions.Builder<StatusModel>()
            .setQuery(query, StatusModel::class.java)
            .build()

        statusGroupAdapter = StatusGroupAdapter(options ){ userId ->
            // Handle item click and fetch the specific user's statuses
            updateStatusRecyclerView(userId)
        }
        statusGroupRecyclerView.layoutManager = LinearLayoutManager(context)
        statusGroupRecyclerView.adapter = statusGroupAdapter
        statusGroupAdapter.startListening()
    }
    private fun updateStatusRecyclerView(userId: String) {
        val query = FirebaseFirestore.getInstance()
            .collection("status")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<StatusModel>()
            .setQuery(query, StatusModel::class.java)
            .build()

        // Update the adapter with new query options for all statuses
        adapter?.updateOptions(options)

        // Now, add the query for the selected user's statuses
        val userQuery = FirebaseFirestore.getInstance()
            .collection("status")
            .whereEqualTo("userId", userId)  // Filter by the selected user's ID
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val userOptions = FirestoreRecyclerOptions.Builder<StatusModel>()
            .setQuery(userQuery, StatusModel::class.java)
            .build()

        // Update the adapter to reflect the user's statuses
        adapter?.updateOptions(userOptions)
    }



    private fun setupClickListeners() {
        uploadButton.setOnClickListener {
            val statusText = statusEditText.text.toString().trim()

            if (mediaUri != null && mediaType != null) {
                // If it's a video, check the duration before uploading
                if (mediaType == "video" && !isVideoDurationValid(mediaUri!!)) {
                    Toast.makeText(context, "Video must be 30 seconds or less", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                FirebaseUtil.uploadStatus(context, statusText, mediaUri!!, mediaType!!) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Status uploaded!", Toast.LENGTH_SHORT).show()
                        constraintStatus.isVisible = false
                        fab.isVisible = true
                    } else {
                        Toast.makeText(context, "Failed to upload status", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Please select an image or video", Toast.LENGTH_SHORT).show()
            }
        }

        fab.setOnClickListener {
            // Create an options dialog for selecting image or video
            constraintStatus.isVisible = true
            fab.isVisible = false
            val options = arrayOf("Select Image", "Select Video")
            val builder = AlertDialog.Builder(requireContext())
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

        backStatus.setOnClickListener {
            constraintStatus.isVisible = false
            fab.isVisible = true
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
        val oneDayAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        val query = FirebaseFirestore.getInstance()
            .collection("status")
            .whereGreaterThan("timestamp", Timestamp(oneDayAgo)) // Filter out old statuses
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<StatusModel>()
            .setQuery(query, StatusModel::class.java)
            .build()

        adapter = StatusRecyclerAdapter(options)
        recyclerView.layoutManager = LinearLayoutManager(context)
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
        retriever.setDataSource(context, videoUri)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationInMs = durationStr?.toLongOrNull() ?: 0
        retriever.release()
        return durationInMs <= 30000  // 30 seconds in milliseconds
    }
}
