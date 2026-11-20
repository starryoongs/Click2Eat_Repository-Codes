package com.example.click2eat.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.example.click2eat.models.User
import com.example.click2eat.utils.PasswordUtils

class UserRepository(context: Context) {

    private val helper = DatabaseHelper(context)

    fun registerUser(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): Boolean {
        val hash = PasswordUtils.hashPassword(password)

        val values = ContentValues().apply {
            put("username", username.trim())
            put("first_name", firstName.trim())
            put("last_name", lastName.trim())
            put("email", email.trim().lowercase())
            put("password", hash)
        }

        return try {
            helper.writableDatabase.insertOrThrow(
                "users",
                null,
                values
            )
            true
        } catch (e: SQLiteConstraintException) {
            false
        }
    }

    fun authenticateUser(
        identifier: String,
        password: String
    ): Long? {

        val db = helper.readableDatabase

        db.query(
            "users",
            arrayOf("id", "password"),
            "username = ? OR email = ?",
            arrayOf(
                identifier.trim(),
                identifier.trim()
            ),
            null,
            null,
            null
        ).use { cursor ->

            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                val hash = cursor.getString(1)

                if (PasswordUtils.verifyPassword(
                        password,
                        hash
                    )
                ) {
                    return id
                }
            }
        }

        return null
    }

    fun getUserById(userId: Long): User? {

        helper.readableDatabase.query(
            "users",
            arrayOf(
                "id",
                "username",
                "first_name",
                "last_name",
                "email",
                "created_at",
                "profile_picture"
            ),
            "id = ?",
            arrayOf(userId.toString()),
            null,
            null,
            null
        ).use { cursor ->

            if (cursor.moveToFirst()) {
                return User(
                    id = cursor.getLong(0),
                    username = cursor.getString(1),
                    firstName = cursor.getString(2),
                    lastName = cursor.getString(3),
                    email = cursor.getString(4),
                    createdAt = cursor.getString(5),
                    profilePicture = cursor.getString(6)
                )
            }
        }

        return null
    }


    fun updateProfile(
        userId: Long,
        firstName: String,
        lastName: String,
        email: String,
        profilePicture: String?
    ): Boolean {

        val values = ContentValues().apply {
            put("first_name", firstName.trim())
            put("last_name", lastName.trim())
            put("email", email.trim().lowercase())

            if (profilePicture != null) {
                put("profile_picture", profilePicture)
            }
        }

        return try {
            helper.writableDatabase.update(
                "users",
                values,
                "id = ?",
                arrayOf(userId.toString())
            ) > 0
        } catch (e: SQLiteConstraintException) {
            false
        }
    }

}
