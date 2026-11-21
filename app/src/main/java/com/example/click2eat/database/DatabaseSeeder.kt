package com.example.click2eat.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

object DatabaseSeeder {

    fun seed(context: Context) {
        val database =
            DatabaseHelper(context.applicationContext).writableDatabase

        val restaurantCount = database.rawQuery(
            "SELECT COUNT(*) FROM restaurants",
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }

        if (restaurantCount > 0) {
            return
        }

        val categoryIds = mutableMapOf<String, Long>()

        val categories = listOf(
            "Fastfood",
            "Burger",
            "Chicken",
            "Coffee",
            "Japanese",
            "Korean",
            "Pastry",
            "Beverage"
        )

        for (categoryName in categories) {
            val values = ContentValues().apply {
                put("name", categoryName)
            }

            database.insertWithOnConflict(
                "categories",
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE
            )

            database.query(
                "categories",
                arrayOf("id"),
                "name = ?",
                arrayOf(categoryName),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    categoryIds[categoryName] =
                        cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                }
            }
        }

        val restaurantIds = mutableMapOf<String, Long>()

        addRestaurant(
            database,
            restaurantIds,
            "Burger King",
            "Burgers, fries, and fast food",
            "burgerking",
            "20-30 mins"
        )

        addRestaurant(
            database,
            restaurantIds,
            "Jollibee",
            "Fast food favorites and chicken meals",
            "jollibee",
            "20-30 mins"
        )

        addRestaurant(
            database,
            restaurantIds,
            "KFC",
            "Chicken meals and sides",
            "kfc",
            "20-30 mins"
        )

        addRestaurant(
            database,
            restaurantIds,
            "McDo",
            "Burgers, chicken, and fries",
            "mcdo",
            "20-30 mins"
        )

        addRestaurant(
            database,
            restaurantIds,
            "Ramen Nagi",
            "Japanese ramen and beverages",
            "ramen_nagi",
            "25-35 mins"
        )

        addRestaurant(
            database,
            restaurantIds,
            "Starbucks",
            "Coffee, pastries, and beverages",
            "starbucks",
            "15-25 mins"
        )

        addRestaurant(
            database,
            restaurantIds,
            "Tokyo Tokyo",
            "Japanese and beverages",
            "tokyo_tokyo",
            "20-30 mins"
        )

        addMenuItem(
            database,
            restaurantIds["McDo"],
            categoryIds["Burger"],
            "Big Mac",
            "Beef patties, lettuce, cheese, pickles, and sauce.",
            19900,
            "bigmac"
        )

        addMenuItem(
            database,
            restaurantIds["McDo"],
            categoryIds["Chicken"],
            "Chicken Sandwich",
            "Juicy chicken patty with creamy mayonnaise.",
            7500,
            "chicken_sandwich"
        )

        addMenuItem(
            database,
            restaurantIds["McDo"],
            categoryIds["Chicken"],
            "Chicken Nuggets",
            "Six crispy chicken pieces.",
            12000,
            "nuggets"
        )

        addMenuItem(
            database,
            restaurantIds["McDo"],
            categoryIds["Fastfood"],
            "Fries",
            "Hot and crunchy straight-cut fries.",
            7000,
            "mcdo_fries"
        )

        addMenuItem(
            database,
            restaurantIds["KFC"],
            categoryIds["FastFood"],
            "Zinger Burger",
            "Yummy burger.",
            16000,
            "kfc_zinger"
        )

        addMenuItem(
            database,
            restaurantIds["KFC"],
            categoryIds["Fastfood"],
            "Brownie",
            "Brownie dessert.",
            16000,
            "kfc_brownie"
        )
    }

    private fun addRestaurant(
        database: SQLiteDatabase,
        restaurantIds: MutableMap<String, Long>,
        name: String,
        description: String,
        image: String,
        deliveryTime: String
    ) {
        val values = ContentValues().apply {
            put("name", name)
            put("description", description)
            put("image", image)
            put("delivery_time", deliveryTime)
        }

        val id = database.insert(
            "restaurants",
            null,
            values
        )

        if (id != -1L) {
            restaurantIds[name] = id
        }
    }

    private fun addMenuItem(
        database: SQLiteDatabase,
        restaurantId: Long?,
        categoryId: Long?,
        name: String,
        description: String,
        priceCentavos: Int,
        image: String
    ) {
        if (restaurantId == null) return

        val values = ContentValues().apply {
            put("restaurant_id", restaurantId)
            put("category_id", categoryId)
            put("name", name)
            put("description", description)
            put("price", priceCentavos)
            put("image", image)
            put("available", 1)
        }

        database.insert(
            "menu_items",
            null,
            values
        )
    }
}