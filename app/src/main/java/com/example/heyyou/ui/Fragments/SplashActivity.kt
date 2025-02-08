package com.example.heyyou.ui.Fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.heyyou.R
import com.example.heyyou.model.UserModel
import com.example.heyyou.utils.AndroidUtil.passUserModelAsIntent
import com.example.heyyou.utils.FirebaseUtil
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Log.d(TAG, "SplashActivity started")

        if (intent.extras != null) {
            val userId = intent.extras!!.getString("userId")

            if (!userId.isNullOrEmpty()) {
                Log.d(TAG, "Received userId: $userId")

                FirebaseUtil.allUserCollectionReference().document(userId).get()
                    .addOnCompleteListener { task: Task<DocumentSnapshot> ->
                        if (task.isSuccessful && task.result.exists()) {
                            val model = task.result.toObject(UserModel::class.java)

                            Log.d(
                                TAG,
                                "User found, launching MainActivity and ChatActivity"
                            )

                            val mainIntent = Intent(this, MainActivity::class.java)
                            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                            startActivity(mainIntent)

                            val intent = Intent(this,ChatActivity::class.java)
                            passUserModelAsIntent(intent, model!!)
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)

                            finish()
                        } else {
                            Log.e(TAG, "User document not found!")
                            goToMainOrLogin()
                        }
                    }
                    .addOnFailureListener { e: Exception? ->
                        Log.e(TAG, "Error fetching user", e)
                        goToMainOrLogin()
                    }
            } else {
                Log.e(TAG, "userId is null or empty!")
                goToMainOrLogin()
            }
        } else {
            goToMainOrLogin()
        }
    }

    private fun goToMainOrLogin() {
        Handler().postDelayed({
            if (FirebaseUtil.isLoggedIn()) {
                Log.d(TAG, "User is logged in, launching MainActivity")
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                Log.d(TAG, "User is not logged in, launching LoginPhoneNumberActivity")
                startActivity(
                    Intent(
                        this@SplashActivity,
                        LoginPhoneNumberActivity::class.java
                    )
                )
            }
            finish()
        }, 1000)
    }

    companion object {
        private const val TAG = "SplashActivity"
    }
}
