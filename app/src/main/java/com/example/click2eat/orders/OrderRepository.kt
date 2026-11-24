package com.example.click2eat.orders

import android.content.ContentValues
import android.content.Context
import com.example.click2eat.database.DatabaseHelper
import com.example.click2eat.models.Order
import com.example.click2eat.models.OrderItem

class OrderRepository(context: Context) {
    data class OrderWithItems(val order: Order, val items: List<OrderItem>)

    private val helper = DatabaseHelper(context.applicationContext)

    fun getOrdersWithItems(userId: Long, statusFilter: String = FILTER_ALL): List<OrderWithItems> =
        getOrders(userId, statusFilter).map { order ->
            OrderWithItems(order, getItems(userId, order.id))
        }

    fun advanceEligibleStatuses(userId: Long) {
        refreshStatuses(userId)
    }

    fun refreshStatuses(userId: Long, nowMillis: Long = System.currentTimeMillis()) {
        if (userId <= 0L) return
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.rawQuery(
                "SELECT id, status, order_date FROM orders WHERE user_id = ? AND status != 'Cancelled'",
                arrayOf(userId.toString())
            ).use { cursor ->
                val updates = mutableListOf<Pair<Long, String>>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val current = cursor.getString(1)
                    val placedAt = cursor.getLong(2)
                    val elapsed = (nowMillis - placedAt).coerceAtLeast(0L)
                    val target = when {
                        elapsed >= COMPLETED_AFTER_MS -> "Completed"
                        elapsed >= PREPARING_AFTER_MS -> "Preparing"
                        else -> "Pending"
                    }
                    if (current != target) updates += id to target
                }
                updates.forEach { (id, status) ->
                    val values = ContentValues().apply { put("status", status) }
                    db.update("orders", values, "id = ? AND user_id = ? AND status != 'Cancelled'", arrayOf(id.toString(), userId.toString()))
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getOrders(userId: Long, statusFilter: String = FILTER_ALL): List<Order> {
        if (userId <= 0L) return emptyList()
        val result = mutableListOf<Order>()
        val db = helper.readableDatabase
        val filter = if (statusFilter == FILTER_ALL) "" else " AND o.status = ?"
        val args = if (filter.isEmpty()) arrayOf(userId.toString()) else arrayOf(userId.toString(), statusFilter)
        val sql = """
            SELECT o.id, o.user_id, o.restaurant_id, COALESCE(r.name, 'Restaurant'),
                   o.total_price, o.delivery_address, o.contact_number, o.status, o.order_date
            FROM orders o LEFT JOIN restaurants r ON r.id = o.restaurant_id
            WHERE o.user_id = ?$filter ORDER BY o.order_date DESC, o.id DESC
        """.trimIndent()
        db.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) result += Order(
                id = c.getLong(0), userId = c.getInt(1), restaurantId = c.getInt(2),
                restaurantName = c.getString(3) ?: "Restaurant", totalAmountCentavos = c.getInt(4),
                deliveryAddress = c.getString(5) ?: "", contactNumber = c.getString(6) ?: "",
                status = c.getString(7) ?: "Pending", createdAt = c.getLong(8)
            )
        }
        return result
    }

    fun getOrder(userId: Long, orderId: Long): Order? {
        if (userId <= 0L || orderId <= 0L) return null
        val db = helper.readableDatabase
        db.rawQuery("""
            SELECT o.id, o.user_id, o.restaurant_id, COALESCE(r.name, 'Restaurant'),
                   o.total_price, o.delivery_address, o.contact_number, o.status, o.order_date
            FROM orders o LEFT JOIN restaurants r ON r.id = o.restaurant_id
            WHERE o.user_id = ? AND o.id = ? LIMIT 1
        """.trimIndent(), arrayOf(userId.toString(), orderId.toString())).use { c ->
            if (!c.moveToFirst()) return null
            return Order(
                id = c.getLong(0), userId = c.getInt(1), restaurantId = c.getInt(2),
                restaurantName = c.getString(3) ?: "Restaurant", totalAmountCentavos = c.getInt(4),
                deliveryAddress = c.getString(5) ?: "", contactNumber = c.getString(6) ?: "",
                status = c.getString(7) ?: "Pending", createdAt = c.getLong(8)
            )
        }
    }

    fun getItems(userId: Long, orderId: Long): List<OrderItem> {
        if (userId <= 0L || orderId <= 0L) return emptyList()
        val result = mutableListOf<OrderItem>()
        val db = helper.readableDatabase
        db.rawQuery("""
       SELECT oi.id, oi.order_id, oi.menu_item_id,
           COALESCE(m.name, 'Item'),
           COALESCE(m.description, ''),
           oi.quantity,
           oi.price,
           COALESCE(m.image, '')
            FROM order_items oi
            INNER JOIN orders o ON o.id = oi.order_id
            LEFT JOIN menu_items m ON m.id = oi.menu_item_id
            WHERE o.user_id = ? AND o.id = ? ORDER BY oi.id
        """.trimIndent(), arrayOf(userId.toString(), orderId.toString())).use { c ->
            while (c.moveToNext()) result +=
                OrderItem(
                    id = c.getLong(0),
                    orderId = c.getLong(1),
                    menuItemId = c.getInt(2),
                    foodName = c.getString(3) ?: "Item",
                    foodDescription = c.getString(4) ?: "",
                    quantity = c.getInt(5),
                    priceCentavos = c.getInt(6),
                    imageResName = c.getString(7) ?: ""
                )
        }
        return result
    }

    /** Atomically cancels only a still-Pending order belonging to this customer. */
    fun cancelPendingOrder(userId: Long, orderId: Long): Boolean {
        if (userId <= 0L || orderId <= 0L) return false
        val values = ContentValues().apply { put("status", "Cancelled") }
        return helper.writableDatabase.update(
            "orders", values, "id = ? AND user_id = ? AND status = 'Pending'",
            arrayOf(orderId.toString(), userId.toString())
        ) == 1
    }

    fun getMostRecentOrderForUser(userId: Long): Order? {
        if (userId <= 0L) return null

        return getOrders(
            userId = userId,
            statusFilter = FILTER_ALL
        ).firstOrNull()
    }

    companion object {
        const val FILTER_ALL = "All"
        const val PREPARING_AFTER_MS = 2 * 60 * 1000L
        const val COMPLETED_AFTER_MS = 5 * 60 * 1000L
    }
}
