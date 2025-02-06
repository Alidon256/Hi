package com.example.heyyou.ui.Fragments

import android.os.Bundle
import android.view.View
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

    lateinit var searchInput: EditText
    lateinit var searchButton: ImageButton
    lateinit var backButton: ImageButton
    lateinit var recyclerView: RecyclerView

    var adapter: SearchUserRecyclerAdapter? = null  // Change to nullable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_user)

        searchInput = findViewById(R.id.seach_username_input)
        searchButton = findViewById(R.id.search_user_btn)
        backButton = findViewById(R.id.back_btn)
        recyclerView = findViewById(R.id.search_user_recycler_view)

        searchInput.requestFocus()

        backButton.setOnClickListener {
            finish()
        }

        searchButton.setOnClickListener(View.OnClickListener {
            val searchTerm = searchInput.text.toString().trim { it <= ' ' }.lowercase(Locale.getDefault())
            if (searchTerm.isEmpty() || searchTerm.length < 3) {
                searchInput.error = "Invalid Username"
                return@OnClickListener
            }
            setupSearchRecyclerView(searchTerm)
        })
    }

    private fun setupSearchRecyclerView(searchTerm: String) {
        // Adjust the query to use `whereArrayContains` for substring search
        val query = FirebaseUtil.allUserCollectionReference()
            .whereArrayContains("searchKeywords", searchTerm) // Use this to search in substrings

        val options = FirestoreRecyclerOptions.Builder<UserModel>()
            .setQuery(query, UserModel::class.java)
            .build()

        // Initialize the adapter only if it hasn't been initialized already
        if (adapter == null) {
            adapter = SearchUserRecyclerAdapter(options, applicationContext)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter
        }

        // Start listening after setting up the adapter
        adapter?.startListening()
    }

    override fun onStart() {
        super.onStart()
        // Only call startListening if the adapter is initialized
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        // Only stop listening if the adapter is initialized
        adapter?.stopListening()
    }

    override fun onResume() {
        super.onResume()
        // Only call startListening if the adapter is initialized
        adapter?.startListening()
    }
}
