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

    private fun getEngine(type: CipherEngineType): CipherEngine {
        return engines.computeIfAbsent(type) { key ->
            val factory = factories[key]
                ?: throw IllegalStateException("Cipher engine $key is selected but no factory has been registered")
            factory()
        }
    }

    private suspend fun <T> executeWithFallback(
        operation: suspend (CipherEngine) -> Result<T>,
    ): Result<T> {
        val primaryType = selectedType
        val primaryResult = executeOn(primaryType, operation)
        if (primaryResult.isSuccess || primaryType != CipherEngineType.QUICKJS) {
            return primaryResult
        }

        val fallbackResult = executeOn(CipherEngineType.RHINO, operation)
        if (fallbackResult.isSuccess) {
            return fallbackResult
        }

        val primaryError = primaryResult.exceptionOrNull()
        val fallbackError = fallbackResult.exceptionOrNull()
        if (fallbackError != null && primaryError != null && fallbackError !== primaryError) {
            fallbackError.addSuppressed(primaryError)
        }
        return fallbackResult
    }

    private suspend fun <T> executeOn(
        type: CipherEngineType,
        operation: suspend (CipherEngine) -> Result<T>,
    ): Result<T> {
        val engine = try {
            getEngine(type)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            return Result.failure(exception)
        }

        return try {
            operation(engine)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
        return executeWithFallback { engine ->
            engine.signatureTimestamp(videoId)
        }
    }

    override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> {
        return executeWithFallback { engine ->
            engine.resolveStreamUrl(videoId, signatureCipher)
        }
    }

    override suspend fun transformN(videoId: String, url: String): Result<String> {
        return executeWithFallback { engine ->
            engine.transformN(videoId, url)
        }
    }
}
