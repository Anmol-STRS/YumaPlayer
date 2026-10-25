/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.settings.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.canvas.CanvasSource
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CanvasSettingsViewModel @Inject constructor(
    private val repository: CanvasSettingsRepository,
) : ViewModel() {
    private val isBusy = MutableStateFlow(false)
    private val cacheBytes = MutableStateFlow(0L)
    private val health = MutableStateFlow(CanvasHealthStatus())
    private val errorMessageRes = MutableStateFlow<Int?>(null)
    private val isScreenActive = MutableStateFlow(false)

    private var healthCheckJob: Job? = null
    private var lastCheckedSource: CanvasSource? = null
    private var lastCheckedEnabled: Boolean = false

    val uiState: StateFlow<CanvasSettingsUiState> =
        combine(
            repository.configuration,
            cacheBytes,
            health,
            isBusy,
            errorMessageRes,
        ) { config, bytes, healthStatus, busy, errorRes ->
            CanvasSettingsUiState(
                enabled = config.enabled,
                source = config.source,
                cacheLimitMb = config.cacheLimitMb,
                cacheBytes = bytes,
                health = healthStatus,
                isBusy = busy,
                errorMessageRes = errorRes,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CanvasSettingsUiState(),
        )

    init {
        updateCacheSize()
        viewModelScope.launch {
            repository.configuration
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    Timber.e(exception, "Failed to observe canvas configuration")
                    errorMessageRes.value = R.string.canvas_settings_load_failed
                }.collect { config ->
                    if (!config.enabled) {
                        healthCheckJob?.cancel()
                        lastCheckedEnabled = false
                        lastCheckedSource = null
                        health.value =
                            CanvasHealthStatus(
                                betterLyrics = CanvasHealth.DISABLED,
                                appleMusic = CanvasHealth.DISABLED,
                                tidal = CanvasHealth.DISABLED,
                            )
                    } else if (isScreenActive.value) {
                        if (!lastCheckedEnabled || lastCheckedSource != config.source) {
                            checkHealth(enabled = true, selectedSource = config.source)
                        }
                    }
                }
        }
    }

    fun setScreenActive(active: Boolean) {
        isScreenActive.value = active
        if (!active) {
            healthCheckJob?.cancel()
        } else {
            val current = uiState.value
            if (current.enabled) {
                checkHealth(enabled = true, selectedSource = current.source)
            }
        }
    }

    fun onErrorDismissed() {
        errorMessageRes.value = null
    }

    fun onIntent(intent: CanvasSettingsUiIntent) {
        when (intent) {
            is CanvasSettingsUiIntent.SetEnabled -> setEnabled(intent.enabled)
            is CanvasSettingsUiIntent.SelectSource -> setSource(intent.source)
            is CanvasSettingsUiIntent.SetCacheLimit -> setCacheLimit(intent.limitMb)
            CanvasSettingsUiIntent.ClearCache -> clearCache()
            CanvasSettingsUiIntent.RefreshHealth -> {
                val current = uiState.value
                if (isScreenActive.value) {
                    checkHealth(enabled = current.enabled, selectedSource = current.source)
                }
            }
        }
    }

    private fun setEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.setEnabled(enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to update canvas enabled setting")
                errorMessageRes.value = R.string.canvas_settings_update_failed
            }
        }
    }

    private fun setSource(source: CanvasSource) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.setSource(source)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to update canvas source setting")
                errorMessageRes.value = R.string.canvas_settings_update_failed
            }
        }
    }

    private fun setCacheLimit(limitMb: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.setCacheLimit(limitMb)
                updateCacheSize()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to update canvas cache limit")
                errorMessageRes.value = R.string.canvas_settings_update_failed
            }
        }
    }

    private fun clearCache() {
        if (isBusy.value) return
        isBusy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleared = repository.clearCache()
                if (!cleared) {
                    Timber.w("Canvas cache clearAndPersist returned false")
                    errorMessageRes.value = R.string.canvas_settings_update_failed
                }
                updateCacheSize()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear canvas cache")
                errorMessageRes.value = R.string.canvas_settings_update_failed
            } finally {
                isBusy.value = false
            }
        }
    }

    private fun updateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cacheBytes.value = repository.getCacheBytes()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to calculate canvas cache size")
                errorMessageRes.value = R.string.canvas_settings_load_failed
            }
        }
    }

    private fun checkHealth(
        enabled: Boolean,
        selectedSource: CanvasSource,
    ) {
        healthCheckJob?.cancel()
        if (!enabled || !isScreenActive.value) {
            lastCheckedEnabled = false
            lastCheckedSource = null
            health.value =
                CanvasHealthStatus(
                    betterLyrics = CanvasHealth.DISABLED,
                    appleMusic = CanvasHealth.DISABLED,
                    tidal = CanvasHealth.DISABLED,
                )
            return
        }

        lastCheckedEnabled = true
        lastCheckedSource = selectedSource

        healthCheckJob =
            viewModelScope.launch(Dispatchers.IO) {
                fun initialStatus(provider: CanvasSource): CanvasHealth =
                    if (selectedSource.accepts(provider)) CanvasHealth.CHECKING else CanvasHealth.NOT_SELECTED

                health.value =
                    CanvasHealthStatus(
                        betterLyrics = initialStatus(CanvasSource.BETTER_LYRICS),
                        appleMusic = initialStatus(CanvasSource.APPLE_MUSIC),
                        tidal = initialStatus(CanvasSource.TIDAL),
                    )

                try {
                    val status = repository.checkHealth(selectedSource)
                    health.value = status
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "Health check failed")
                    health.value =
                        CanvasHealthStatus(
                            betterLyrics = CanvasHealth.UNAVAILABLE,
                            appleMusic = CanvasHealth.UNAVAILABLE,
                            tidal = CanvasHealth.UNAVAILABLE,
                        )
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        healthCheckJob?.cancel()
    }
}
