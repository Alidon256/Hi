package com.example.heyyou

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.heyyou.utils.FirebaseUtil
import com.example.heyyou.utils.FirebaseUtil.currentUserDetails
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    var bottomNavigationView: BottomNavigationView? = null
    var searchButton: ImageButton? = null

    var chatFragment: ChatFragment? = null
    var profileFragment: ProfileFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chatFragment = ChatFragment()
        profileFragment = ProfileFragment()

        bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        searchButton = findViewById<ImageButton>(R.id.main_search_btn)

        searchButton!!.setOnClickListener(View.OnClickListener { v: View? ->
            startActivity(Intent(this@MainActivity, SearchUserActivity::class.java))
        })

        bottomNavigationView!!.setOnItemSelectedListener(object :
            NavigationBarView.OnItemSelectedListener {
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                if (item.itemId == R.id.menu_chat) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_frame_layout, chatFragment!!).commit()
                }
                if (item.itemId == R.id.menu_profile) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.main_frame_layout, profileFragment!!).commit()
                }
                return true
            }
        })
        bottomNavigationView!!.setSelectedItemId(R.id.menu_chat)

        getFCMToken()
    }

    fun getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(OnCompleteListener { task: Task<String?>? ->
                if (task!!.isSuccessful) {
                    val token = task.getResult()
                    currentUserDetails().update("fcmToken", token)
                }
            })
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













