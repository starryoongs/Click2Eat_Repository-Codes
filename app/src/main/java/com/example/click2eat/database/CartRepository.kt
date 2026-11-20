
package com.example.click2eat.database

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.click2eat.models.CartItem

class CartRepository(context: Context) {

    private val databaseHelper = DatabaseHelper(context)

    private fun getDatabase(): SQLiteDatabase {
        return databaseHelper.writableDatabase
    }

    // Add food item to cart
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

            // Check if item already exists in the cart
            val cursor = db.rawQuery(
                """
                SELECT quantity FROM cart_items
                WHERE user_id = ? AND menu_item_id = ?
                """.trimIndent(),
                arrayOf(
                    userId.toString(),
                    menuItemId.toString()
                )
            )

            val existingQuantity = cursor.use {
                if (it.moveToFirst()) {
                    it.getInt(0)
                } else {
                    null
                }
            }

            if (existingQuantity != null) {

                // Increase quantity instead of duplicating item
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

                // Insert new cart item
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

    // Retrieve all cart items for logged-in customer
    fun getCartItems(userId: Int): List<CartItem> {

        val cartItems = mutableListOf<CartItem>()

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
                m.description,
                m.price,
                c.quantity,
                m.image,
                r.image AS restaurant_image
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

                val cartItem = CartItem(
                    id = it.getInt(0),
                    userId = it.getInt(1),
                    menuItemId = it.getInt(2),
                    restaurantId = it.getInt(3),
                    restaurantName = it.getString(4),
                    foodName = it.getString(5),
                    foodDescription =
                        if (it.isNull(6)) null else it.getString(6),
                    price = it.getInt(7),
                    quantity = it.getInt(8),
                    image =
                        if (it.isNull(9)) null else it.getString(9),
                    restaurantImage =
                        if (it.isNull(10)) null else it.getString(10)
                )

                cartItems.add(cartItem)
            }
        }

        return cartItems
    }

    // Update food quantity
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

            Log.e(
                "CartRepository",
                "Failed to update quantity",
                e
            )

            false
        }
    }

    // Remove one food item from cart
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

    // Retrieve items from one restaurant only
    fun getRestaurantCartItems(
        userId: Int,
        restaurantId: Int
    ): List<CartItem> {

        if (userId <= 0 || restaurantId <= 0) {
            return emptyList()
        }

        return getCartItems(userId).filter {
            it.restaurantId == restaurantId
        }
    }

    // Clear all cart items from selected restaurant
    fun clearRestaurantCart(
        userId: Int,
        restaurantId: Int
    ): Boolean {

        if (userId <= 0 || restaurantId <= 0) {
            return false
        }

        return try {

            val db = getDatabase()

            val rowsDeleted = db.delete(
                "cart_items",
                """
                user_id = ? AND menu_item_id IN (
                    SELECT id FROM menu_items
                    WHERE restaurant_id = ?
                )
                """.trimIndent(),
                arrayOf(
                    userId.toString(),
                    restaurantId.toString()
                )
            )

            rowsDeleted > 0

        } catch (e: Exception) {

            Log.e(
                "CartRepository",
                "Failed to clear restaurant cart",
                e
            )

            false
        }
    }
}
