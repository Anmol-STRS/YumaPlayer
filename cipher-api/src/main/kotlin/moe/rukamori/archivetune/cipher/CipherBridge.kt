/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026) | Original work by © Rukamori
 * GPL-3.0 License | Contributors: see git history
 */

package moe.rukamori.archivetune.cipher

import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap

object CipherBridge : CipherEngine {
    private val factories = ConcurrentHashMap<CipherEngineType, () -> CipherEngine>()
    private val engines = ConcurrentHashMap<CipherEngineType, CipherEngine>()

    @Volatile
    private var selectedType: CipherEngineType = CipherEngineType.RHINO

    @Volatile
    private var cachedSignatureTimestamp: Int? = null

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

    fun invalidateTimestampCache() {
        cachedSignatureTimestamp = null
    }

    fun resetForTesting() {
        factories.clear()
        engines.clear()
        selectedType = CipherEngineType.RHINO
        cachedSignatureTimestamp = null
    }

    private fun getEngine(type: CipherEngineType): CipherEngine? {
        return engines[type] ?: factories[type]?.invoke()?.let { engine ->
            engines.putIfAbsent(type, engine) ?: engine
        }
    }

    private fun fallbackTypeFor(primary: CipherEngineType): CipherEngineType {
        return when (primary) {
            CipherEngineType.QUICKJS -> CipherEngineType.RHINO
            CipherEngineType.RHINO -> CipherEngineType.QUICKJS
        }
    }

    private suspend fun <T> executeWithFallback(
        operation: suspend (CipherEngine) -> Result<T>
    ): Result<T> {
        val primaryType = selectedType
        val primaryEngine = getEngine(primaryType)

        val primaryResult = if (primaryEngine != null) {
            try {
                operation(primaryEngine)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(IllegalStateException("Primary engine $primaryType is not registered"))
        }

        if (primaryResult.isSuccess) {
            return primaryResult
        }

        val fallbackType = fallbackTypeFor(primaryType)
        val fallbackEngine = getEngine(fallbackType) ?: return primaryResult

        return try {
            operation(fallbackEngine)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
        cachedSignatureTimestamp?.let { return Result.success(it) }

        val result = executeWithFallback { engine ->
            engine.signatureTimestamp(videoId)
        }

        result.getOrNull()?.let {
            cachedSignatureTimestamp = it
        }

        return result
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