
package com.example.click2eat.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.example.click2eat.models.User

class UserRepository(context: Context) {

    private val databaseHelper = DatabaseHelper(context)

    fun registerUser(
        username: String,
        firstName: String,
        lastName: String,
        email: String,
        passwordHash: String
    ): Boolean {

        val db = databaseHelper.writableDatabase

        val values = ContentValues().apply {
            put("username", username)
            put("first_name", firstName)
            put("last_name", lastName)
            put("email", email)
            put("password", passwordHash)
        }

        return try {
            db.insertOrThrow("users", null, values)
            true
        } catch (e: SQLiteConstraintException) {
            false
        }
    }

    fun getUserById(userId: Long): User? {

        val db = databaseHelper.readableDatabase

        val cursor = db.query(
            "users",
            arrayOf(
                "id",
                "username",
                "first_name",
                "last_name",
                "email",
                "created_at"
            ),
            "id = ?",
            arrayOf(userId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return User(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    username = it.getString(
                        it.getColumnIndexOrThrow("username")
                    ),
                    firstName = it.getString(
                        it.getColumnIndexOrThrow("first_name")
                    ),
                    lastName = it.getString(
                        it.getColumnIndexOrThrow("last_name")
                    ),
                    email = it.getString(
                        it.getColumnIndexOrThrow("email")
                    ),
                    createdAt = it.getString(
                        it.getColumnIndexOrThrow("created_at")
                    )
                )
            }
        }

        return null
    }
}
