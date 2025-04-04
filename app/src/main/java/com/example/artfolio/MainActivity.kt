package com.example.artfolio

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArtworkAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var pickProfileImageLauncher: ActivityResultLauncher<String>
    private lateinit var profileUsername: TextView
    private lateinit var profileBio: TextView
    private lateinit var profileSocialMedia: TextView
    private lateinit var profileImage: ImageView
    private lateinit var sharedPreferences: SharedPreferences
    private var userType: String? = null
    private var userEmail: String? = null

    private var selectedImageUri: Uri? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private lateinit var ivPreview: ImageView
    private lateinit var tvImageSize: TextView

    companion object {
        private const val PREFS_NAME = "UserPrefs"
        private const val KEY_EMAIL = "user_email"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Art Gallery"

        dbHelper = DatabaseHelper(this)
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        profileUsername = findViewById(R.id.profileUsername)
        profileBio = findViewById(R.id.profileBio)
        profileSocialMedia = findViewById(R.id.profileSocialMedia)
        profileImage = findViewById(R.id.profileImage)

        profileImage.outlineProvider = ViewOutlineProvider.BACKGROUND
        profileImage.clipToOutline = true

        userEmail = intent.getStringExtra("EMAIL")
        userType = intent.getStringExtra("USER_TYPE")
        val username = intent.getStringExtra("USERNAME")
        val profileImagePath = intent.getStringExtra("PROFILE_IMAGE_PATH")

        if (userEmail != null) {
            sharedPreferences.edit().putString(KEY_EMAIL, userEmail).apply()
            if (userType == "artist") {
                loadProfileDetails(userEmail!!) // Load bio and social media only for artists
                profileBio.visibility = View.VISIBLE
                profileSocialMedia.visibility = View.VISIBLE
            } else {
                profileBio.visibility = View.GONE
                profileSocialMedia.visibility = View.GONE
            }
        }

        if (username != null) {
            profileUsername.text = username
        } else {
            profileUsername.text = "Guest"
        }

        if (profileImagePath != null) {
            Glide.with(this)
                .load(profileImagePath)
                .apply(RequestOptions.circleCropTransform())
                .into(profileImage)
        }

        pickProfileImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                profileImage.setImageBitmap(bitmap)
                val newProfileImagePath = saveImageToInternalStorage(it)
                if (userEmail != null) {
                    dbHelper.updateProfileImage(userEmail!!, newProfileImagePath)
                }
                profileImage.outlineProvider = ViewOutlineProvider.BACKGROUND
                profileImage.clipToOutline = true
            }
        }

        profileImage.setOnClickListener {
            if (userType == "artist") {
                showArtistProfile(userEmail ?: "")
            } else {
                showBuyerProfile(userEmail ?: "")
            }
        }

        recyclerView = findViewById<RecyclerView>(R.id.artworkRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val wishlistIds = if (userType == "buyer" && userEmail != null) {
            dbHelper.getWishlist(userEmail!!).map { it.id }
        } else {
            emptyList()
        }

        adapter = ArtworkAdapter(
            userType ?: "buyer",
            onEditClick = { artwork -> if (userType == "artist") showEditDialog(artwork) },
            onDeleteClick = { artwork -> if (userType == "artist") deleteArtwork(artwork) },
            onViewClick = { artwork -> showArtworkDetails(artwork) },
            onBuyClick = { artwork ->
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                dbHelper.addPurchase(userEmail ?: "", artwork.id, date)
                Toast.makeText(this, "Purchased ${artwork.title} for $${artwork.discountedPrice}", Toast.LENGTH_LONG).show()
            },
            onWishlistClick = { artwork ->
                if (userType == "buyer" && userEmail != null) {
                    val isInWishlist = dbHelper.getWishlist(userEmail!!).any { it.id == artwork.id }
                    if (isInWishlist) {
                        dbHelper.removeFromWishlist(userEmail!!, artwork.id)
                        Toast.makeText(this, "${artwork.title} removed from wishlist", Toast.LENGTH_SHORT).show()
                    } else {
                        dbHelper.addToWishlist(userEmail!!, artwork.id)
                        Toast.makeText(this, "${artwork.title} added to wishlist", Toast.LENGTH_SHORT).show()
                    }
                    loadArtworks()
                }
            },
            wishlistIds = wishlistIds
        )
        recyclerView.adapter = adapter

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                imageWidth = bitmap.width
                imageHeight = bitmap.height
                ivPreview.setImageBitmap(bitmap)
                tvImageSize.text = "Image Size: ${imageWidth}x${imageHeight}px"
            }
        }

        loadArtworks()

        val fabUpload = findViewById<FloatingActionButton>(R.id.fabUpload)
        if (userType == "buyer") {
            fabUpload.visibility = View.GONE
        }
        fabUpload.setOnClickListener {
            if (userType == "artist") {
                showUploadDialog()
            } else {
                Toast.makeText(this, "Only artists can upload artwork!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfileDetails(email: String) {
        val (bio, socialMedia, _) = dbHelper.getUserProfile(email)
        profileBio.text = "Bio: ${bio ?: "Not set"}"
        profileSocialMedia.text = "Social Media: ${socialMedia ?: "Not set"}"
    }

    private fun loadArtworks() {
        val artworks = dbHelper.getArtworks(userType ?: "buyer", userEmail)
        val wishlistIds = if (userType == "buyer" && userEmail != null) {
            dbHelper.getWishlist(userEmail!!).map { it.id }
        } else {
            emptyList()
        }
        adapter.submitList(artworks)
        adapter.updateWishlistIds(wishlistIds)
    }

    private fun showUploadDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_artwork, null)
        ivPreview = dialogView.findViewById(R.id.ivArtworkPreview)
        val btnPickImage = dialogView.findViewById<Button>(R.id.btnPickImage)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val etMedium = dialogView.findViewById<EditText>(R.id.etMedium)
        val etStyle = dialogView.findViewById<EditText>(R.id.etStyle)
        val etTheme = dialogView.findViewById<EditText>(R.id.etTheme)
        val etOriginalPrice = dialogView.findViewById<EditText>(R.id.etOriginalPrice)
        val etDiscountedPrice = dialogView.findViewById<EditText>(R.id.etDiscountedPrice)
        tvImageSize = dialogView.findViewById(R.id.tvImageSize)

        selectedImageUri = null
        ivPreview.setImageDrawable(null)
        tvImageSize.text = ""

        btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add Artwork")
            .setPositiveButton("Save") { _, _ ->
                val title = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                val medium = etMedium.text.toString().trim()
                val style = etStyle.text.toString().trim()
                val theme = etTheme.text.toString().trim()
                val originalPrice = etOriginalPrice.text.toString().toFloatOrNull() ?: 0f
                val discountedPrice = etDiscountedPrice.text.toString().toFloatOrNull() ?: 0f

                if (title.isEmpty() || description.isEmpty() || medium.isEmpty() ||
                    style.isEmpty() || theme.isEmpty() || selectedImageUri == null ||
                    originalPrice <= 0 || discountedPrice <= 0 || discountedPrice > originalPrice) {
                    Toast.makeText(this, "All fields are required, and discounted price must be less than original price", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val imagePath = saveImageToInternalStorage(selectedImageUri!!)
                val artwork = ArtworkWithPhone(
                    title = title,
                    description = description,
                    imagePath = imagePath,
                    medium = medium,
                    style = style,
                    theme = theme,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    artistEmail = userEmail ?: "",
                    artistPhone = "",
                    originalPrice = originalPrice,
                    discountedPrice = discountedPrice
                )
                dbHelper.addArtwork(artwork, userEmail ?: "")
                loadArtworks()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showArtistProfile(email: String) {
        val (bio, socialMedia, mobileNo) = dbHelper.getUserProfile(email)
        val pastSales = dbHelper.getPastSales(email)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_artist_profile, null)
        val tvBio = dialogView.findViewById<TextView>(R.id.tvBio)
        val tvSocialMedia = dialogView.findViewById<TextView>(R.id.tvSocialMedia)
        val tvMobileNo = dialogView.findViewById<TextView>(R.id.tvMobileNo)
        val rvPastSales = dialogView.findViewById<RecyclerView>(R.id.rvPastSales)
        val etBio = dialogView.findViewById<EditText>(R.id.etBio)
        val etSocialMedia = dialogView.findViewById<EditText>(R.id.etSocialMedia)

        tvBio.text = bio ?: "No bio available"
        tvSocialMedia.text = socialMedia ?: "No social media links"
        tvMobileNo.text = "Mobile: ${mobileNo ?: "Not available"}"
        etBio.setText(bio ?: "")
        etSocialMedia.setText(socialMedia ?: "")

        rvPastSales.layoutManager = LinearLayoutManager(this)
        rvPastSales.adapter = ArtworkAdapter(
            "artist",
            onEditClick = {},
            onDeleteClick = {},
            onViewClick = { artwork -> showArtworkDetails(artwork) },
            onBuyClick = {},
            onWishlistClick = {}
        ).apply { submitList(pastSales) }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Artist Profile")
            .setPositiveButton("Update") { _, _ ->
                val newBio = etBio.text.toString().trim()
                val newSocialMedia = etSocialMedia.text.toString().trim()
                dbHelper.updateUserProfile(email, newBio, newSocialMedia)
                loadProfileDetails(email) // Refresh bio and social media on main screen
                pickProfileImageLauncher.launch("image/*")
            }
            .setNegativeButton("Close", null)
            .create()
        dialog.show()
    }

    private fun showBuyerProfile(email: String) {
        val wishlist = dbHelper.getWishlist(email)
        val purchases = dbHelper.getPurchaseHistory(email)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_buyer_profile, null)
        val rvWishlist = dialogView.findViewById<RecyclerView>(R.id.rvWishlist)
        val rvPurchases = dialogView.findViewById<RecyclerView>(R.id.rvPurchases)

        rvWishlist.layoutManager = LinearLayoutManager(this)
        val wishlistAdapter = ArtworkAdapter(
            "buyer",
            onEditClick = {},
            onDeleteClick = {},
            onViewClick = { artwork -> showArtworkDetails(artwork) },
            onBuyClick = { artwork ->
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                dbHelper.addPurchase(email, artwork.id, date)
            },
            onWishlistClick = { artwork ->
                dbHelper.removeFromWishlist(email, artwork.id)
                Toast.makeText(this, "${artwork.title} removed from wishlist", Toast.LENGTH_SHORT).show()
                showBuyerProfile(email) // Refresh dialog
            },
            wishlistIds = wishlist.map { it.id }
        )
        rvWishlist.adapter = wishlistAdapter
        wishlistAdapter.submitList(wishlist)

        rvPurchases.layoutManager = LinearLayoutManager(this)
        rvPurchases.adapter = ArtworkAdapter(
            "buyer",
            onEditClick = {},
            onDeleteClick = {},
            onViewClick = { artwork -> showArtworkDetails(artwork) },
            onBuyClick = {},
            onWishlistClick = {}
        ).apply { submitList(purchases) }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Buyer Profile")
            .setPositiveButton("Change Profile Picture") { _, _ ->
                pickProfileImageLauncher.launch("image/*")
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun deleteArtwork(artwork: ArtworkWithPhone) {
        AlertDialog.Builder(this)
            .setTitle("Delete Artwork")
            .setMessage("Are you sure you want to delete this artwork?")
            .setPositiveButton("Yes") { _, _ ->
                dbHelper.deleteArtwork(artwork.id)
                loadArtworks()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showArtworkDetails(artwork: ArtworkWithPhone) {
        val intent = Intent(this, ArtworkDetailActivity::class.java).apply {
            putExtra("IMAGE_PATH", artwork.imagePath)
            putExtra("TITLE", artwork.title)
            putExtra("DESCRIPTION", artwork.description)
        }
        startActivity(intent)
    }

    private fun showEditDialog(artwork: ArtworkWithPhone) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_artwork, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val etMedium = dialogView.findViewById<EditText>(R.id.etMedium)
        val etStyle = dialogView.findViewById<EditText>(R.id.etStyle)
        val etTheme = dialogView.findViewById<EditText>(R.id.etTheme)
        val etOriginalPrice = dialogView.findViewById<EditText>(R.id.etOriginalPrice)
        val etDiscountedPrice = dialogView.findViewById<EditText>(R.id.etDiscountedPrice)
        ivPreview = dialogView.findViewById(R.id.ivArtworkPreview)
        val btnPickImage = dialogView.findViewById<Button>(R.id.btnPickImage)
        tvImageSize = dialogView.findViewById(R.id.tvImageSize)

        etTitle.setText(artwork.title)
        etDescription.setText(artwork.description)
        etMedium.setText(artwork.medium)
        etStyle.setText(artwork.style)
        etTheme.setText(artwork.theme)
        etOriginalPrice.setText(artwork.originalPrice.toString())
        etDiscountedPrice.setText(artwork.discountedPrice.toString())

        Glide.with(this).load(artwork.imagePath).into(ivPreview)
        tvImageSize.text = "Image Size: ${artwork.imageWidth}x${artwork.imageHeight}px"

        btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Edit Artwork")
            .setPositiveButton("Update") { _, _ ->
                val updatedTitle = etTitle.text.toString().trim()
                val updatedDescription = etDescription.text.toString().trim()
                val updatedMedium = etMedium.text.toString().trim()
                val updatedStyle = etStyle.text.toString().trim()
                val updatedTheme = etTheme.text.toString().trim()
                val updatedOriginalPrice = etOriginalPrice.text.toString().toFloatOrNull() ?: 0f
                val updatedDiscountedPrice = etDiscountedPrice.text.toString().toFloatOrNull() ?: 0f

                if (updatedTitle.isEmpty() || updatedDescription.isEmpty() || updatedMedium.isEmpty() ||
                    updatedStyle.isEmpty() || updatedTheme.isEmpty() || updatedOriginalPrice <= 0 ||
                    updatedDiscountedPrice <= 0 || updatedDiscountedPrice > updatedOriginalPrice) {
                    Toast.makeText(this, "All fields are required, and discounted price must be less than original price", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedImagePath = if (selectedImageUri != null) {
                    saveImageToInternalStorage(selectedImageUri!!)
                } else {
                    artwork.imagePath
                }

                val updatedArtwork = artwork.copy(
                    title = updatedTitle,
                    description = updatedDescription,
                    medium = updatedMedium,
                    style = updatedStyle,
                    theme = updatedTheme,
                    imagePath = updatedImagePath,
                    originalPrice = updatedOriginalPrice,
                    discountedPrice = updatedDiscountedPrice
                )
                dbHelper.updateArtwork(updatedArtwork)
                loadArtworks()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun saveImageToInternalStorage(imageUri: Uri): String {
        val file = File(filesDir, "${System.currentTimeMillis()}.jpg")
        contentResolver.openInputStream(imageUri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }
}