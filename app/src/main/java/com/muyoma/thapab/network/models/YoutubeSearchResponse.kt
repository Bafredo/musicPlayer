package com.muyoma.thapab.network.models


import kotlinx.serialization.Serializable

@Serializable
data class YoutubeVideo(
    val title: String,
    val videoId: String,
    val url: String // This URL might be the direct download URL or similar
)

@Serializable
data class DownloadResponse(
    val message: String,
    val file: String // The filename of the downloaded MP3
)