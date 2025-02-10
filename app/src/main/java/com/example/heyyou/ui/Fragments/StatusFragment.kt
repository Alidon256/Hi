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
import com.google.firebase.firestore.ListenerRegistration
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
    private var statusListener: ListenerRegistration? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val PICK_VIDEO_REQUEST = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        setupStatusGroupRecyclerView() // NEW: Initialize Status Group View
        setupRecyclerViewScrollListener()

        statusVideoView.apply {
            setOnCompletionListener { start() }
            setOnClickListener {
                if (isPlaying) pause() else start()
            }
        }

        return view
    }
    private fun setupRecyclerViewScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                playMostVisibleVideo()
            }
        })
    }

    private fun playMostVisibleVideo() {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

        var mostVisibleViewHolder: StatusRecyclerAdapter.StatusViewHolder? = null
        var maxVisibleArea = 0

        for (i in firstVisiblePosition..lastVisiblePosition) {
            val holder = recyclerView.findViewHolderForAdapterPosition(i) as? StatusRecyclerAdapter.StatusViewHolder
            if (holder != null && holder.statusVideo.visibility == View.VISIBLE) {
                val location = IntArray(2)
                holder.statusVideo.getLocationOnScreen(location)
                val visibleHeight = recyclerView.height - location[1]

                if (visibleHeight > maxVisibleArea) {
                    maxVisibleArea = visibleHeight
                    mostVisibleViewHolder = holder
                }
            }
        }

        mostVisibleViewHolder?.statusVideo?.start()
    }


    private fun setupStatusGroupRecyclerView() {
        statusGroupRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        statusGroupAdapter = StatusGroupAdapter(emptyList()) { userId ->
            updateStatusRecyclerView(userId)
        }
        statusGroupRecyclerView.adapter = statusGroupAdapter
    }

    private fun fetchStatusUpdates() {
        val oneDayAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        val query = FirebaseFirestore.getInstance()
            .collection("status")
            .whereGreaterThan("timestamp", Timestamp(oneDayAgo))
            .orderBy("timestamp", Query.Direction.DESCENDING)

        statusListener?.remove() // Remove old listener before setting a new one

        statusListener = query.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val latestStatuses = hashMapOf<String, StatusModel>()

                for (document in snapshot.documents) {
                    val status = document.toObject(StatusModel::class.java)
                    if (status != null) {
                        latestStatuses[status.userId] = status // Keep only the latest status per user
                    }
                }

                val uniqueStatusList = latestStatuses.values.toList()
                statusGroupAdapter.updateStatusList(uniqueStatusList)
            }
        }
    }

    private fun updateStatusRecyclerView(userId: String) {
        val query = FirebaseFirestore.getInstance()
            .collection("status")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<StatusModel>()
            .setQuery(query, StatusModel::class.java)
            .build()

        adapter?.updateOptions(options)
    }

    private fun setupClickListeners() {
        uploadButton.setOnClickListener {
            val statusText = statusEditText.text.toString().trim()

            if (mediaUri != null && mediaType != null) {
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
            constraintStatus.isVisible = true
            fab.isVisible = false
            val options = arrayOf("Select Image", "Select Video")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Media")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> pickMedia(PICK_IMAGE_REQUEST, "image/*")
                        1 -> pickMedia(PICK_VIDEO_REQUEST, "video/*")
                    }
                }
                .show()
        }

        backStatus.setOnClickListener {
            constraintStatus.isVisible = false
            fab.isVisible = true
            statusVideoView.pause()
        }
    }

    private fun pickMedia(requestCode: Int, type: String) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = type
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            mediaUri = data.data!!
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    mediaType = "image"
                    statusImageView.setImageURI(mediaUri)
                    statusImageView.visibility = View.VISIBLE
                    statusVideoView.visibility = View.GONE
                }
                PICK_VIDEO_REQUEST -> {
                    mediaType = "video"
                    statusVideoView.setVideoURI(mediaUri)
                    statusVideoView.visibility = View.VISIBLE
                    statusImageView.visibility = View.GONE
                    statusVideoView.start()
                }
            }
        }
    }

    private fun setupStatusRecyclerView() {
        val oneDayAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        val query = FirebaseFirestore.getInstance()
            .collection("status")
            .whereGreaterThan("timestamp", Timestamp(oneDayAgo))
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<StatusModel>()
            .setQuery(query, StatusModel::class.java)
            .build()

        adapter = StatusRecyclerAdapter(options)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        adapter?.startListening()
        fetchStatusUpdates()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        statusVideoView.stopPlayback()  // Stop video playback when fragment is destroyed
    }

    private fun isVideoDurationValid(videoUri: Uri): Boolean {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val durationInMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
        retriever.release()
        return durationInMs <= 30000
    }
}
