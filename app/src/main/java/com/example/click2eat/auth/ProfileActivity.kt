package com.example.click2eat.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.click2eat.R
import com.example.click2eat.database.UserRepository
import com.example.click2eat.home.HomeActivity
import com.example.click2eat.utils.ValidationUtils
import java.io.File
import java.util.concurrent.Executors

class ProfileActivity : AppCompatActivity() {

    private lateinit var repository: UserRepository
    private lateinit var session: SessionManager

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var username: TextView
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var email: EditText
    private lateinit var save: Button

    private lateinit var profileImage: ImageView
    private lateinit var selectedFileText: TextView

    private var userId = -1L

    private var selectedImageUri: Uri? = null
    private var currentProfilePicture: String? = null

    // IMAGE PICKER
    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->

        if (uri != null) {
            selectedImageUri = uri

            profileImage.setImageURI(uri)

            selectedFileText.text = getImageFileName(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)
        userId = session.getUserId()

        if (userId == -1L) {
            goToLogin()
            return
        }

        setContentView(R.layout.activity_profile)

        val backHome = findViewById<Button>(
            R.id.btnBackHome
        )

        backHome.setOnClickListener {
            val intent = Intent(
                this,
                HomeActivity::class.java
            ).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            startActivity(intent)
            finish()
        }

        repository = UserRepository(this)

        // INITIALIZE VIEWS
        username = findViewById(R.id.tvUsername)
        firstName = findViewById(R.id.etFirstName)
        lastName = findViewById(R.id.etLastName)
        email = findViewById(R.id.etEmail)
        save = findViewById(R.id.btnSaveProfile)

        profileImage = findViewById(R.id.ivProfilePicture)
        selectedFileText = findViewById(R.id.tvSelectedFile)

        val choosePicture =
            findViewById<Button>(R.id.btnChoosePicture)

        // CHOOSE PROFILE PICTURE
        choosePicture.setOnClickListener {
            imagePicker.launch("image/*")
        }

        // LOAD EXISTING PROFILE
        loadProfile()

        // SAVE PROFILE
        save.setOnClickListener {

            val f = firstName.text.toString().trim()
            val l = lastName.text.toString().trim()
            val e = email.text.toString().trim()

            when {
                f.isEmpty() -> {
                    firstName.error = "First name is required"
                }

                l.isEmpty() -> {
                    lastName.error = "Last name is required"
                }

                !ValidationUtils.isValidEmail(e) -> {
                    email.error = "Invalid email address"
                }

                else -> {
                    save.isEnabled = false

                    val imageUriToSave = selectedImageUri

                    executor.execute {
                        var newImageFile: String? = null

                        try {
                            // SAVE IMAGE TO PRIVATE STORAGE
                            if (imageUriToSave != null) {
                                newImageFile = saveProfileImage(
                                    imageUriToSave
                                )
                            }

                            // UPDATE SQLITE
                            val success = repository.updateProfile(
                                userId,
                                f,
                                l,
                                e,
                                newImageFile
                            )

                            if (!success && newImageFile != null) {
                                File(filesDir, newImageFile).delete()
                            }

                            runOnUiThread {
                                save.isEnabled = true

                                if (success) {

                                    if (newImageFile != null) {
                                        currentProfilePicture = newImageFile
                                        selectedImageUri = null
                                    }

                                    Toast.makeText(
                                        this,
                                        "Profile updated successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } else {
                                    Toast.makeText(
                                        this,
                                        "Email already used or update failed",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                        } catch (exception: Exception) {

                            if (newImageFile != null) {
                                File(filesDir, newImageFile).delete()
                            }

                            runOnUiThread {
                                save.isEnabled = true

                                Toast.makeText(
                                    this,
                                    "Unable to update profile",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    // LOAD PROFILE INFORMATION
    private fun loadProfile() {

        executor.execute {

            try {
                val user = repository.getUserById(userId)

                runOnUiThread {

                    if (user == null) {
                        session.logout()
                        goToLogin()
                    } else {

                        username.text = user.username
                        firstName.setText(user.firstName)
                        lastName.setText(user.lastName)
                        email.setText(user.email)

                        // LOAD SAVED PROFILE PICTURE
                        currentProfilePicture = user.profilePicture

                        if (!user.profilePicture.isNullOrEmpty()) {

                            val file = File(
                                filesDir,
                                user.profilePicture
                            )

                            if (file.exists()) {
                                profileImage.setImageURI(
                                    Uri.fromFile(file)
                                )

                                selectedFileText.text =
                                    user.profilePicture
                            }
                        }
                    }
                }

            } catch (exception: Exception) {

                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Unable to load profile",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // GET SELECTED IMAGE FILE NAME
    private fun getImageFileName(uri: Uri): String {

        var fileName = "Selected image"

        contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )?.use { cursor ->

            val index = cursor.getColumnIndex(
                OpenableColumns.DISPLAY_NAME
            )

            if (index >= 0 && cursor.moveToFirst()) {
                fileName = cursor.getString(index)
            }
        }

        return fileName
    }

    // SAVE PROFILE IMAGE
    private fun saveProfileImage(uri: Uri): String {

        val mimeType = contentResolver.getType(uri)

        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/jpeg" -> "jpg"
            else -> "img"
        }

        val fileName =
            "profile_${userId}_${System.currentTimeMillis()}.$extension"

        val file = File(filesDir, fileName)

        contentResolver.openInputStream(uri).use { input ->

            requireNotNull(input) {
                "Unable to open selected image"
            }

            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return fileName
    }

    // REDIRECT TO LOGIN
    private fun goToLogin() {

        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
