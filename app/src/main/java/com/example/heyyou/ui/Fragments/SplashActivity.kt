package com.example.heyyou.ui.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.heyyou.R;
import com.example.heyyou.model.UserModel;
import com.example.heyyou.utils.AndroidUtil;
import com.example.heyyou.utils.FirebaseUtil;


public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Log.d(TAG, "SplashActivity started");

        if (getIntent().getExtras() != null) {
            String userId = getIntent().getExtras().getString("userId");

            if (userId != null && !userId.isEmpty()) {
                Log.d(TAG, "Received userId: " + userId);

                FirebaseUtil.allUserCollectionReference().document(userId).get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult().exists()) {
                                UserModel model = task.getResult().toObject(UserModel.class);

                                Log.d(TAG, "User found, launching MainActivity and ChatActivity");

                                Intent mainIntent = new Intent(this, MainActivity.class);
                                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivity(mainIntent);

                                Intent intent = new Intent(this, ChatActivity.class);
                                AndroidUtil.passUserModelAsIntent(intent, model);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);

                                finish();
                            } else {
                                Log.e(TAG, "User document not found!");
                                goToMainOrLogin();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error fetching user", e);
                            goToMainOrLogin();
                        });

            } else {
                Log.e(TAG, "userId is null or empty!");
                goToMainOrLogin();
            }

        } else {
            goToMainOrLogin();
        }
    }

    private void goToMainOrLogin() {
        new Handler().postDelayed(() -> {
            if (FirebaseUtil.isLoggedIn()) {
                Log.d(TAG, "User is logged in, launching MainActivity");
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                Log.d(TAG, "User is not logged in, launching LoginPhoneNumberActivity");
                startActivity(new Intent(SplashActivity.this, LoginPhoneNumberActivity.class));
            }
            finish();
        }, 1000);
    }
}
