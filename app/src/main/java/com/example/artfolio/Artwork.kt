package com.example.artfolio

import android.os.Parcel
import android.os.Parcelable

data class Artwork(
    val id: Int = 0,
    val title: String,
    val description: String,
    val imagePath: String,
    val medium: String,
    val style: String,
    val theme: String,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        title = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        imagePath = parcel.readString() ?: "",
        medium = parcel.readString() ?: "",
        style = parcel.readString() ?: "",
        theme = parcel.readString() ?: "",
        imageWidth = parcel.readInt(),
        imageHeight = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(imagePath)
        parcel.writeString(medium)
        parcel.writeString(style)
        parcel.writeString(theme)
        parcel.writeInt(imageWidth)
        parcel.writeInt(imageHeight)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Artwork> {
        override fun createFromParcel(parcel: Parcel): Artwork = Artwork(parcel)
        override fun newArray(size: Int): Array<Artwork?> = arrayOfNulls(size)
    }
}