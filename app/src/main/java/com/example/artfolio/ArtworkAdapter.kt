package com.example.artfolio

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ArtworkAdapter(
    private val userType: String,
    private val onEditClick: (ArtworkWithPhone) -> Unit,
    private val onDeleteClick: (ArtworkWithPhone) -> Unit,
    private val onViewClick: (ArtworkWithPhone) -> Unit,
    private val onBuyClick: (ArtworkWithPhone) -> Unit,
    private val onWishlistClick: (ArtworkWithPhone) -> Unit,
    private var wishlistIds: List<Int> = emptyList(), // Changed to var for dynamic updates
    private val alwaysShowWishlist: Boolean = false
) : ListAdapter<ArtworkWithPhone, ArtworkAdapter.ArtworkViewHolder>(ArtworkDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtworkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_artwork, parent, false)
        return ArtworkViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtworkViewHolder, position: Int) {
        val artwork = getItem(position)
        holder.bind(artwork)
    }

    inner class ArtworkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivArtwork: ImageView = itemView.findViewById(R.id.ivArtwork)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        private val tvOriginalPrice: TextView = itemView.findViewById(R.id.tvOriginalPrice)
        private val tvDiscountedPrice: TextView = itemView.findViewById(R.id.tvDiscountedPrice)
        private val tvDiscountPercentage: TextView = itemView.findViewById(R.id.tvDiscountPercentage)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        private val btnWishlist: Button = itemView.findViewById(R.id.btnWishlist)
        private val btnBuy: Button = itemView.findViewById(R.id.btnBuy)

        fun bind(artwork: ArtworkWithPhone) {
            tvTitle.text = artwork.title
            tvDescription.text = artwork.description
            tvPhone.text = artwork.artistPhone
            tvOriginalPrice.text = "Rs.${artwork.originalPrice}"
            tvDiscountedPrice.text = "Rs.${artwork.discountedPrice}"
            val discount = if (artwork.originalPrice > 0) {
                ((artwork.originalPrice - artwork.discountedPrice) / artwork.originalPrice * 100).toInt()
            } else 0
            tvDiscountPercentage.text = "$discount% OFF"
            Glide.with(itemView.context).load(artwork.imagePath).into(ivArtwork)

            // Apply strikethrough to original price if there's a discount
            if (artwork.originalPrice > artwork.discountedPrice) {
                tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            if (userType == "artist") {
                btnEdit.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
                btnWishlist.visibility = View.GONE
                btnBuy.visibility = View.GONE
                tvOriginalPrice.visibility = View.VISIBLE
                tvDiscountedPrice.visibility = View.VISIBLE
                tvDiscountPercentage.visibility = if (discount > 0) View.VISIBLE else View.GONE
                btnEdit.setOnClickListener { onEditClick(artwork) }
                btnDelete.setOnClickListener { onDeleteClick(artwork) }
            } else { // Buyer
                btnEdit.visibility = View.GONE
                btnDelete.visibility = View.GONE
                btnWishlist.visibility = View.VISIBLE
                btnBuy.visibility = View.VISIBLE
                tvOriginalPrice.visibility = View.VISIBLE
                tvDiscountedPrice.visibility = View.VISIBLE
                tvDiscountPercentage.visibility = if (discount > 0) View.VISIBLE else View.GONE
                btnBuy.setOnClickListener { onBuyClick(artwork) }
            }

            // Wishlist button logic for buyer
            if (userType == "buyer") {
                val isInWishlist = wishlistIds.contains(artwork.id)
                if (alwaysShowWishlist) {
                    btnWishlist.text = "Wishlist" // Always "Wishlist" in All Art
                } else {
                    btnWishlist.text = if (isInWishlist) "Remove" else "Wishlist"
                }
                btnWishlist.setOnClickListener {
                    onWishlistClick(artwork)
                    // Update wishlistIds locally after click to reflect toggle
                    if (isInWishlist) {
                        wishlistIds = wishlistIds - artwork.id
                    } else {
                        wishlistIds = wishlistIds + artwork.id
                    }
                    bind(artwork) // Re-bind to update button text immediately
                }
            }

            itemView.setOnClickListener { onViewClick(artwork) }
        }
    }

    class ArtworkDiffCallback : DiffUtil.ItemCallback<ArtworkWithPhone>() {
        override fun areItemsTheSame(oldItem: ArtworkWithPhone, newItem: ArtworkWithPhone): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ArtworkWithPhone, newItem: ArtworkWithPhone): Boolean {
            return oldItem == newItem
        }
    }

    fun updateWishlistIds(newWishlistIds: List<Int>) {
        wishlistIds = newWishlistIds
        submitList(currentList) // Trigger rebind with new wishlist state
    }
}