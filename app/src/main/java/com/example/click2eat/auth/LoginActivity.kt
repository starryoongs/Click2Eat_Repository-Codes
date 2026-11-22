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
import com.example.click2eat.home.HomeActivity
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {

    private lateinit var repository: UserRepository
    private lateinit var sessionManager: SessionManager
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            openHome()
            return
        }

        setContentView(R.layout.activity_login)

        repository = UserRepository(this)

        val identifier = findViewById<EditText>(R.id.etIdentifier)
        val password = findViewById<EditText>(R.id.etPassword)
        val login = findViewById<Button>(R.id.btnLogin)
        val register = findViewById<TextView>(R.id.tvRegister)

        login.setOnClickListener {
            val input = identifier.text.toString().trim()
            val pass = password.text.toString()

            if (input.isEmpty()) {
                identifier.error = "Username or email is required"
                return@setOnClickListener
            }

            if (pass.isEmpty()) {
                password.error = "Password is required"
                return@setOnClickListener
            }

            login.isEnabled = false

            executor.execute {
                try {
                    val userId = repository.authenticateUser(input, pass)

                    runOnUiThread {
                        login.isEnabled = true

                        if (userId != null) {
                            sessionManager.saveLoginSession(userId)
                            openHome()
                        } else {
                            Toast.makeText(
                                this,
                                "Invalid username/email or password",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        login.isEnabled = true

                        Toast.makeText(
                            this,
                            "Unable to log in. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        register.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }

    private fun openHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}