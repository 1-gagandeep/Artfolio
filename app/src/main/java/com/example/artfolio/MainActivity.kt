package com.example.artfolio

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArtworkAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var pickProfileImageLauncher: ActivityResultLauncher<String>
    private lateinit var profileUsername: TextView
    private lateinit var profileImage: ImageView
    private lateinit var sharedPreferences: SharedPreferences

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

        // Initialize profile views
        profileUsername = findViewById(R.id.profileUsername)
        profileImage = findViewById(R.id.profileImage)

        // Get user data from intent
        val username = intent.getStringExtra("USERNAME")
        val profileImagePath = intent.getStringExtra("PROFILE_IMAGE_PATH")
        val email = intent.getStringExtra("EMAIL") // Assuming email is passed from SigninActivity

        // Save email to SharedPreferences
        if (email != null) {
            sharedPreferences.edit().putString(KEY_EMAIL, email).apply()
        }

        // Set username
        if (username != null) {
            profileUsername.text = username
        } else {
            profileUsername.text = "Guest"
        }

        // Set profile image if available
        if (profileImagePath != null) {
            Glide.with(this).load(profileImagePath).into(profileImage)
        }

        // Initialize profile image picker
        pickProfileImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                profileImage.setImageBitmap(bitmap)
                val newProfileImagePath = saveImageToInternalStorage(it)
                // Update profile image in database
                val currentEmail = sharedPreferences.getString(KEY_EMAIL, null)
                if (currentEmail != null) {
                    dbHelper.updateProfileImage(currentEmail, newProfileImagePath)
                }
            }
        }

        // Make profile image clickable
        profileImage.setOnClickListener {
            pickProfileImageLauncher.launch("image/*")
        }

        // Setup RecyclerView
        recyclerView = findViewById<RecyclerView>(R.id.artworkRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ArtworkAdapter(
            onEditClick = { artwork -> showEditDialog(artwork) },
            onDeleteClick = { artwork -> deleteArtwork(artwork) },
            onViewClick = { artwork -> showArtworkDetails(artwork) }
        )
        recyclerView.adapter = adapter

        // Initialize artwork image picker
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

        findViewById<FloatingActionButton>(R.id.fabUpload).setOnClickListener {
            showUploadDialog()
        }
    }

    private fun loadArtworks() {
        val artworks = dbHelper.getAllArtworks()
        adapter.submitList(artworks)
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

                if (title.isEmpty() || description.isEmpty() || medium.isEmpty() ||
                    style.isEmpty() || theme.isEmpty() || selectedImageUri == null) {
                    Toast.makeText(this, "All fields and image are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val imagePath = saveImageToInternalStorage(selectedImageUri!!)
                val artwork = Artwork(
                    title = title,
                    description = description,
                    imagePath = imagePath,
                    medium = medium,
                    style = style,
                    theme = theme,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight
                )
                dbHelper.addArtwork(artwork)
                loadArtworks()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun deleteArtwork(artwork: Artwork) {
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

    private fun showArtworkDetails(artwork: Artwork) {
        val intent = Intent(this, ArtworkDetailActivity::class.java).apply {
            putExtra("IMAGE_PATH", artwork.imagePath)
            putExtra("TITLE", artwork.title)
            putExtra("DESCRIPTION", artwork.description)
        }
        startActivity(intent)
    }

    private fun showEditDialog(artwork: Artwork) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_artwork, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val etMedium = dialogView.findViewById<EditText>(R.id.etMedium)
        val etStyle = dialogView.findViewById<EditText>(R.id.etStyle)
        val etTheme = dialogView.findViewById<EditText>(R.id.etTheme)
        ivPreview = dialogView.findViewById(R.id.ivArtworkPreview)
        val btnPickImage = dialogView.findViewById<Button>(R.id.btnPickImage)
        tvImageSize = dialogView.findViewById(R.id.tvImageSize)

        etTitle.setText(artwork.title)
        etDescription.setText(artwork.description)
        etMedium.setText(artwork.medium)
        etStyle.setText(artwork.style)
        etTheme.setText(artwork.theme)

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
                    imagePath = updatedImagePath
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