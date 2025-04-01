//package com.example.artfolio
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Button
//import android.widget.EditText
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.example.artfolio.DatabaseHelper
//import com.example.artfolio.MainActivity
//import com.example.artfolio.R
//import com.example.artfolio.SigninActivity
//
//
//class SignupActivity : AppCompatActivity() {
//    private lateinit var dbHelper: DatabaseHelper
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_signup)
//
//        dbHelper = DatabaseHelper(this)
//
//        val etFirstName = findViewById<EditText>(R.id.etFirstName)
//        val etLastName = findViewById<EditText>(R.id.etLastName)
//        val etEmail = findViewById<EditText>(R.id.etEmail)
//        val etPassword = findViewById<EditText>(R.id.etPassword)
//        val btnSignup = findViewById<Button>(R.id.btnSignup)
//
//        btnSignup.setOnClickListener {
//            val firstname = etFirstName.text.toString().trim()
//            val lastname = etLastName.text.toString().trim()
//            val email = etEmail.text.toString().trim()
//            val password = etPassword.text.toString().trim()
//
//            if (firstname.isEmpty() || lastname.isEmpty() || email.isEmpty() || password.isEmpty()) {
//                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show()
//            } else {
//                val success = dbHelper.insertUser(firstname, lastname, email, password)
//                if (success) {
//                    Toast.makeText(this, "Signup Successful!", Toast.LENGTH_SHORT).show()
//                    startActivity(Intent(this, SigninActivity::class.java))
//                    finish()
//                } else {
//                    Toast.makeText(this, "Email already exists!", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//}

// SignupActivity.kt
package com.example.artfolio

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.artfolio.DatabaseHelper

import com.example.artfolio.R
import com.example.artfolio.SigninActivity

class SignupActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        dbHelper = DatabaseHelper(this)

        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSignup = findViewById<Button>(R.id.btnSignup)

        btnSignup.setOnClickListener {
            val firstname = etFirstName.text.toString().trim()
            val lastname = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (firstname.isEmpty() || lastname.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show()
            } else {
                val success = dbHelper.insertUser(firstname, lastname, email, password)
                if (success) {
                    Toast.makeText(this, "Signup Successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, SigninActivity::class.java)
                    intent.putExtra("USERNAME", "$firstname $lastname") // Pass username
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Email already exists!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}