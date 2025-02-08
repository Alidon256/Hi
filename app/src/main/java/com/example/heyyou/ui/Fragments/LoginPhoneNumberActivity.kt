package com.example.heyyou.ui.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.heyyou.R
import com.hbb20.CountryCodePicker

class LoginPhoneNumberActivity : AppCompatActivity() {
    private lateinit var countryCodePicker: CountryCodePicker
    private lateinit var phoneInput: EditText
    private lateinit var sendOtpBtn: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_phone_number)

        countryCodePicker = findViewById(R.id.login_countrycode)
        phoneInput = findViewById(R.id.login_mobile_number)
        sendOtpBtn = findViewById(R.id.send_otp_btn)
        progressBar = findViewById(R.id.login_progress_bar)

        progressBar.visibility = View.GONE

        countryCodePicker.registerCarrierNumberEditText(phoneInput)
        sendOtpBtn.setOnClickListener {
            if (!countryCodePicker.isValidFullNumber) {
                phoneInput.error = "Phone number not valid"
                return@setOnClickListener
            }
            val intent = Intent(
                this@LoginPhoneNumberActivity,
                LoginOtpActivity::class.java
            )
            intent.putExtra("phone", countryCodePicker.fullNumberWithPlus)
            startActivity(intent)
        }
    }
}