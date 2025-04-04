package com.example.artfolio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class AllArtFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArtworkAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var viewModel: ArtworkViewModel
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userEmail = arguments?.getString("EMAIL")
        dbHelper = DatabaseHelper(requireContext())
        viewModel = ViewModelProvider(requireActivity()).get(ArtworkViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_all_art, container, false)
        recyclerView = view.findViewById(R.id.allArtRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = ArtworkAdapter(
            "buyer",
            onEditClick = {},
            onDeleteClick = {},
            onViewClick = { artwork -> showArtworkDetails(artwork) },
            onBuyClick = { artwork ->
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                dbHelper.addPurchase(userEmail ?: "", artwork.id, date)
                Toast.makeText(context, "Purchased ${artwork.title} for $${artwork.discountedPrice}", Toast.LENGTH_LONG).show()
                loadAllArt()
                viewModel.setWishlist(dbHelper.getWishlist(userEmail ?: ""))
            },
            onWishlistClick = { artwork ->
                val isInWishlist = dbHelper.getWishlist(userEmail ?: "").any { it.id == artwork.id }
                if (isInWishlist) {
                    dbHelper.removeFromWishlist(userEmail ?: "", artwork.id)
                    Toast.makeText(context, "${artwork.title} removed from wishlist", Toast.LENGTH_SHORT).show()
                } else {
                    dbHelper.addToWishlist(userEmail ?: "", artwork.id)
                    Toast.makeText(context, "${artwork.title} added to wishlist", Toast.LENGTH_SHORT).show()
                }
                loadAllArt()
                viewModel.setWishlist(dbHelper.getWishlist(userEmail ?: "")) // Sync wishlist
            },
            wishlistIds = dbHelper.getWishlist(userEmail ?: "").map { it.id },
            alwaysShowWishlist = true
        )
        recyclerView.adapter = adapter

        // Observe changes in all art
        viewModel.allArt.observe(viewLifecycleOwner) { artworks ->
            adapter.submitList(artworks)
        }

        loadAllArt()
        return view
    }

    private fun loadAllArt() {
        val artworks = dbHelper.getArtworks("buyer", userEmail)
        viewModel.setAllArt(artworks)
        adapter.updateWishlistIds(dbHelper.getWishlist(userEmail ?: "").map { it.id })
    }

    private fun showArtworkDetails(artwork: ArtworkWithPhone) {
        val intent = android.content.Intent(context, ArtworkDetailActivity::class.java).apply {
            putExtra("IMAGE_PATH", artwork.imagePath)
            putExtra("TITLE", artwork.title)
            putExtra("DESCRIPTION", artwork.description)
        }
        startActivity(intent)
    }
}

