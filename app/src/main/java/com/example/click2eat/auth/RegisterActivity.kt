package com.example.click2eat.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.click2eat.R
import com.example.click2eat.database.UserRepository
import com.example.click2eat.utils.ValidationUtils
import java.util.concurrent.Executors

class RegisterActivity : AppCompatActivity() {

    private lateinit var repository: UserRepository
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        repository = UserRepository(this)

        val username = findViewById<EditText>(R.id.etUsername)
        val firstName = findViewById<EditText>(R.id.etFirstName)
        val lastName = findViewById<EditText>(R.id.etLastName)
        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val confirm = findViewById<EditText>(
            R.id.etConfirmPassword
        )
        val register = findViewById<Button>(R.id.btnRegister)
        val login = findViewById<TextView>(R.id.tvLogin)

        register.setOnClickListener {
            val u = username.text.toString().trim()
            val f = firstName.text.toString().trim()
            val l = lastName.text.toString().trim()
            val e = email.text.toString().trim()
            val p = password.text.toString()
            val c = confirm.text.toString()

            when {
                !ValidationUtils.isValidUsername(u) ->
                    username.error = "Use 3-30 letters, numbers or underscores"

                !ValidationUtils.isNotEmpty(f) ->
                    firstName.error = "First name is required"

                !ValidationUtils.isNotEmpty(l) ->
                    lastName.error = "Last name is required"

                !ValidationUtils.isValidEmail(e) ->
                    email.error = "Invalid email address"

                !ValidationUtils.isValidPassword(p) ->
                    password.error = "Use 8+ characters with uppercase, lowercase and number"

                !ValidationUtils.passwordsMatch(p, c) ->
                    confirm.error = "Passwords do not match"

                else -> {
                    register.isEnabled = false

                    executor.execute {
                        try {
                            val success = repository.registerUser(
                                u, f, l, e, p
                            )

                            runOnUiThread {
                                register.isEnabled = true

                                if (success) {
                                    Toast.makeText(
                                        this,
                                        "Registration successful!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    startActivity(
                                        Intent(
                                            this,
                                            LoginActivity::class.java
                                        )
                                    )
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Username or email already exists",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                register.isEnabled = true
                                Toast.makeText(
                                    this,
                                    "Unable to register. Please try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
        }

        login.setOnClickListener {
            startActivity(
                Intent(this, LoginActivity::class.java)
            )
            finish()
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
