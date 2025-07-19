package com.muyoma.thapab.models

import android.os.Parcelable
import com.muyoma.thapab.R
import kotlinx.parcelize.Parcelize

// Song data model
@Parcelize
data class Song(val id: Long, val title: String, val artist: String, val coverResId: Int = R.drawable.bg,
                val data: String ) : Parcelable
data class Playlist(val id: Long,val title : String, val songs : Int, val duration : Double,val thumbnail : Int )