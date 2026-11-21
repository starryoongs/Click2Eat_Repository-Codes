package com.example.click2eat.database

import android.content.Context
import com.example.click2eat.models.Restaurant

class RestaurantRepository(context: Context) {

    private val helper = DatabaseHelper(context.applicationContext)

    fun getAllRestaurants(): List<Restaurant> {
        val restaurants = mutableListOf<Restaurant>()

        helper.readableDatabase.query(
            "restaurants",
            arrayOf(
                "id",
                "name",
                "description",
                "image",
                "delivery_time"
            ),
            null,
            null,
            null,
            null,
            "name ASC"
        ).use { cursor ->

            while (cursor.moveToNext()) {
                restaurants.add(
                    Restaurant(
                        id = cursor.getLong(
                            cursor.getColumnIndexOrThrow("id")
                        ),
                        name = cursor.getString(
                            cursor.getColumnIndexOrThrow("name")
                        ),
                        description = cursor.getString(
                            cursor.getColumnIndexOrThrow("description")
                        ),
                        image = cursor.getString(
                            cursor.getColumnIndexOrThrow("image")
                        ),
                        deliveryTime = cursor.getString(
                            cursor.getColumnIndexOrThrow("delivery_time")
                        )
                    )
                )
            }
        }

        return restaurants
    }

    fun getRestaurantById(id: Long): Restaurant? {
        helper.readableDatabase.query(
            "restaurants",
            arrayOf(
                "id",
                "name",
                "description",
                "image",
                "delivery_time"
            ),
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        ).use { cursor ->

            if (cursor.moveToFirst()) {
                return Restaurant(
                    id = cursor.getLong(
                        cursor.getColumnIndexOrThrow("id")
                    ),
                    name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name")
                    ),
                    description = cursor.getString(
                        cursor.getColumnIndexOrThrow("description")
                    ),
                    image = cursor.getString(
                        cursor.getColumnIndexOrThrow("image")
                    ),
                    deliveryTime = cursor.getString(
                        cursor.getColumnIndexOrThrow("delivery_time")
                    )
                )
            }
        }

        return null
    }

    fun searchRestaurants(keyword: String): List<Restaurant> {
        val restaurants = mutableListOf<Restaurant>()
        val searchKeyword = "%" + keyword.trim().lowercase() + "%"

        helper.readableDatabase.rawQuery(
            """
            SELECT id, name, description, image, delivery_time
            FROM restaurants
            WHERE LOWER(name) LIKE ?
               OR LOWER(COALESCE(description, '')) LIKE ?
            ORDER BY name ASC
            """.trimIndent(),
            arrayOf(searchKeyword, searchKeyword)
        ).use { cursor ->

            while (cursor.moveToNext()) {
                restaurants.add(
                    Restaurant(
                        id = cursor.getLong(
                            cursor.getColumnIndexOrThrow("id")
                        ),
                        name = cursor.getString(
                            cursor.getColumnIndexOrThrow("name")
                        ),
                        description = cursor.getString(
                            cursor.getColumnIndexOrThrow("description")
                        ),
                        image = cursor.getString(
                            cursor.getColumnIndexOrThrow("image")
                        ),
                        deliveryTime = cursor.getString(
                            cursor.getColumnIndexOrThrow("delivery_time")
                        )
                    )
                )
            }
        }

        return restaurants
    }
}