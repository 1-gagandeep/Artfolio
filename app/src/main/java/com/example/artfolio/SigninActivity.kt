package com.example.artfolio

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SigninActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        dbHelper = DatabaseHelper(this)

        val etEmail = findViewById<EditText>(R.id.editTextEmail)
        val etPassword = findViewById<EditText>(R.id.editTextPassword)
        val btnSignin = findViewById<Button>(R.id.buttonSignIn)
        val btnSignup = findViewById<Button>(R.id.buttonSignUp)

        btnSignup.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        btnSignin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            val (isValid, username, profileImagePath) = dbHelper.checkUser(email, password)
            if (isValid) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PROFILE_IMAGE_PATH", profileImagePath)
                intent.putExtra("EMAIL", email) // Pass email for MainActivity
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
            }
        }
    }
}