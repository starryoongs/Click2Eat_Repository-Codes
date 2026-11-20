package com.example.click2eat.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.click2eat.models.Order
import com.example.click2eat.models.OrderItem

class CheckoutRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    data class CheckoutSummary(
        val restaurantId: Int,
        val restaurantName: String,
        val deliveryTime: String,
        val items: List<OrderItem>,
        val totalCentavos: Int
    )

    sealed class PlaceOrderResult {
        data class Success(val orderId: Long, val restaurantName: String, val totalCentavos: Int) : PlaceOrderResult()
        data class Error(val message: String) : PlaceOrderResult()
    }

    /**
     * Loads cart items for a specific user & restaurant.
     * If restaurantId == -1 or no cart items exist yet (before Members 2 & 3 finish),
     * seeds temporary placeholder records into SQLite so checkout & transactions can be tested.
     */
    fun getCheckoutSummary(userId: Int, requestedRestaurantId: Int): CheckoutSummary? {
        val db = dbHelper.writableDatabase

        var targetRestaurantId = requestedRestaurantId
        if (targetRestaurantId <= 0) {
            targetRestaurantId = findFirstCartRestaurantId(db, userId)
                ?: seedTemporaryPlaceholderData(db, userId)
        }

        // Fetch restaurant details
        var restaurantName = "McDo - Fastfood"
        var deliveryTime = "20-30 mins"
        db.rawQuery(
            "SELECT name, delivery_time FROM restaurants WHERE id = ?",
            arrayOf(targetRestaurantId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                restaurantName = cursor.getString(0) ?: restaurantName
                deliveryTime = cursor.getString(1) ?: deliveryTime
            }
        }

        // Fetch current cart items joined with menu_items for live prices & availability
        val items = mutableListOf<OrderItem>()
        val query = """
            SELECT c.menu_item_id, m.name, m.description, m.price, c.quantity
            FROM cart_items c
            INNER JOIN menu_items m ON c.menu_item_id = m.id
            WHERE c.user_id = ? AND m.restaurant_id = ?
        """.trimIndent()

        db.rawQuery(query, arrayOf(userId.toString(), targetRestaurantId.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                val menuItemId = cursor.getInt(0)
                val name = cursor.getString(1)
                val desc = cursor.getString(2) ?: ""
                val priceCentavos = cursor.getInt(3)
                val qty = cursor.getInt(4)

                if (qty > 0) {
                    items.add(
                        OrderItem(
                            menuItemId = menuItemId,
                            foodName = name,
                            foodDescription = desc,
                            quantity = qty,
                            priceCentavos = priceCentavos
                        )
                    )
                }
            }
        }

        if (items.isEmpty()) return null

        val totalCentavos = items.sumOf { it.subtotalCentavos }
        return CheckoutSummary(
            restaurantId = targetRestaurantId,
            restaurantName = restaurantName,
            deliveryTime = deliveryTime,
            items = items,
            totalCentavos = totalCentavos
        )
    }

    /**
     * Executes Member 4's atomic checkout transaction:
     * 1. Revalidates non-empty cart, single restaurant, and item availability in SQLite
     * 2. Recalculates total from SQLite
     * 3. Inserts order ('Pending', Unix epoch ms) and order_items
     * 4. Deletes purchased cart_items for that restaurant
     */
    fun placeOrderAtomic(
        userId: Int,
        restaurantId: Int,
        deliveryAddress: String,
        contactNumber: String
    ): PlaceOrderResult {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // 1. Revalidate cart items, availability, and single-restaurant restriction
            val verifiedItems = mutableListOf<OrderItem>()
            var restaurantName = ""

            db.rawQuery(
                "SELECT name FROM restaurants WHERE id = ?",
                arrayOf(restaurantId.toString())
            ).use { rCursor ->
                if (rCursor.moveToFirst()) {
                    restaurantName = rCursor.getString(0)
                } else {
                    return PlaceOrderResult.Error("Selected restaurant no longer exists.")
                }
            }

            val cartQuery = """
                SELECT c.id, c.menu_item_id, m.name, m.price, m.available, m.restaurant_id, c.quantity
                FROM cart_items c
                INNER JOIN menu_items m ON c.menu_item_id = m.id
                WHERE c.user_id = ? AND m.restaurant_id = ?
            """.trimIndent()

            db.rawQuery(cartQuery, arrayOf(userId.toString(), restaurantId.toString())).use { cursor ->
                while (cursor.moveToNext()) {
                    val menuItemId = cursor.getInt(1)
                    val foodName = cursor.getString(2)
                    val priceCentavos = cursor.getInt(3)
                    val isAvailable = cursor.getInt(4) == 1
                    val itemRestaurantId = cursor.getInt(5)
                    val quantity = cursor.getInt(6)

                    if (itemRestaurantId != restaurantId) {
                        return PlaceOrderResult.Error("Cart contains items from multiple restaurants.")
                    }
                    if (!isAvailable) {
                        return PlaceOrderResult.Error("$foodName is currently unavailable.")
                    }
                    if (quantity <= 0) {
                        return PlaceOrderResult.Error("Invalid quantity found for $foodName.")
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

            // 2. Recalculate order total using current SQLite records
            val recalculatedTotalCentavos = verifiedItems.sumOf { it.subtotalCentavos }
            val timestampMillis = System.currentTimeMillis()

            // 3. Insert into orders table
            val orderValues = ContentValues().apply {
                put("user_id", userId)
                put("restaurant_id", restaurantId)
                put("total_amount", recalculatedTotalCentavos)
                put("delivery_address", deliveryAddress.trim())
                put("contact_number", contactNumber.trim())
                put("status", "Pending")
                put("created_at", timestampMillis)
            }

            val generatedOrderId = db.insertOrThrow("orders", null, orderValues)
            if (generatedOrderId == -1L) {
                return PlaceOrderResult.Error("Failed to generate order record.")
            }

            // 4. Insert each item into order_items
            for (item in verifiedItems) {
                val itemValues = ContentValues().apply {
                    put("order_id", generatedOrderId)
                    put("menu_item_id", item.menuItemId)
                    put("quantity", item.quantity)
                    put("price", item.priceCentavos)
                }
                val rowId = db.insertOrThrow("order_items", null, itemValues)
                if (rowId == -1L) {
                    return PlaceOrderResult.Error("Failed to save ordered item: ${item.foodName}")
                }
            }

            // 5. Clear purchased cart records ONLY for this user & restaurant
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
            SELECT o.id, o.user_id, o.restaurant_id, r.name, o.total_amount,
                   o.delivery_address, o.contact_number, o.status, o.created_at
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
                    restaurantName = cursor.getString(3) ?: "Restaurant",
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

    private fun findFirstCartRestaurantId(db: SQLiteDatabase, userId: Int): Int? {
        val sql = """
            SELECT m.restaurant_id
            FROM cart_items c
            INNER JOIN menu_items m ON c.menu_item_id = m.id
            WHERE c.user_id = ?
            LIMIT 1
        """.trimIndent()
        db.rawQuery(sql, arrayOf(userId.toString())).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getInt(0)
        }
        return null
    }

    /**
     * Temporary placeholder seeder used ONLY when Members 2 & 3 haven't populated data yet.
     * Safe to keep: once real restaurants & cart_items exist, this is never triggered.
     */
    private fun seedTemporaryPlaceholderData(db: SQLiteDatabase, userId: Int): Int {
        var restaurantId = -1
        db.rawQuery("SELECT id FROM restaurants LIMIT 1", null).use { c ->
            if (c.moveToFirst()) restaurantId = c.getInt(0)
        }

        if (restaurantId == -1) {
            val restValues = ContentValues().apply {
                put("name", "McDo - Fastfood")
                put("description", "Fast food burgers, fries, and chicken meals")
                put("delivery_time", "20-30 mins")
            }
            restaurantId = db.insert("restaurants", null, restValues).toInt()
        }

        val item1Id = db.insert("menu_items", null, ContentValues().apply {
            put("restaurant_id", restaurantId)
            put("name", "Big Mac")
            put("description", "A Classic: Two all-100% beef patties, lettuce, cheese, pickles, onions on a sesame seed bun.")
            put("price", 19900) // ₱199.00 each -> Qty 2 = ₱398.00
            put("available", 1)
        }).toInt()

        val item2Id = db.insert("menu_items", null, ContentValues().apply {
            put("restaurant_id", restaurantId)
            put("name", "Chicken Sandwich")
            put("description", "Made of a juicy chicken patty topped with creamy mayo, sandwiched in a soft tasty bun.")
            put("price", 7500) // ₱75.00
            put("available", 1)
        }).toInt()

        if (item1Id != -1) {
            db.insert("cart_items", null, ContentValues().apply {
                put("user_id", userId)
                put("menu_item_id", item1Id)
                put("quantity", 2)
            })
        }
        if (item2Id != -1) {
            db.insert("cart_items", null, ContentValues().apply {
                put("user_id", userId)
                put("menu_item_id", item2Id)
                put("quantity", 1)
            })
        }

        return restaurantId
    }
}