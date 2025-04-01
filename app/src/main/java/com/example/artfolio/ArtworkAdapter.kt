package com.example.artfolio

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

//class ArtworkAdapter(
//    private val onEditClick: (Artwork) -> Unit,
//    private val onDeleteClick: (Artwork) -> Unit,
//    private val onViewClick: (Artwork) -> Unit
//) : ListAdapter<Artwork, ArtworkAdapter.ViewHolder>(ArtworkDiffCallback()) {
//
//    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val imageView: ImageView = itemView.findViewById(R.id.artworkImage)
//        val titleText: TextView = itemView.findViewById(R.id.artworkTitle)
//        val descText: TextView = itemView.findViewById(R.id.artworkDescription)
//        val editButton: Button = itemView.findViewById(R.id.btnEdit)
//        val deleteButton: Button = itemView.findViewById(R.id.btnDelete)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_artwork, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val artwork = getItem(position)
//        holder.titleText.text = artwork.title
//        holder.descText.text = artwork.description
//        Glide.with(holder.itemView.context)
//            .load(artwork.imagePath)
//            .into(holder.imageView)
//
//        holder.editButton.setOnClickListener { onEditClick(artwork) }
//        holder.deleteButton.setOnClickListener { onDeleteClick(artwork) }
//        holder.imageView.setOnClickListener { onViewClick(artwork) }
//    }
//}
//
//class ArtworkDiffCallback : DiffUtil.ItemCallback<Artwork>() {
//    override fun areItemsTheSame(oldItem: Artwork, newItem: Artwork) = oldItem.id == newItem.id
//    override fun areContentsTheSame(oldItem: Artwork, newItem: Artwork) = oldItem == newItem
//}

class ArtworkAdapter(
    private val onEditClick: (Artwork) -> Unit,
    private val onDeleteClick: (Artwork) -> Unit,
    private val onViewClick: (Artwork) -> Unit
) : ListAdapter<Artwork, ArtworkAdapter.ArtworkViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtworkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_artwork, parent, false)
        return ArtworkViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtworkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ArtworkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val artworkImage: ImageView = view.findViewById(R.id.artworkImage)
        private val artworkTitle: TextView = view.findViewById(R.id.artworkTitle)
        private val artworkDescription: TextView = view.findViewById(R.id.artworkDescription)
        private val btnEdit: Button = view.findViewById(R.id.btnEdit)
        private val btnDelete: Button = view.findViewById(R.id.btnDelete)

        fun bind(artwork: Artwork) {
            artworkTitle.text = artwork.title
            artworkDescription.text = artwork.description
            Glide.with(itemView.context).load(artwork.imagePath).into(artworkImage)

            btnEdit.setOnClickListener { onEditClick(artwork) }
            btnDelete.setOnClickListener { onDeleteClick(artwork) }
            itemView.setOnClickListener { onViewClick(artwork) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Artwork>() {
        override fun areItemsTheSame(oldItem: Artwork, newItem: Artwork) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Artwork, newItem: Artwork) = oldItem == newItem
    }
}
