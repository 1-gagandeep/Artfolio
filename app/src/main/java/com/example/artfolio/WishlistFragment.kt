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

class WishlistFragment : Fragment() {

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
        val view = inflater.inflate(R.layout.fragment_wishlist, container, false)
        recyclerView = view.findViewById(R.id.wishlistRecyclerView)
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
                loadWishlist()
                viewModel.setAllArt(dbHelper.getArtworks("buyer", userEmail))
            },
            onWishlistClick = { artwork ->
                dbHelper.removeFromWishlist(userEmail ?: "", artwork.id)
                Toast.makeText(context, "${artwork.title} removed from wishlist", Toast.LENGTH_SHORT).show()
                loadWishlist()
                viewModel.setAllArt(dbHelper.getArtworks("buyer", userEmail)) // Sync all art
            },
            wishlistIds = dbHelper.getWishlist(userEmail ?: "").map { it.id }
        )
        recyclerView.adapter = adapter

        // Observe changes in wishlist
        viewModel.wishlist.observe(viewLifecycleOwner) { wishlist ->
            adapter.submitList(wishlist)
            adapter.updateWishlistIds(wishlist.map { it.id })
        }

        loadWishlist()
        return view
    }

    private fun loadWishlist() {
        val wishlist = dbHelper.getWishlist(userEmail ?: "")
        viewModel.setWishlist(wishlist)
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

