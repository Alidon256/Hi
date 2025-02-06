package com.example.heyyou.ui.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.heyyou.R
import com.example.heyyou.utils.FirebaseUtil
import com.example.heyyou.utils.FirebaseUtil.currentUserDetails
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    var searchButton: ImageButton? = null
    private lateinit var fab: ExtendedFloatingActionButton
    private var chatFragment: ChatFragment? = null
    private var profileFragment: ProfileFragment? = null
    private var usersFragment: UsersFragment? = null
    private var statusFragment: StatusFragment?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chatFragment = ChatFragment()
        profileFragment = ProfileFragment()
        usersFragment = UsersFragment()
        statusFragment = StatusFragment()



        bottomNavigationView = findViewById(R.id.bottom_navigation)
        searchButton = findViewById(R.id.main_search_btn)
        fab = findViewById(R.id.add)


        fab.setOnClickListener {
            // Show the UsersFragment when FAB is clicked
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_frame_layout, usersFragment!!)
                .addToBackStack(null) // Add the transaction to back stack
                .commit()
            fab.visibility = View.GONE
        }

        searchButton!!.setOnClickListener {
            startActivity(Intent(this@MainActivity, SearchUserActivity::class.java))
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.menu_chat) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_frame_layout, chatFragment!!).commit()
                fab.visibility = View.VISIBLE
            }
            if (item.itemId == R.id.menu_profile) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_frame_layout, profileFragment!!).commit()
                fab.visibility = View.GONE
            }
            if (item.itemId == R.id.Memories) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_frame_layout, statusFragment!!).commit()
                fab.visibility = View.GONE
            }
            true
        }
        bottomNavigationView.setSelectedItemId(R.id.menu_chat)

        getFCMToken()
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener { task: Task<String?>? ->
                if (task!!.isSuccessful) {
                    val token = task.result
                    currentUserDetails().update("fcmToken", token)
                }
            }
    }
    override fun onResume() {
        super.onResume()
        FirebaseUtil.setUserOnlineStatus(true) // User is online
    }

    override fun onPause() {
        super.onPause()
        FirebaseUtil.setUserOnlineStatus(false) // User is offline
    }

}
