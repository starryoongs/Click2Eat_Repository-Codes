package com.example.click2eat.database

import android.content.Context
import com.example.click2eat.models.Category
import com.example.click2eat.models.MenuItem

class MenuRepository(context: Context) {

    private val helper = DatabaseHelper(context.applicationContext)

    fun getMenuItemsForRestaurant(
        restaurantId: Long
    ): List<MenuItem> {
        val menuItems = mutableListOf<MenuItem>()

        helper.readableDatabase.query(
            "menu_items",
            null,
            "restaurant_id = ?",
            arrayOf(restaurantId.toString()),
            null,
            null,
            "name ASC"
        ).use { cursor ->

            while (cursor.moveToNext()) {
                menuItems.add(
                    MenuItem(
                        id = cursor.getLong(
                            cursor.getColumnIndexOrThrow("id")
                        ),
                        restaurantId = cursor.getLong(
                            cursor.getColumnIndexOrThrow("restaurant_id")
                        ),
                        categoryId =
                            if (cursor.isNull(
                                    cursor.getColumnIndexOrThrow(
                                        "category_id"
                                    )
                                )
                            ) {
                                null
                            } else {
                                cursor.getLong(
                                    cursor.getColumnIndexOrThrow(
                                        "category_id"
                                    )
                                )
                            },
                        name = cursor.getString(
                            cursor.getColumnIndexOrThrow("name")
                        ),
                        description = cursor.getString(
                            cursor.getColumnIndexOrThrow("description")
                        ),
                        priceCentavos = cursor.getInt(
                            cursor.getColumnIndexOrThrow("price")
                        ),
                        image = cursor.getString(
                            cursor.getColumnIndexOrThrow("image")
                        ),
                        available = cursor.getInt(
                            cursor.getColumnIndexOrThrow("available")
                        ) == 1
                    )
                )
            }
        }

        return menuItems
    }

    fun searchMenuItems(keyword: String): List<MenuItem> {
        val menuItems = mutableListOf<MenuItem>()
        val searchKeyword = "%" + keyword.trim().lowercase() + "%"

        helper.readableDatabase.rawQuery(
            """
            SELECT id, restaurant_id, category_id, name,
                   description, price, image, available
            FROM menu_items
            WHERE LOWER(name) LIKE ?
               OR LOWER(COALESCE(description, '')) LIKE ?
            ORDER BY name ASC
            """.trimIndent(),
            arrayOf(searchKeyword, searchKeyword)
        ).use { cursor ->

            while (cursor.moveToNext()) {
                menuItems.add(
                    MenuItem(
                        id = cursor.getLong(
                            cursor.getColumnIndexOrThrow("id")
                        ),
                        restaurantId = cursor.getLong(
                            cursor.getColumnIndexOrThrow("restaurant_id")
                        ),
                        categoryId =
                            if (cursor.isNull(
                                    cursor.getColumnIndexOrThrow(
                                        "category_id"
                                    )
                                )
                            ) {
                                null
                            } else {
                                cursor.getLong(
                                    cursor.getColumnIndexOrThrow(
                                        "category_id"
                                    )
                                )
                            },
                        name = cursor.getString(
                            cursor.getColumnIndexOrThrow("name")
                        ),
                        description = cursor.getString(
                            cursor.getColumnIndexOrThrow("description")
                        ),
                        priceCentavos = cursor.getInt(
                            cursor.getColumnIndexOrThrow("price")
                        ),
                        image = cursor.getString(
                            cursor.getColumnIndexOrThrow("image")
                        ),
                        available = cursor.getInt(
                            cursor.getColumnIndexOrThrow("available")
                        ) == 1
                    )
                )
            }
        }

        return menuItems
    }

    fun getCategories(): List<Category> {
        val categories = mutableListOf<Category>()

        helper.readableDatabase.query(
            "categories",
            arrayOf("id", "name"),
            null,
            null,
            null,
            null,
            "id ASC"
        ).use { cursor ->

            while (cursor.moveToNext()) {
                categories.add(
                    Category(
                        id = cursor.getLong(
                            cursor.getColumnIndexOrThrow("id")
                        ),
                        name = cursor.getString(
                            cursor.getColumnIndexOrThrow("name")
                        )
                    )
                )
            }
        }

        return categories
    }
}