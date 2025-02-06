package com.example.heyyou.ui.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.heyyou.R
import com.example.heyyou.utils.AndroidUtil
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class LoginOtpActivity : AppCompatActivity() {
    var phoneNumber: String? = null
    var timeoutSeconds: Long = 60L
    var verificationCode: String? = null
    var resendingToken: ForceResendingToken? = null

    lateinit var otpInput: EditText
    lateinit var nextBtn: Button
    lateinit var progressBar: ProgressBar
    lateinit var resendOtpTextView: TextView
    var mAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_otp)

        otpInput = findViewById(R.id.login_otp)
        nextBtn = findViewById(R.id.login_next_btn)
        progressBar = findViewById(R.id.login_progress_bar)
        resendOtpTextView = findViewById(R.id.resend_otp_textview)

        phoneNumber = intent.extras!!.getString("phone")

        sendOtp(phoneNumber!!, false)

        nextBtn.setOnClickListener {
            val enteredOtp = otpInput.getText().toString()
            val credential = PhoneAuthProvider.getCredential(verificationCode!!, enteredOtp)
            signIn(credential)
        }

        resendOtpTextView.setOnClickListener {
            sendOtp(
                phoneNumber!!, true
            )
        }
    }

    private fun sendOtp(phoneNumber: String, isResend: Boolean) {
        startResendTimer()
        setInProgress(true)
        val builder =
            PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object : OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                        signIn(phoneAuthCredential)
                        setInProgress(false)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        AndroidUtil.showToast(applicationContext, "OTP verification failed")
                        setInProgress(false)
                    }

                    override fun onCodeSent(s: String, forceResendingToken: ForceResendingToken) {
                        super.onCodeSent(s, forceResendingToken)
                        verificationCode = s
                        resendingToken = forceResendingToken
                        AndroidUtil.showToast(applicationContext, "OTP sent successfully")
                        setInProgress(false)
                    }
                })
        if (isResend) {
            PhoneAuthProvider.verifyPhoneNumber(
                builder.setForceResendingToken(resendingToken!!).build()
            )
        } else {
            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        }
    }

    fun setInProgress(inProgress: Boolean) {
        if (inProgress) {
            progressBar.visibility = View.VISIBLE
            nextBtn.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
            nextBtn.visibility = View.VISIBLE
        }
    }

    fun signIn(phoneAuthCredential: PhoneAuthCredential) {
        //login and go to next activity
        setInProgress(true)
        mAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener { task ->
            setInProgress(false)
            if (task.isSuccessful) {
                val intent = Intent(
                    this@LoginOtpActivity,
                    LoginUsernameActivity::class.java
                )
                intent.putExtra("phone", phoneNumber)
                startActivity(intent)
            } else {
                AndroidUtil.showToast(applicationContext, "OTP verification failed")
            }
        }
    }

    private fun startResendTimer() {
        resendOtpTextView.isEnabled = false
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                timeoutSeconds--
                resendOtpTextView!!.text = "Resend OTP in $timeoutSeconds seconds"
                if (timeoutSeconds <= 0) {
                    timeoutSeconds = 60L
                    timer.cancel()
                    runOnUiThread {
                        resendOtpTextView!!.isEnabled = true
                    }
                }
            }
        }, 0, 1000)
    }
}













