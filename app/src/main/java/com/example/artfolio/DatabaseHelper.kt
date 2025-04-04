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
        private const val DATABASE_VERSION = 8 // Incremented to 8 for new tables and fields

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
        const val KEY_ORIGINAL_PRICE = "original_price"
        const val KEY_DISCOUNTED_PRICE = "discounted_price"

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
        const val COLUMN_BIO = "bio" // New field
        const val COLUMN_SOCIAL_MEDIA = "social_media" // New field (e.g., JSON or comma-separated)

        // Table: Wishlist
        const val TABLE_WISHLIST = "wishlist"
        const val WISHLIST_ID = "id"
        const val WISHLIST_BUYER_EMAIL = "buyer_email"
        const val WISHLIST_ARTWORK_ID = "artwork_id"

        // Table: Purchases
        const val TABLE_PURCHASES = "purchases"
        const val PURCHASE_ID = "id"
        const val PURCHASE_BUYER_EMAIL = "buyer_email"
        const val PURCHASE_ARTWORK_ID = "artwork_id"
        const val PURCHASE_DATE = "purchase_date"

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
                $KEY_ARTIST_EMAIL TEXT,
                $KEY_ORIGINAL_PRICE REAL,
                $KEY_DISCOUNTED_PRICE REAL
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
                $COLUMN_USER_TYPE TEXT,
                $COLUMN_BIO TEXT,
                $COLUMN_SOCIAL_MEDIA TEXT
            )
        """.trimIndent()

        val createWishlistTable = """
            CREATE TABLE $TABLE_WISHLIST (
                $WISHLIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $WISHLIST_BUYER_EMAIL TEXT,
                $WISHLIST_ARTWORK_ID INTEGER,
                FOREIGN KEY ($WISHLIST_ARTWORK_ID) REFERENCES $TABLE_ARTWORK($KEY_ID)
            )
        """.trimIndent()

        val createPurchasesTable = """
            CREATE TABLE $TABLE_PURCHASES (
                $PURCHASE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $PURCHASE_BUYER_EMAIL TEXT,
                $PURCHASE_ARTWORK_ID INTEGER,
                $PURCHASE_DATE TEXT,
                FOREIGN KEY ($PURCHASE_ARTWORK_ID) REFERENCES $TABLE_ARTWORK($KEY_ID)
            )
        """.trimIndent()

        db.execSQL(createArtworkTable)
        db.execSQL(createUsersTable)
        db.execSQL(createWishlistTable)
        db.execSQL(createPurchasesTable)
        Log.i(TAG, "Database created with tables: $TABLE_ARTWORK, $TABLE_USERS, $TABLE_WISHLIST, $TABLE_PURCHASES")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.i(TAG, "Upgrading database from version $oldVersion to $newVersion")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ARTWORK")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WISHLIST")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PURCHASES")
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
            put(KEY_ORIGINAL_PRICE, artwork.originalPrice)
            put(KEY_DISCOUNTED_PRICE, artwork.discountedPrice)
        }
        val id = db.insertOrThrow(TABLE_ARTWORK, null, values)
        db.close()
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
        val cursor = if (userType == "artist" && email != null) {
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
                    artistPhone = it.getString(it.getColumnIndexOrThrow(COLUMN_MOBILE_NO)) ?: "Not available",
                    originalPrice = it.getFloat(it.getColumnIndexOrThrow(KEY_ORIGINAL_PRICE)),
                    discountedPrice = it.getFloat(it.getColumnIndexOrThrow(KEY_DISCOUNTED_PRICE))
                )
                artworks.add(artwork)
            }
        }
        cursor.close()
        db.close()
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
            put(KEY_ORIGINAL_PRICE, artwork.originalPrice)
            put(KEY_DISCOUNTED_PRICE, artwork.discountedPrice)
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

    /** ======================= User Profile Methods ======================= */

    fun updateUserProfile(email: String, bio: String, socialMedia: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_BIO, bio)
            put(COLUMN_SOCIAL_MEDIA, socialMedia)
        }
        db.update(TABLE_USERS, values, "$COLUMN_EMAIL = ?", arrayOf(email))
        db.close()
        Log.d(TAG, "Profile updated for $email: Bio=$bio, SocialMedia=$socialMedia")
    }

    fun getUserProfile(email: String): Triple<String?, String?, String?> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_BIO, $COLUMN_SOCIAL_MEDIA, $COLUMN_MOBILE_NO FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?",
            arrayOf(email)
        )
        var bio: String? = null
        var socialMedia: String? = null
        var mobileNo: String? = null
        if (cursor.moveToFirst()) {
            bio = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIO))
            socialMedia = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOCIAL_MEDIA))
            mobileNo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MOBILE_NO))
        }
        cursor.close()
        db.close()
        return Triple(bio, socialMedia, mobileNo)
    }

    fun getPastSales(artistEmail: String): List<ArtworkWithPhone> {
        val sales = mutableListOf<ArtworkWithPhone>()
        val db = readableDatabase
        val query = """
            SELECT a.* 
            FROM $TABLE_ARTWORK a 
            INNER JOIN $TABLE_PURCHASES p ON a.$KEY_ID = p.$PURCHASE_ARTWORK_ID 
            WHERE a.$KEY_ARTIST_EMAIL = ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(artistEmail))
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
                    artistPhone = "",
                    originalPrice = it.getFloat(it.getColumnIndexOrThrow(KEY_ORIGINAL_PRICE)),
                    discountedPrice = it.getFloat(it.getColumnIndexOrThrow(KEY_DISCOUNTED_PRICE))
                )
                sales.add(artwork)
            }
        }
        cursor.close()
        db.close()
        return sales
    }

    /** ======================= Wishlist Methods ======================= */

    fun addToWishlist(buyerEmail: String, artworkId: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(WISHLIST_BUYER_EMAIL, buyerEmail)
            put(WISHLIST_ARTWORK_ID, artworkId)
        }
        val id = db.insert(TABLE_WISHLIST, null, values)
        db.close()
        return id
    }

    fun removeFromWishlist(buyerEmail: String, artworkId: Int): Int {
        val db = writableDatabase
        val rowsAffected = db.delete(TABLE_WISHLIST, "$WISHLIST_BUYER_EMAIL = ? AND $WISHLIST_ARTWORK_ID = ?", arrayOf(buyerEmail, artworkId.toString()))
        db.close()
        return rowsAffected
    }

    fun getWishlist(buyerEmail: String): List<ArtworkWithPhone> {
        val wishlist = mutableListOf<ArtworkWithPhone>()
        val db = readableDatabase
        val query = """
            SELECT a.* 
            FROM $TABLE_ARTWORK a 
            INNER JOIN $TABLE_WISHLIST w ON a.$KEY_ID = w.$WISHLIST_ARTWORK_ID 
            WHERE w.$WISHLIST_BUYER_EMAIL = ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(buyerEmail))
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
                    artistPhone = "",
                    originalPrice = it.getFloat(it.getColumnIndexOrThrow(KEY_ORIGINAL_PRICE)),
                    discountedPrice = it.getFloat(it.getColumnIndexOrThrow(KEY_DISCOUNTED_PRICE))
                )
                wishlist.add(artwork)
            }
        }
        cursor.close()
        db.close()
        return wishlist
    }

    /** ======================= Purchase Methods ======================= */

    fun addPurchase(buyerEmail: String, artworkId: Int, purchaseDate: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(PURCHASE_BUYER_EMAIL, buyerEmail)
            put(PURCHASE_ARTWORK_ID, artworkId)
            put(PURCHASE_DATE, purchaseDate)
        }
        val id = db.insert(TABLE_PURCHASES, null, values)
        db.close()
        return id
    }

    fun getPurchaseHistory(buyerEmail: String): List<ArtworkWithPhone> {
        val purchases = mutableListOf<ArtworkWithPhone>()
        val db = readableDatabase
        val query = """
            SELECT a.* 
            FROM $TABLE_ARTWORK a 
            INNER JOIN $TABLE_PURCHASES p ON a.$KEY_ID = p.$PURCHASE_ARTWORK_ID 
            WHERE p.$PURCHASE_BUYER_EMAIL = ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(buyerEmail))
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
                    artistPhone = "",
                    originalPrice = it.getFloat(it.getColumnIndexOrThrow(KEY_ORIGINAL_PRICE)),
                    discountedPrice = it.getFloat(it.getColumnIndexOrThrow(KEY_DISCOUNTED_PRICE))
                )
                purchases.add(artwork)
            }
        }
        cursor.close()
        db.close()
        return purchases
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
            put(COLUMN_BIO, "")
            put(COLUMN_SOCIAL_MEDIA, "")
        }
        val result = db.insertOrThrow(TABLE_USERS, null, values)
        db.close()
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
            Quadruple(true, username, profileImagePath, userType)
        } else {
            cursor.close()
            db.close()
            Quadruple(false, null, null, null)
        }
    }

    fun updateProfileImage(email: String, imagePath: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PROFILE_IMAGE, imagePath)
        }
        db.update(TABLE_USERS, values, "$COLUMN_EMAIL = ?", arrayOf(email))
        db.close()
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
    val artistPhone: String,
    val originalPrice: Float = 0f,
    val discountedPrice: Float = 0f
)

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)