/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.canvas

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import moe.rukamori.archivetune.canvas.models.CanvasArtwork
import moe.rukamori.archivetune.canvas.models.matchesSongIdentity
import java.util.Locale

object TidalCanvasProvider {
    private const val SEARCH_URL = "https://api.tidal.com/v1/search"
    private val coverIdPattern = Regex("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")

    @Volatile
    private var embedToken: String? = null

    fun initialize(token: String?) {
        embedToken = token?.trim()?.takeIf { it.isNotEmpty() }
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                connectTimeoutMillis = 12_000
                requestTimeoutMillis = 18_000
                socketTimeoutMillis = 18_000
            }
            expectSuccess = false
        }
    }

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        storefront: String = "us",
    ): CanvasArtwork? {
        val token = embedToken ?: return null
        return try {
            val results = search("$artist $song", storefront, 10, token)
            val items = results.tracks?.items ?: emptyList()
            for (track in items) {
                val album = track.album ?: continue
                val coverId = track.videoCoverId ?: track.videoCover ?: album.videoCoverId ?: album.videoCover
                val video = coverUrl(coverId, "videos", "1280x1280.mp4") ?: continue
                val artwork =
                    CanvasArtwork(
                        source = CanvasSource.TIDAL,
                        name = track.title,
                        artist = track.artists.joinToString(", ") { it.name },
                        albumName = album.title,
                        static = coverUrl(album.cover, "images", "640x640.jpg"),
                        videoUrl = video,
                    )
                if (artwork.matchesSongIdentity(song, artist)) return artwork
            }
            null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    suspend fun isHealthy(): Boolean {
        val token = embedToken ?: return false
        return try {
            val response = search("music", "US", 1, token)
            (response.tracks?.items?.size ?: 0) > 0
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun search(
        query: String,
        storefront: String,
        limit: Int,
        token: String,
    ): SearchResponse {
        return client.get(SEARCH_URL) {
            header("X-Tidal-Token", token)
            parameter("query", query)
            parameter("types", "TRACKS")
            parameter("countryCode", storefront.uppercase(Locale.ROOT))
            parameter("limit", limit)
        }.body<SearchResponse>()
    }

    private fun coverUrl(
        id: String?,
        kind: String,
        file: String,
    ): String? =
        id?.takeIf(coverIdPattern::matches)?.let {
            "https://resources.tidal.com/$kind/${it.replace('-', '/')}/$file"
        }

    @Serializable
    private data class SearchResponse(val tracks: TrackPage? = null)

    @Serializable
    private data class TrackPage(val items: List<Track> = emptyList())

    @Serializable
    private data class Track(
        val title: String,
        val artists: List<Artist> = emptyList(),
        val album: Album? = null,
        val videoCover: String? = null,
        val videoCoverId: String? = null,
    )

    @Serializable
    private data class Artist(val name: String)

    @Serializable
    private data class Album(
        val title: String? = null,
        val cover: String? = null,
        val videoCover: String? = null,
        val videoCoverId: String? = null,
    )
}
