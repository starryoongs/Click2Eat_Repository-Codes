package com.example.click2eat.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordUtils {

    private const val ITERATIONS = 600_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val ALGORITHM =
        "PBKDF2WithHmacSHA256"

    fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)

        val hash = deriveKey(
            password,
            salt,
            ITERATIONS
        )

        return listOf(
            "pbkdf2_sha256",
            ITERATIONS.toString(),
            Base64.encodeToString(
                salt,
                Base64.NO_WRAP
            ),
            Base64.encodeToString(
                hash,
                Base64.NO_WRAP
            )
        ).joinToString(":")
    }

    fun verifyPassword(
        password: String,
        storedHash: String
    ): Boolean {
        return try {
            val parts = storedHash.split(":")

            if (parts.size != 4 ||
                parts[0] != "pbkdf2_sha256"
            ) return false

            val iterations = parts[1].toInt()

            if (iterations < 100_000 ||
                iterations > 1_000_000
            ) return false

            val salt = Base64.decode(
                parts[2],
                Base64.NO_WRAP
            )

            val expected = Base64.decode(
                parts[3],
                Base64.NO_WRAP
            )

            if (salt.size != SALT_LENGTH ||
                expected.size != KEY_LENGTH / 8
            ) return false

            val actual = deriveKey(
                password,
                salt,
                iterations
            )

            MessageDigest.isEqual(expected, actual)

        } catch (e: Exception) {
            false
        }
    }

    private fun deriveKey(
        password: String,
        salt: ByteArray,
        iterations: Int
    ): ByteArray {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            iterations,
            KEY_LENGTH
        )

        return try {
            SecretKeyFactory
                .getInstance(ALGORITHM)
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }
}
