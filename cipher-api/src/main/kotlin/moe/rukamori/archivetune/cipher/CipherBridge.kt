/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.cipher

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
        operationName: String,
        operation: suspend (CipherEngine) -> Result<T>,
    ): Result<T> {
        val primaryType = selectedType
        val primaryResult = executeOn(primaryType, operationName, operation)
        currentCoroutineContext().ensureActive()
        if (primaryResult.isSuccess || primaryType != CipherEngineType.QUICKJS) {
            return primaryResult
        }

        CipherTrace.log(
            "FALLBACK",
            "operation=$operationName from=$primaryType to=${CipherEngineType.RHINO}",
        )

        val fallbackResult = executeOn(CipherEngineType.RHINO, operationName, operation)
        currentCoroutineContext().ensureActive()
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
        operationName: String,
        operation: suspend (CipherEngine) -> Result<T>,
    ): Result<T> {
        val startNanos = System.nanoTime()
        var loggedCancelled = false
        val result: Result<T> = try {
            val engine = getEngine(type)
            val opResult = operation(engine)
            val failure = opResult.exceptionOrNull()
            if (failure is CancellationException) {
                loggedCancelled = true
                CipherTrace.log("CANCELLED", "operation=$operationName engine=$type")
                throw failure
            }
            opResult
        } catch (cancellation: CancellationException) {
            if (!loggedCancelled) {
                loggedCancelled = true
                CipherTrace.log("CANCELLED", "operation=$operationName engine=$type")
            }
            throw cancellation
        } catch (exception: Exception) {
            Result.failure(exception)
        }

        try {
            currentCoroutineContext().ensureActive()
        } catch (cancellation: CancellationException) {
            if (!loggedCancelled) {
                loggedCancelled = true
                CipherTrace.log("CANCELLED", "operation=$operationName engine=$type")
            }
            throw cancellation
        }

        val durationMs = (System.nanoTime() - startNanos) / 1_000_000L
        if (result.isSuccess) {
            CipherTrace.log(
                "ENGINE_RESULT",
                "operation=$operationName engine=$type success=true durationMs=$durationMs",
            )
        } else {
            val failure = result.exceptionOrNull()
            val failureClass = failure?.javaClass?.simpleName ?: "Unknown"
            CipherTrace.log(
                "ENGINE_RESULT",
                "operation=$operationName engine=$type success=false durationMs=$durationMs failure=$failureClass",
            )
        }
        return result
    }

    override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
        return executeWithFallback("ST") { engine ->
            engine.signatureTimestamp(videoId)
        }
    }

    override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> {
        return executeWithFallback("SIGNATURE") { engine ->
            engine.resolveStreamUrl(videoId, signatureCipher)
        }
    }

    override suspend fun transformN(videoId: String, url: String): Result<String> {
        return executeWithFallback("N_TRANSFORM") { engine ->
            engine.transformN(videoId, url)
        }
    }
}
