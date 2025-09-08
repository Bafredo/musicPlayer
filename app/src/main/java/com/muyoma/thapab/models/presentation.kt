package com.muyoma.thapab.models

import android.net.Uri
import android.os.Parcelable
import com.muyoma.thapab.R
import kotlinx.parcelize.Parcelize

// Song data model
@Parcelize
data class Song(
    val id: String, val title: String, val artist: String, val coverResId: Int = R.drawable.bg,
    val data: String,val albumArtUri: Uri? = null
) : Parcelable
data class Playlist(val id: Long,val title : String, val songs : Int,val thumbnail : Int )