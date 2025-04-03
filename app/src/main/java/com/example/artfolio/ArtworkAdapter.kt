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
import kotlin.math.roundToInt

class ArtworkAdapter(
    private val userType: String,
    private val onEditClick: (ArtworkWithPhone) -> Unit,
    private val onDeleteClick: (ArtworkWithPhone) -> Unit,
    private val onViewClick: (ArtworkWithPhone) -> Unit,
    private val onBuyClick: (ArtworkWithPhone) -> Unit
) : ListAdapter<ArtworkWithPhone, ArtworkAdapter.ArtworkViewHolder>(ArtworkDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtworkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_artwork, parent, false)
        return ArtworkViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtworkViewHolder, position: Int) {
        val artwork = getItem(position)
        holder.bind(artwork, userType, onEditClick, onDeleteClick, onViewClick, onBuyClick)
    }

    class ArtworkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivArtwork: ImageView = itemView.findViewById(R.id.ivArtwork)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        private val tvOriginalPrice: TextView = itemView.findViewById(R.id.tvOriginalPrice)
        private val tvDiscountedPrice: TextView = itemView.findViewById(R.id.tvDiscountedPrice)
        private val tvDiscountPercentage: TextView = itemView.findViewById(R.id.tvDiscountPercentage)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        private val btnBuy: Button = itemView.findViewById(R.id.btnBuy)

        fun bind(
            artwork: ArtworkWithPhone,
            userType: String,
            onEditClick: (ArtworkWithPhone) -> Unit,
            onDeleteClick: (ArtworkWithPhone) -> Unit,
            onViewClick: (ArtworkWithPhone) -> Unit,
            onBuyClick: (ArtworkWithPhone) -> Unit
        ) {
            tvTitle.text = artwork.title
            tvDescription.text = artwork.description
            Glide.with(itemView.context).load(artwork.imagePath).into(ivArtwork)

            if (userType == "buyer") {
                tvPhone.visibility = View.VISIBLE
                tvPhone.text = "Phone: ${artwork.artistPhone}"
                tvOriginalPrice.visibility = View.VISIBLE
                tvDiscountedPrice.visibility = View.VISIBLE
                tvDiscountPercentage.visibility = View.VISIBLE
                btnBuy.visibility = View.VISIBLE
                btnEdit.visibility = View.GONE
                btnDelete.visibility = View.GONE

                tvOriginalPrice.text = "Rs.${artwork.originalPrice}"
                tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvDiscountedPrice.text = "Rs.${artwork.discountedPrice}"
                val discountPercentage = ((artwork.originalPrice - artwork.discountedPrice) / artwork.originalPrice * 100).roundToInt()
                tvDiscountPercentage.text = "$discountPercentage% OFF"

                btnBuy.setOnClickListener { onBuyClick(artwork) }
            } else { // artist
                tvPhone.visibility = View.GONE
                tvOriginalPrice.visibility = View.GONE
                tvDiscountedPrice.visibility = View.GONE
                tvDiscountPercentage.visibility = View.GONE
                btnBuy.visibility = View.GONE
                btnEdit.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE

                btnEdit.setOnClickListener { onEditClick(artwork) }
                btnDelete.setOnClickListener { onDeleteClick(artwork) }
            }

            ivArtwork.setOnClickListener { onViewClick(artwork) }
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
}