package com.example.artfolio

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class ArtworkDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artwork_detail)

        val artwork = intent.getParcelableExtra<Artwork>("ARTWORK")
        if (artwork != null) {
            findViewById<TextView>(R.id.detailTitle).text = artwork.title
            findViewById<TextView>(R.id.detailDescription).text = artwork.description
            findViewById<TextView>(R.id.detailMedium).text = "Medium: ${artwork.medium}"
            findViewById<TextView>(R.id.detailStyle).text = "Style: ${artwork.style}"
            findViewById<TextView>(R.id.detailTheme).text = "Theme: ${artwork.theme}"
            findViewById<TextView>(R.id.detailImageSize).text = "Size: ${artwork.imageWidth}x${artwork.imageHeight}px"
            Glide.with(this)
                .load(artwork.imagePath)
                .into(findViewById(R.id.detailImage))
        }
    }
}