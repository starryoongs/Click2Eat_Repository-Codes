package com.example.click2eat.database

import android.content.ContentValues
import android.content.Context
import com.example.click2eat.models.Order
import com.example.click2eat.models.OrderItem

class CheckoutRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    data class CheckoutSummary(
        val restaurantId: Int,
        val restaurantName: String,
        val restaurantImage: String,
        val deliveryTime: String,
        val items: List<OrderItem>,
        val totalCentavos: Int
    )

    sealed class PlaceOrderResult {
        data class Success(
            val orderId: Long,
            val restaurantName: String,
            val totalCentavos: Int
        ) : PlaceOrderResult()

        data class Error(val message: String) : PlaceOrderResult()
    }

    /**
     * Loads the selected restaurant's cart items for the logged-in user directly from SQLite.
     */
    fun getCheckoutSummary(userId: Int, restaurantId: Int): CheckoutSummary? {
        if (userId <= 0 || restaurantId <= 0) return null

        val db = dbHelper.readableDatabase

        var restaurantName = ""
        var restaurantImage = ""
        var deliveryTime = "20-30 mins"

        db.rawQuery(
            "SELECT name, image, delivery_time FROM restaurants WHERE id = ?",
            arrayOf(restaurantId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                restaurantName = cursor.getString(0) ?: ""
                restaurantImage = cursor.getString(1) ?: ""
                deliveryTime = cursor.getString(2) ?: "20-30 mins"
            } else {
                return null
            }
        }

        val items = mutableListOf<OrderItem>()
        val query = """
            SELECT c.menu_item_id, m.name, m.description, m.image, m.price, c.quantity
            FROM cart_items c
            INNER JOIN menu_items m ON c.menu_item_id = m.id
            WHERE c.user_id = ? AND m.restaurant_id = ?
        """.trimIndent()

        db.rawQuery(query, arrayOf(userId.toString(), restaurantId.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                val menuItemId = cursor.getInt(0)
                val name = cursor.getString(1) ?: ""
                val desc = cursor.getString(2) ?: ""
                val image = cursor.getString(3) ?: ""
                val priceCentavos = cursor.getInt(4)
                val qty = cursor.getInt(5)

                if (qty > 0) {
                    items.add(
                        OrderItem(
                            menuItemId = menuItemId,
                            foodName = name,
                            foodDescription = desc,
                            imageResName = image,
                            quantity = qty,
                            priceCentavos = priceCentavos
                        )
                    )
                }
            }
        }

        if (items.isEmpty()) return null

        return CheckoutSummary(
            restaurantId = restaurantId,
            restaurantName = restaurantName,
            restaurantImage = restaurantImage,
            deliveryTime = deliveryTime,
            items = items,
            totalCentavos = items.sumOf { it.subtotalCentavos }
        )
    }

    /**
     * Atomic SQLite transaction for Member 4:
     * 1. Revalidates restaurant, non-empty cart, single-restaurant rule, and item availability
     * 2. Recalculates total in centavos from SQLite
     * 3. Inserts into orders (total_price, order_date, 'Pending') and order_items
     * 4. Deletes purchased cart_items ONLY for this user and restaurant
     */
    fun placeOrderAtomic(
        userId: Int,
        restaurantId: Int,
        deliveryAddress: String,
        contactNumber: String
    ): PlaceOrderResult {
        if (userId <= 0) {
            return PlaceOrderResult.Error("Please log in before placing an order.")
        }
        if (restaurantId <= 0) {
            return PlaceOrderResult.Error("Invalid restaurant selected.")
        }

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            var restaurantName = ""
            db.rawQuery(
                "SELECT name FROM restaurants WHERE id = ?",
                arrayOf(restaurantId.toString())
            ).use { rCursor ->
                if (rCursor.moveToFirst()) {
                    restaurantName = rCursor.getString(0) ?: ""
                } else {
                    return PlaceOrderResult.Error("Selected restaurant no longer exists.")
                }
            }

            val verifiedItems = mutableListOf<OrderItem>()
            val cartQuery = """
                SELECT c.menu_item_id, m.name, m.price, m.available, m.restaurant_id, c.quantity
                FROM cart_items c
                INNER JOIN menu_items m ON c.menu_item_id = m.id
                WHERE c.user_id = ? AND m.restaurant_id = ?
            """.trimIndent()

            db.rawQuery(cartQuery, arrayOf(userId.toString(), restaurantId.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    val menuItemId = cursor.getInt(0)
                    val foodName = cursor.getString(1) ?: ""
                    val priceCentavos = cursor.getInt(2)
                    val isAvailable = cursor.getInt(3) == 1
                    val itemRestaurantId = cursor.getInt(4)
                    val quantity = cursor.getInt(5)

                    if (itemRestaurantId != restaurantId) {
                        return PlaceOrderResult.Error("Cart items must belong to a single restaurant.")
                    }
                    if (!isAvailable) {
                        return PlaceOrderResult.Error("$foodName is no longer available.")
                    }
                    if (quantity <= 0) {
                        return PlaceOrderResult.Error("Invalid quantity for $foodName.")
                    }

                    verifiedItems.add(
                        OrderItem(
                            menuItemId = menuItemId,
                            foodName = foodName,
                            quantity = quantity,
                            priceCentavos = priceCentavos
                        )
                    )
                }
            }

            if (verifiedItems.isEmpty()) {
                return PlaceOrderResult.Error("Your cart is empty.")
            }

            val recalculatedTotalCentavos = verifiedItems.sumOf { it.subtotalCentavos }
            val timestampMillis = System.currentTimeMillis()

            val orderValues = ContentValues().apply {
                put("user_id", userId)
                put("restaurant_id", restaurantId)
                put("total_price", recalculatedTotalCentavos)
                put("delivery_address", deliveryAddress.trim())
                put("contact_number", contactNumber.trim())
                put("status", "Pending")
                put("order_date", timestampMillis)
            }

            val generatedOrderId = db.insertOrThrow("orders", null, orderValues)
            if (generatedOrderId == -1L) {
                return PlaceOrderResult.Error("Failed to create order.")
            }

            for (item in verifiedItems) {
                val itemValues = ContentValues().apply {
                    put("order_id", generatedOrderId)
                    put("menu_item_id", item.menuItemId)
                    put("quantity", item.quantity)
                    put("price", item.priceCentavos)
                }
                val rowId = db.insertOrThrow("order_items", null, itemValues)
                if (rowId == -1L) {
                    return PlaceOrderResult.Error("Failed to save item: ${item.foodName}")
                }
            }

            // Clear only the purchased restaurant's cart items for this customer
            val deleteSql = """
                DELETE FROM cart_items
                WHERE user_id = ? AND menu_item_id IN (
                    SELECT id FROM menu_items WHERE restaurant_id = ?
                )
            """.trimIndent()
            db.execSQL(deleteSql, arrayOf(userId, restaurantId))

            db.setTransactionSuccessful()
            return PlaceOrderResult.Success(
                orderId = generatedOrderId,
                restaurantName = restaurantName,
                totalCentavos = recalculatedTotalCentavos
            )
        } catch (e: Exception) {
            return PlaceOrderResult.Error("Checkout failed: ${e.message ?: "Database error"}")
        } finally {
            db.endTransaction()
        }
    }

    fun getOrderById(orderId: Long): Order? {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT o.id, o.user_id, o.restaurant_id, r.name, o.total_price,
                   o.delivery_address, o.contact_number, o.status, o.order_date
            FROM orders o
            LEFT JOIN restaurants r ON o.restaurant_id = r.id
            WHERE o.id = ?
        """.trimIndent()

        db.rawQuery(query, arrayOf(orderId.toString())).use { cursor ->
            if (cursor.moveToFirst()) {
                return Order(
                    id = cursor.getLong(0),
                    userId = cursor.getInt(1),
                    restaurantId = cursor.getInt(2),
                    restaurantName = cursor.getString(3) ?: "",
                    totalAmountCentavos = cursor.getInt(4),
                    deliveryAddress = cursor.getString(5) ?: "",
                    contactNumber = cursor.getString(6) ?: "",
                    status = cursor.getString(7) ?: "Pending",
                    createdAt = cursor.getLong(8)
                )
            }
        }
        return null
    }
}