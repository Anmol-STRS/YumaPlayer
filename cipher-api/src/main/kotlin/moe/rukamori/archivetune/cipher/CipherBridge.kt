/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.cipher

import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap

object CipherBridge : CipherEngine {
    private val factories = ConcurrentHashMap<CipherEngineType, () -> CipherEngine>()
    private val engines = ConcurrentHashMap<CipherEngineType, CipherEngine>()

    @Volatile
    private var selectedType: CipherEngineType = CipherEngineType.RHINO

    fun registerFactory(type: CipherEngineType, factory: () -> CipherEngine) {
        val existing = factories.putIfAbsent(type, factory)
        if (existing != null) {
            throw IllegalStateException("Factory for engine $type is already registered")
        }
    }

    fun selectEngine(type: CipherEngineType) {
        selectedType = type
    }

    fun getSelectedEngineType(): CipherEngineType = selectedType

    fun resetForTesting() {
        factories.clear()
        engines.clear()
        selectedType = CipherEngineType.RHINO
    }

    private fun currentEngine(): CipherEngine {
        val type = selectedType
        return engines.computeIfAbsent(type) { key ->
            val factory = factories[key]
                ?: throw IllegalStateException("Cipher engine $key is selected but no factory has been registered")
            factory()
        }
    }

    override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
        return try {
            val engine = currentEngine()
            engine.signatureTimestamp(videoId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> {
        return try {
            val engine = currentEngine()
            engine.resolveStreamUrl(videoId, signatureCipher)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun transformN(videoId: String, url: String): Result<String> {
        return try {
            val engine = currentEngine()
            engine.transformN(videoId, url)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
