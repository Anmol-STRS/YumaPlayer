/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.cipher

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class CipherBridgeTest {

    @Before
    @After
    fun cleanup() {
        CipherBridge.resetForTesting()
    }

    @Test
    fun defaultRhinoNeverCallsQuickJsFactory() = runTest {
        val rhinoFactoryCalls = AtomicInteger(0)
        val quickJsFactoryCalls = AtomicInteger(0)

        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            rhinoFactoryCalls.incrementAndGet()
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> = Result.success(100)
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("rhino_url")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("rhino_n")
            }
        }
        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            quickJsFactoryCalls.incrementAndGet()
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> = Result.success(200)
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("qjs_url")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("qjs_n")
            }
        }

        val result = CipherBridge.signatureTimestamp("testVideo")

        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrNull())
        assertEquals(1, rhinoFactoryCalls.get())
        assertEquals(0, quickJsFactoryCalls.get())
        assertEquals(CipherEngineType.RHINO, CipherBridge.getSelectedEngineType())
    }

    @Test
    fun registrationIsInertUntilOperationExecution() = runTest {
        val factoryCalls = AtomicInteger(0)
        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            factoryCalls.incrementAndGet()
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> = Result.success(100)
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("url")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("n")
            }
        }

        assertEquals(0, factoryCalls.get())

        CipherBridge.signatureTimestamp("video1")
        assertEquals(1, factoryCalls.get())

        CipherBridge.signatureTimestamp("video2")
        assertEquals(1, factoryCalls.get())
    }

    @Test(expected = IllegalStateException::class)
    fun duplicateRegistrationThrowsAndPreservesExisting() = runTest {
        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> = Result.success(1)
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("v1")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("v1")
            }
        }
        assertEquals(1, CipherBridge.signatureTimestamp("test").getOrNull())

        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> = Result.success(2)
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("v2")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("v2")
            }
        }
    }

    @Test
    fun explicitSelectionActivatesQuickJsOnly() = runTest {
        val rhinoCalls = AtomicInteger(0)
        val quickCalls = AtomicInteger(0)

        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    rhinoCalls.incrementAndGet()
                    return Result.success(1)
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("rhino")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("rhino")
            }
        }
        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    quickCalls.incrementAndGet()
                    return Result.success(2)
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("quick")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("quick")
            }
        }

        CipherBridge.selectEngine(CipherEngineType.QUICKJS)
        assertEquals(CipherEngineType.QUICKJS, CipherBridge.getSelectedEngineType())

        val result = CipherBridge.signatureTimestamp("test")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
        assertEquals(0, rhinoCalls.get())
        assertEquals(1, quickCalls.get())
    }

    @Test
    fun selectDuringSuspendedOpPreservesCapturedBackend() = runTest {
        val suspendedStarted = CompletableDeferred<Unit>()
        val allowComplete = CompletableDeferred<Unit>()

        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> = Result.success(1)
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> {
                    suspendedStarted.complete(Unit)
                    allowComplete.await()
                    return Result.success("from_rhino")
                }
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("rhino")
            }
        }
        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> = Result.success(2)
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("from_quickjs")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("quickjs")
            }
        }

        val op = async {
            CipherBridge.resolveStreamUrl("video", "cipher")
        }

        suspendedStarted.await()

        CipherBridge.selectEngine(CipherEngineType.QUICKJS)
        assertEquals(CipherEngineType.QUICKJS, CipherBridge.getSelectedEngineType())

        allowComplete.complete(Unit)
        val result = op.await()

        assertTrue(result.isSuccess)
        assertEquals("from_rhino", result.getOrNull())
    }

    @Test
    fun backendAndFactoryExceptionsAreCapturedIntoResultFailureWithoutRetryingRhino() = runTest {
        val rhinoCalled = AtomicBoolean(false)

        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    rhinoCalled.set(true)
                    return Result.success(1)
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("rhino")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("rhino")
            }
        }

        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    throw IllegalStateException("Uncaught backend crash")
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("quick")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("quick")
            }
        }

        CipherBridge.selectEngine(CipherEngineType.QUICKJS)
        val result = CipherBridge.signatureTimestamp("test")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("Uncaught backend crash", result.exceptionOrNull()?.message)
        assertFalse(rhinoCalled.get())
        assertEquals(CipherEngineType.QUICKJS, CipherBridge.getSelectedEngineType())
    }

    @Test
    fun factoryExceptionReturnsFailureForEveryOperation() = runTest {
        val failure = IllegalStateException("Factory failed")
        CipherBridge.registerFactory(CipherEngineType.QUICKJS) { throw failure }
        CipherBridge.selectEngine(CipherEngineType.QUICKJS)

        assertEquals(failure, CipherBridge.signatureTimestamp("video").exceptionOrNull())
        assertEquals(failure, CipherBridge.resolveStreamUrl("video", "cipher").exceptionOrNull())
        assertEquals(failure, CipherBridge.transformN("video", "url").exceptionOrNull())
        assertEquals(CipherEngineType.QUICKJS, CipherBridge.getSelectedEngineType())
    }

    @Test
    fun missingFactoryWhenQuickJsSelectedReturnsResultFailureWithoutRetryingRhino() = runTest {
        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> = Result.success(1)
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("rhino")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("rhino")
            }
        }

        CipherBridge.selectEngine(CipherEngineType.QUICKJS)
        val result = CipherBridge.signatureTimestamp("test")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(CipherEngineType.QUICKJS, CipherBridge.getSelectedEngineType())
    }

    @Test(expected = CancellationException::class)
    fun cancellationExceptionPropagatesOutward() = runTest {
        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    currentCoroutineContext().cancel()
                    throw CancellationException("Operation cancelled")
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("")
            }
        }

        CipherBridge.signatureTimestamp("test")
    }
}
