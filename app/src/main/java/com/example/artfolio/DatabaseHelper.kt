package com.example.artfolio

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ArtGallery.db"
        private const val DATABASE_VERSION = 3 // Ensure this is higher than previous versions

        // Table: Artworks
        private const val TABLE_ARTWORK = "artwork"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_IMAGE_PATH = "image_path"
        private const val KEY_MEDIUM = "medium"
        private const val KEY_STYLE = "style"
        private const val KEY_THEME = "theme"
        private const val KEY_IMAGE_WIDTH = "image_width"
        private const val KEY_IMAGE_HEIGHT = "image_height"

        // Table: Users
        private const val TABLE_USERS = "users"
        private const val COLUMN_USER_ID = "id"
        private const val COLUMN_FIRSTNAME = "firstname"
        private const val COLUMN_LASTNAME = "lastname"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_PROFILE_IMAGE = "profile_image"

        private const val TAG = "DatabaseHelper"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createArtworkTable = """
            CREATE TABLE $TABLE_ARTWORK (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_TITLE TEXT,
                $KEY_DESCRIPTION TEXT,
                $KEY_IMAGE_PATH TEXT,
                $KEY_MEDIUM TEXT,
                $KEY_STYLE TEXT,
                $KEY_THEME TEXT,
                $KEY_IMAGE_WIDTH INTEGER,
                $KEY_IMAGE_HEIGHT INTEGER
            )
        """.trimIndent()

        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FIRSTNAME TEXT,
                $COLUMN_LASTNAME TEXT,
                $COLUMN_EMAIL TEXT UNIQUE,
                $COLUMN_PASSWORD TEXT,
                $COLUMN_PROFILE_IMAGE TEXT
            )
        """.trimIndent()

        db.execSQL(createArtworkTable)
        db.execSQL(createUsersTable)
        Log.i(TAG, "Database created with tables: $TABLE_ARTWORK, $TABLE_USERS")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.i(TAG, "Upgrading database from version $oldVersion to $newVersion")
        if (oldVersion < 3) {
            try {
                // Add profile_image column if it doesn't exist
                db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_PROFILE_IMAGE TEXT")
                Log.i(TAG, "Added $COLUMN_PROFILE_IMAGE column to $TABLE_USERS")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding $COLUMN_PROFILE_IMAGE column: ${e.message}")
            }
        }
        // For future upgrades, add more conditions here
    }

    /** ======================= CRUD Operations for Artworks ======================= */

    fun addArtwork(artwork: Artwork): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, artwork.title)
            put(KEY_DESCRIPTION, artwork.description)
            put(KEY_IMAGE_PATH, artwork.imagePath)
            put(KEY_MEDIUM, artwork.medium)
            put(KEY_STYLE, artwork.style)
            put(KEY_THEME, artwork.theme)
            put(KEY_IMAGE_WIDTH, artwork.imageWidth)
            put(KEY_IMAGE_HEIGHT, artwork.imageHeight)
        }
        val id = db.insert(TABLE_ARTWORK, null, values)
        db.close()
        return id
    }

    fun getAllArtworks(): List<Artwork> {
        val artworks = mutableListOf<Artwork>()
        val db = readableDatabase
        val cursor: Cursor? = db.rawQuery("SELECT * FROM $TABLE_ARTWORK", null)

        cursor?.use {
            while (it.moveToNext()) {
                val artwork = Artwork(
                    id = it.getInt(it.getColumnIndexOrThrow(KEY_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(KEY_TITLE)),
                    description = it.getString(it.getColumnIndexOrThrow(KEY_DESCRIPTION)),
                    imagePath = it.getString(it.getColumnIndexOrThrow(KEY_IMAGE_PATH)),
                    medium = it.getString(it.getColumnIndexOrThrow(KEY_MEDIUM)),
                    style = it.getString(it.getColumnIndexOrThrow(KEY_STYLE)),
                    theme = it.getString(it.getColumnIndexOrThrow(KEY_THEME)),
                    imageWidth = it.getInt(it.getColumnIndexOrThrow(KEY_IMAGE_WIDTH)),
                    imageHeight = it.getInt(it.getColumnIndexOrThrow(KEY_IMAGE_HEIGHT))
                )
                artworks.add(artwork)
            }
        }
        cursor?.close()
        db.close()
        return artworks
    }

    fun updateArtwork(artwork: Artwork): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, artwork.title)
            put(KEY_DESCRIPTION, artwork.description)
            put(KEY_IMAGE_PATH, artwork.imagePath)
            put(KEY_MEDIUM, artwork.medium)
            put(KEY_STYLE, artwork.style)
            put(KEY_THEME, artwork.theme)
            put(KEY_IMAGE_WIDTH, artwork.imageWidth)
            put(KEY_IMAGE_HEIGHT, artwork.imageHeight)
        }
        val rowsAffected = db.update(TABLE_ARTWORK, values, "$KEY_ID = ?", arrayOf(artwork.id.toString()))
        db.close()
        return rowsAffected
    }

    fun deleteArtwork(id: Int): Int {
        val db = writableDatabase
        val rowsAffected = db.delete(TABLE_ARTWORK, "$KEY_ID = ?", arrayOf(id.toString()))
        db.close()
        return rowsAffected
    }

    /** ======================= User Authentication Methods ======================= */

    fun insertUser(firstname: String, lastname: String, email: String, password: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FIRSTNAME, firstname)
            put(COLUMN_LASTNAME, lastname)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_PROFILE_IMAGE, "") // Initial empty profile image
        }

        return try {
            val result = db.insert(TABLE_USERS, null, values)
            db.close()
            Log.d(TAG, "User inserted: $firstname $lastname, Email: $email, Result: $result")
            result != -1L
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting user: ${e.message}")
            false
        }
    }

    fun checkUser(email: String, password: String): Triple<Boolean, String?, String?> {
        val db = this.readableDatabase
        val query = "SELECT $COLUMN_FIRSTNAME, $COLUMN_LASTNAME, $COLUMN_PROFILE_IMAGE FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(email, password))

        return if (cursor.moveToFirst()) {
            val firstname = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRSTNAME))
            val lastname = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LASTNAME))
            val profileImagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_IMAGE))
            val username = "$firstname $lastname"
            cursor.close()
            db.close()
            Log.d(TAG, "User found: $username, Profile Image: $profileImagePath")
            Triple(true, username, profileImagePath)
        } else {
            cursor.close()
            db.close()
            Log.d(TAG, "No user found for email: $email")
            Triple(false, null, null)
        }
    }

    fun updateProfileImage(email: String, imagePath: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PROFILE_IMAGE, imagePath)
        }
        val rowsAffected = db.update(TABLE_USERS, values, "$COLUMN_EMAIL = ?", arrayOf(email))
        db.close()
        Log.d(TAG, "Profile image updated for $email: $imagePath, Rows affected: $rowsAffected")
    }
}