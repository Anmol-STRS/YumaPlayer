/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.settings.canvas

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import moe.rukamori.archivetune.canvas.AppleMusicProvider
import moe.rukamori.archivetune.canvas.ArchiveTuneCanvas
import moe.rukamori.archivetune.canvas.CanvasSource
import moe.rukamori.archivetune.canvas.TidalCanvasProvider
import moe.rukamori.archivetune.constants.ArchiveTuneCanvasKey
import moe.rukamori.archivetune.constants.CanvasSourceKey
import moe.rukamori.archivetune.constants.MaxCanvasCacheSizeKey
import moe.rukamori.archivetune.ui.player.CanvasArtworkPlaybackCache
import moe.rukamori.archivetune.utils.dataStore
import moe.rukamori.archivetune.utils.isInternetAvailable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class CanvasSettingsConfiguration(
    val enabled: Boolean,
    val source: CanvasSource,
    val cacheLimitMb: Int,
)

@Singleton
class CanvasSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val configuration: Flow<CanvasSettingsConfiguration> =
        context.dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                emit(emptyPreferences())
            }.map { preferences ->
                val rawSource = CanvasSource.fromPreference(preferences[CanvasSourceKey])
                val safeSource = if (rawSource == CanvasSource.SPOTIFY) CanvasSource.ALL else rawSource
                CanvasSettingsConfiguration(
                    enabled = preferences[ArchiveTuneCanvasKey] ?: false,
                    source = safeSource,
                    cacheLimitMb = (preferences[MaxCanvasCacheSizeKey] ?: CanvasArtworkPlaybackCache.DEFAULT_MAX_SIZE_MEGABYTES).coerceAtLeast(-1),
                )
            }.distinctUntilChanged()

    suspend fun setEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            context.dataStore.edit { it[ArchiveTuneCanvasKey] = enabled }
        }
    }

    suspend fun setSource(source: CanvasSource) {
        val safeSource = if (source == CanvasSource.SPOTIFY) CanvasSource.ALL else source
        withContext(Dispatchers.IO) {
            context.dataStore.edit { it[CanvasSourceKey] = safeSource.name }
        }
    }

    suspend fun setCacheLimit(limitMb: Int) {
        withContext(Dispatchers.IO) {
            context.dataStore.edit { it[MaxCanvasCacheSizeKey] = limitMb }
            CanvasArtworkPlaybackCache.setMaxSize(limitMb)
        }
    }

    suspend fun getCacheBytes(): Long =
        withContext(Dispatchers.IO) {
            CanvasArtworkPlaybackCache.byteSize()
        }

    suspend fun clearCache(): Boolean =
        withContext(Dispatchers.IO) {
            CanvasArtworkPlaybackCache.clearAndPersist()
        }

    fun isOnline(): Boolean = isInternetAvailable(context)

    suspend fun checkHealth(selectedSource: CanvasSource): CanvasHealthStatus =
        withContext(Dispatchers.IO) {
            if (!isOnline()) {
                return@withContext CanvasHealthStatus(
                    betterLyrics = CanvasHealth.OFFLINE,
                    appleMusic = CanvasHealth.OFFLINE,
                    tidal = CanvasHealth.OFFLINE,
                )
            }

            coroutineScope {
                val betterLyrics =
                    if (selectedSource.accepts(CanvasSource.BETTER_LYRICS)) {
                        async { checkProvider { ArchiveTuneCanvas.isHealthy() } }
                    } else {
                        null
                    }

                val appleMusic =
                    if (selectedSource.accepts(CanvasSource.APPLE_MUSIC)) {
                        async { checkProvider { AppleMusicProvider.isHealthy() } }
                    } else {
                        null
                    }

                val tidal =
                    if (selectedSource.accepts(CanvasSource.TIDAL)) {
                        async { checkProvider { TidalCanvasProvider.isHealthy() } }
                    } else {
                        null
                    }

                CanvasHealthStatus(
                    betterLyrics = betterLyrics?.await() ?: CanvasHealth.NOT_SELECTED,
                    appleMusic = appleMusic?.await() ?: CanvasHealth.NOT_SELECTED,
                    tidal = tidal?.await() ?: CanvasHealth.NOT_SELECTED,
                )
            }
        }

    private suspend fun checkProvider(block: suspend () -> Boolean): CanvasHealth =
        try {
            if (withTimeoutOrNull(20_000L) { block() } == true) {
                CanvasHealth.AVAILABLE
            } else {
                CanvasHealth.UNAVAILABLE
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Provider health check failed")
            CanvasHealth.UNAVAILABLE
        }
}
