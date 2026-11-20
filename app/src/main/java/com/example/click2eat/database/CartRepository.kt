package com.example.click2eat.database

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log

class CartRepository(context: Context) {

    private val databaseHelper = DatabaseHelper(context)

    private fun getDatabase(): SQLiteDatabase {
        return databaseHelper.writableDatabase
    }

    fun addToCart(
        userId: Int,
        menuItemId: Int,
        quantity: Int = 1
    ): Boolean {

        if (userId <= 0 || menuItemId <= 0 || quantity <= 0) {
            return false
        }

        val db = getDatabase()
        db.beginTransaction()

        try {
            // Check if the food item exists and is available
            val foodCursor = db.rawQuery(
                """
                SELECT id FROM menu_items
                WHERE id = ? AND available = 1
                """.trimIndent(),
                arrayOf(menuItemId.toString())
            )

            val foodAvailable = foodCursor.use {
                it.moveToFirst()
            }

            if (!foodAvailable) {
                return false
            }

            // Check if the item is already in the cart
            val cursor = db.rawQuery(
                """
                SELECT quantity FROM cart_items
                WHERE user_id = ? AND menu_item_id = ?
                """.trimIndent(),
                arrayOf(userId.toString(), menuItemId.toString())
            )

            val existingQuantity = cursor.use {
                if (it.moveToFirst()) {
                    it.getInt(0)
                } else {
                    null
                }
            }

            if (existingQuantity != null) {

                // Increase quantity instead of creating a duplicate
                val newQuantity =
                    existingQuantity.toLong() + quantity

                if (newQuantity > Int.MAX_VALUE) {
                    return false
                }

                val values = ContentValues().apply {
                    put("quantity", newQuantity.toInt())
                }

                val rowsUpdated = db.update(
                    "cart_items",
                    values,
                    "user_id = ? AND menu_item_id = ?",
                    arrayOf(
                        userId.toString(),
                        menuItemId.toString()
                    )
                )

                if (rowsUpdated != 1) {
                    return false
                }

            } else {

                // Add a new cart item
                val values = ContentValues().apply {
                    put("user_id", userId)
                    put("menu_item_id", menuItemId)
                    put("quantity", quantity)
                }

                val result = db.insert(
                    "cart_items",
                    null,
                    values
                )

                if (result == -1L) {
                    return false
                }
            }

            db.setTransactionSuccessful()
            return true

        } catch (e: Exception) {

            Log.e(
                "CartRepository",
                "Failed to add item",
                e
            )

            return false

        } finally {
            db.endTransaction()
        }
    }

    fun getCartItems(userId: Int): List<com.example.click2eat.models.CartItem> {

        val cartItems = mutableListOf<com.example.click2eat.models.CartItem>()

        if (userId <= 0) {
            return cartItems
        }

        val db = getDatabase()

        val cursor = db.rawQuery(
            """
        SELECT
            c.id,
            c.user_id,
            c.menu_item_id,
            m.restaurant_id,
            r.name AS restaurant_name,
            m.name AS food_name,
            m.price,
            c.quantity,
            m.image
        FROM cart_items c
        INNER JOIN menu_items m
            ON c.menu_item_id = m.id
        INNER JOIN restaurants r
            ON m.restaurant_id = r.id
        WHERE c.user_id = ?
        ORDER BY r.name, m.name
        """.trimIndent(),
            arrayOf(userId.toString())
        )

        cursor.use {
            while (it.moveToNext()) {

                val cartItem = com.example.click2eat.models.CartItem(
                    id = it.getInt(0),
                    userId = it.getInt(1),
                    menuItemId = it.getInt(2),
                    restaurantId = it.getInt(3),
                    restaurantName = it.getString(4),
                    foodName = it.getString(5),
                    price = it.getInt(6),
                    quantity = it.getInt(7),
                    image = if (it.isNull(8)) null else it.getString(8)
                )

                cartItems.add(cartItem)
            }
        }

        return cartItems
    }

    fun updateQuantity(
        userId: Int,
        cartItemId: Int,
        newQuantity: Int
    ): Boolean {

        if (userId <= 0 || cartItemId <= 0 || newQuantity <= 0) {
            return false
        }

        return try {
            val db = getDatabase()

            val values = ContentValues().apply {
                put("quantity", newQuantity)
            }

            val rowsUpdated = db.update(
                "cart_items",
                values,
                "id = ? AND user_id = ?",
                arrayOf(
                    cartItemId.toString(),
                    userId.toString()
                )
            )

            rowsUpdated == 1

        } catch (e: Exception) {
            Log.e("CartRepository", "Failed to update quantity", e)
            false
        }
    }

    fun removeCartItem(
        userId: Int,
        cartItemId: Int
    ): Boolean {

        if (userId <= 0 || cartItemId <= 0) {
            return false
        }

        return try {
            val db = getDatabase()

            val rowsDeleted = db.delete(
                "cart_items",
                "id = ? AND user_id = ?",
                arrayOf(
                    cartItemId.toString(),
                    userId.toString()
                )
            )

            rowsDeleted == 1

        } catch (e: Exception) {
            Log.e(
                "CartRepository",
                "Failed to remove cart item",
                e
            )
            false
        }
    }

    fun getRestaurantCartItems(
        userId: Int,
        restaurantId: Int
    ): List<com.example.click2eat.models.CartItem> {

        if (userId <= 0 || restaurantId <= 0) {
            return emptyList()
        }

        return getCartItems(userId).filter {
            it.restaurantId == restaurantId
        }
    }

}
