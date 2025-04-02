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
        private const val DATABASE_VERSION = 6

        // Table: Artworks
        const val TABLE_ARTWORK = "artwork"
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_IMAGE_PATH = "image_path"
        const val KEY_MEDIUM = "medium"
        const val KEY_STYLE = "style"
        const val KEY_THEME = "theme"
        const val KEY_IMAGE_WIDTH = "image_width"
        const val KEY_IMAGE_HEIGHT = "image_height"
        const val KEY_ARTIST_EMAIL = "artist_email"

        // Table: Users
        const val TABLE_USERS = "users"
        const val COLUMN_USER_ID = "id"
        const val COLUMN_FIRSTNAME = "firstname"
        const val COLUMN_LASTNAME = "lastname"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_PROFILE_IMAGE = "profile_image"
        const val COLUMN_MOBILE_NO = "mobile_no"
        const val COLUMN_USER_TYPE = "user_type"

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
                $KEY_IMAGE_HEIGHT INTEGER,
                $KEY_ARTIST_EMAIL TEXT
            )
        """.trimIndent()

        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_FIRSTNAME TEXT,
                $COLUMN_LASTNAME TEXT,
                $COLUMN_EMAIL TEXT UNIQUE,
                $COLUMN_PASSWORD TEXT,
                $COLUMN_PROFILE_IMAGE TEXT,
                $COLUMN_MOBILE_NO TEXT,
                $COLUMN_USER_TYPE TEXT
            )
        """.trimIndent()

        db.execSQL(createArtworkTable)
        db.execSQL(createUsersTable)
        Log.i(TAG, "Database created with tables: $TABLE_ARTWORK, $TABLE_USERS")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.i(TAG, "Upgrading database from version $oldVersion to $newVersion")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ARTWORK")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    /** ======================= CRUD Operations for Artworks ======================= */

    fun addArtwork(artwork: ArtworkWithPhone, artistEmail: String): Long {
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
            put(KEY_ARTIST_EMAIL, artistEmail)
        }
        val id = try {
            db.insertOrThrow(TABLE_ARTWORK, null, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting artwork: ${e.message}")
            -1L
        } finally {
            db.close()
        }
        Log.d(TAG, "Artwork added with ID: $id, Artist: $artistEmail")
        return id
    }

    fun getArtworks(userType: String, email: String?): List<ArtworkWithPhone> {
        val artworks = mutableListOf<ArtworkWithPhone>()
        val db = readableDatabase
        val query = if (userType == "artist" && email != null) {
            """
                SELECT a.*, u.$COLUMN_MOBILE_NO 
                FROM $TABLE_ARTWORK a 
                LEFT JOIN $TABLE_USERS u ON a.$KEY_ARTIST_EMAIL = u.$COLUMN_EMAIL 
                WHERE a.$KEY_ARTIST_EMAIL = ?
            """.trimIndent()
        } else {
            """
                SELECT a.*, u.$COLUMN_MOBILE_NO 
                FROM $TABLE_ARTWORK a 
                LEFT JOIN $TABLE_USERS u ON a.$KEY_ARTIST_EMAIL = u.$COLUMN_EMAIL
            """.trimIndent()
        }
        val cursor: Cursor? = if (userType == "artist" && email != null) {
            db.rawQuery(query, arrayOf(email))
        } else {
            db.rawQuery(query, null)
        }

        cursor?.use {
            while (it.moveToNext()) {
                val artwork = ArtworkWithPhone(
                    id = it.getInt(it.getColumnIndexOrThrow(KEY_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(KEY_TITLE)) ?: "",
                    description = it.getString(it.getColumnIndexOrThrow(KEY_DESCRIPTION)) ?: "",
                    imagePath = it.getString(it.getColumnIndexOrThrow(KEY_IMAGE_PATH)) ?: "",
                    medium = it.getString(it.getColumnIndexOrThrow(KEY_MEDIUM)) ?: "",
                    style = it.getString(it.getColumnIndexOrThrow(KEY_STYLE)) ?: "",
                    theme = it.getString(it.getColumnIndexOrThrow(KEY_THEME)) ?: "",
                    imageWidth = it.getInt(it.getColumnIndexOrThrow(KEY_IMAGE_WIDTH)),
                    imageHeight = it.getInt(it.getColumnIndexOrThrow(KEY_IMAGE_HEIGHT)),
                    artistEmail = it.getString(it.getColumnIndexOrThrow(KEY_ARTIST_EMAIL)) ?: "",
                    artistPhone = it.getString(it.getColumnIndexOrThrow(COLUMN_MOBILE_NO)) ?: "Not available"
                )
                artworks.add(artwork)
            }
        }
        cursor?.close()
        db.close()
        Log.d(TAG, "Fetched ${artworks.size} artworks for userType: $userType, email: $email")
        return artworks
    }

    fun updateArtwork(artwork: ArtworkWithPhone): Int {
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
        Log.d(TAG, "Updated artwork ID: ${artwork.id}, Rows affected: $rowsAffected")
        return rowsAffected
    }

    fun deleteArtwork(id: Int): Int {
        val db = writableDatabase
        val rowsAffected = db.delete(TABLE_ARTWORK, "$KEY_ID = ?", arrayOf(id.toString()))
        db.close()
        Log.d(TAG, "Deleted artwork ID: $id, Rows affected: $rowsAffected")
        return rowsAffected
    }

    /** ======================= User Authentication Methods ======================= */

    fun insertUser(firstname: String, lastname: String, email: String, password: String, mobileNo: String, userType: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_FIRSTNAME, firstname)
            put(COLUMN_LASTNAME, lastname)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_PROFILE_IMAGE, "")
            put(COLUMN_MOBILE_NO, mobileNo)
            put(COLUMN_USER_TYPE, userType)
        }
        val result = try {
            db.insertOrThrow(TABLE_USERS, null, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting user: ${e.message}")
            -1L
        } finally {
            db.close()
        }
        Log.d(TAG, "User inserted: $firstname $lastname, Email: $email, Mobile: $mobileNo, Type: $userType, Result: $result")
        return result != -1L
    }

    fun checkUser(email: String, password: String): Quadruple<Boolean, String?, String?, String?> {
        val db = readableDatabase
        val query = """
            SELECT $COLUMN_FIRSTNAME, $COLUMN_LASTNAME, $COLUMN_PROFILE_IMAGE, $COLUMN_USER_TYPE 
            FROM $TABLE_USERS 
            WHERE $COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(email, password))

        return if (cursor.moveToFirst()) {
            val firstname = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRSTNAME))
            val lastname = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LASTNAME))
            val profileImagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_IMAGE))
            val userType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_TYPE))
            val username = "$firstname $lastname"
            cursor.close()
            db.close()
            Log.d(TAG, "User found: $username, Type: $userType")
            Quadruple(true, username, profileImagePath, userType)
        } else {
            cursor.close()
            db.close()
            Log.d(TAG, "No user found for email: $email")
            Quadruple(false, null, null, null)
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

data class ArtworkWithPhone(
    val id: Int = 0,
    val title: String,
    val description: String,
    val imagePath: String,
    val medium: String,
    val style: String,
    val theme: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val artistEmail: String,
    val artistPhone: String
)

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)