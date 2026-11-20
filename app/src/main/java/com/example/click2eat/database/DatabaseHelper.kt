
package com.example.click2eat.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "click2eat.db"
        const val DATABASE_VERSION = 1
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {

        // 1. USERS
        db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                first_name TEXT NOT NULL,
                last_name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent())

        // 2. RESTAURANTS
        db.execSQL("""
            CREATE TABLE restaurants (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                image TEXT,
                delivery_time TEXT
            )
        """.trimIndent())

        // 3. CATEGORIES
        db.execSQL("""
            CREATE TABLE categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE
            )
        """.trimIndent())

        // 4. MENU ITEMS
        db.execSQL("""
            CREATE TABLE menu_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                restaurant_id INTEGER NOT NULL,
                category_id INTEGER,
                name TEXT NOT NULL,
                description TEXT,
                price INTEGER NOT NULL CHECK(price >= 0),
                image TEXT,
                available INTEGER NOT NULL DEFAULT 1
                    CHECK(available IN (0, 1)),
                FOREIGN KEY (restaurant_id)
                    REFERENCES restaurants(id),
                FOREIGN KEY (category_id)
                    REFERENCES categories(id)
            )
        """.trimIndent())

        // 5. CART ITEMS
        db.execSQL("""
            CREATE TABLE cart_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                menu_item_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL DEFAULT 1
                    CHECK(quantity > 0),
                UNIQUE(user_id, menu_item_id),
                FOREIGN KEY (user_id)
                    REFERENCES users(id),
                FOREIGN KEY (menu_item_id)
                    REFERENCES menu_items(id)
            )
        """.trimIndent())

        // 6. ORDERS
        db.execSQL("""
            CREATE TABLE orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                restaurant_id INTEGER NOT NULL,
                total_price INTEGER NOT NULL
                    CHECK(total_price >= 0),
                delivery_address TEXT NOT NULL,
                contact_number TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'Pending'
                    CHECK(status IN (
                        'Pending',
                        'Preparing',
                        'Completed',
                        'Cancelled'
                    )),
                order_date INTEGER NOT NULL,
                FOREIGN KEY (user_id)
                    REFERENCES users(id),
                FOREIGN KEY (restaurant_id)
                    REFERENCES restaurants(id)
            )
        """.trimIndent())

        // 7. ORDER ITEMS
        db.execSQL("""
            CREATE TABLE order_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id INTEGER NOT NULL,
                menu_item_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL
                    CHECK(quantity > 0),
                price INTEGER NOT NULL
                    CHECK(price >= 0),
                FOREIGN KEY (order_id)
                    REFERENCES orders(id),
                FOREIGN KEY (menu_item_id)
                    REFERENCES menu_items(id)
            )
        """.trimIndent())
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Future schema migrations go here.
        // Do not delete customer or order records.
    }
}
