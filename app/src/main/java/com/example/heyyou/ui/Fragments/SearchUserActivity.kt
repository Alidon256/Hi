package com.example.heyyou.ui.Fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.heyyou.R
import com.example.heyyou.model.UserModel
import com.example.heyyou.ui.adapter.SearchUserRecyclerAdapter
import com.example.heyyou.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import java.util.Locale

class SearchUserActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var backButton: ImageButton
    private lateinit var recyclerView: RecyclerView
    private var adapter: SearchUserRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_user)

        searchInput = findViewById(R.id.seach_username_input)
        backButton = findViewById(R.id.back_btn)
        recyclerView = findViewById(R.id.search_user_recycler_view)

        searchInput.requestFocus()

        backButton.setOnClickListener { finish() }

        // Live search as user types
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchTerm = s.toString().trim()
                if (searchTerm.length >= 2) {
                    setupSearchRecyclerView(searchTerm)
                } else {
                    clearResults()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSearchRecyclerView(searchTerm: String) {
        val lowercaseSearchTerm = searchTerm.lowercase(Locale.getDefault())

        val query = FirebaseUtil.allUserCollectionReference()
            .whereArrayContains("searchKeywords", lowercaseSearchTerm) // Match any substring

        val options = FirestoreRecyclerOptions.Builder<UserModel>()
            .setQuery(query, UserModel::class.java)
            .setLifecycleOwner(this) // Automatically manages start/stop listening
            .build()

        if (adapter == null) {
            adapter = SearchUserRecyclerAdapter(options, applicationContext)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter
            adapter?.startListening()
        } else {
            adapter?.updateOptions(options) // Update results dynamically
        }
    }

    private fun clearResults() {
        adapter?.stopListening()
        adapter?.notifyDataSetChanged()
        recyclerView.adapter = null // Hide RecyclerView when no search
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }
}