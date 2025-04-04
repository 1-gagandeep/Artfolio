package com.example.artfolio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ArtworkViewModel : ViewModel() {
    private val _wishlist = MutableLiveData<List<ArtworkWithPhone>>()
    val wishlist: LiveData<List<ArtworkWithPhone>> get() = _wishlist

    private val _allArt = MutableLiveData<List<ArtworkWithPhone>>()
    val allArt: LiveData<List<ArtworkWithPhone>> get() = _allArt

    fun setWishlist(wishlist: List<ArtworkWithPhone>) {
        _wishlist.value = wishlist
    }

    fun setAllArt(allArt: List<ArtworkWithPhone>) {
        _allArt.value = allArt
    }
}

