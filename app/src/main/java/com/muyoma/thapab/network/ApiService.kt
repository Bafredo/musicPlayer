// app/src/main/java/com/muyoma/thapab/network/ApiService.kt
package com.muyoma.thapab.network

import com.muyoma.thapab.network.models.DownloadResponse
import com.muyoma.thapab.network.models.YoutubeVideo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.LogLevel // Correct LogLevel import
import io.ktor.client.plugins.logging.Logger // Correct Logger import
import io.ktor.client.plugins.logging.Logging // Correct Logging plugin import
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import android.util.Log // For Android's Log

// Base URL for your Node.js backend
const val BASE_URL = "https://muyoma.site"

interface ApiService {
    suspend fun searchYoutubeVideo(query: String): YoutubeVideo

    suspend fun downloadAudio(videoId: String, title: String): DownloadResponse

    // Function to get the full streaming URL (not making a request here, just constructing it)
    fun getStreamUrl(filename: String): String
}

class ApiServiceImpl : ApiService {

    private val httpClient = HttpClient(Android) {
        // Configure JSON serialization
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // Ignore fields in JSON that are not in data class
                prettyPrint = true
                isLenient = true
            })
        }

        // Configure Logging (helpful for debugging network requests)
        install(Logging) {
            // Use Ktor's Logger interface
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("KtorClient", message) // Use Android's Log
                }
            }
            level = LogLevel.ALL // Log everything (headers, body, etc.)
        }

        // Default request settings
        defaultRequest {
            url(BASE_URL)
            headers {
                append("Accept", "application/json")
            }
            // Add a timeout for requests to prevent indefinite waits
//            timeout {
//                requestTimeoutMillis = 15000 // 15 seconds
//                connectTimeoutMillis = 15000 // 15 seconds
//                socketTimeoutMillis = 15000 // 15 seconds
//            }
        }
    }

    override suspend fun searchYoutubeVideo(query: String): YoutubeVideo {
        return httpClient.get("search") {
            parameter("q", query)
        }.body()
    }

    override suspend fun downloadAudio(videoId: String, title: String): DownloadResponse {
        return httpClient.get("download") {
            parameter("videoId", videoId)
            parameter("title", title)
        }.body()
    }

    override fun getStreamUrl(filename: String): String {
        return "$BASE_URL/stream/$filename"
    }
}