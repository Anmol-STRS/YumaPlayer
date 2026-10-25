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
        CipherTrace.sink = null
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
    fun explicitQuickJsSelectionExecutesQuickJsAndSkipsRhinoOnSuccess() = runTest {
        val rhinoCalled = AtomicBoolean(false)
        val quickCalled = AtomicBoolean(false)

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
                    quickCalled.set(true)
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
        assertTrue(quickCalled.get())
        assertFalse(rhinoCalled.get())
        assertEquals(CipherEngineType.QUICKJS, CipherBridge.getSelectedEngineType())
    }

    @Test
    fun failedQuickJsBackendFallsBackToRhinoOnce() = runTest {
        val rhinoCalls = AtomicInteger(0)
        val quickCalls = AtomicInteger(0)

        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    quickCalls.incrementAndGet()
                    return Result.failure(IllegalStateException("QuickJS decipher failed"))
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("quick")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("quick")
            }
        }
        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    rhinoCalls.incrementAndGet()
                    return Result.success(999)
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("rhino")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("rhino")
            }
        }

        CipherBridge.selectEngine(CipherEngineType.QUICKJS)
        val result = CipherBridge.signatureTimestamp("test")

        assertTrue(result.isSuccess)
        assertEquals(999, result.getOrNull())
        assertEquals(1, quickCalls.get())
        assertEquals(1, rhinoCalls.get())
        assertEquals(CipherEngineType.QUICKJS, CipherBridge.getSelectedEngineType())
    }

    @Test
    fun failedQuickJsFactoryFallsBackToRhinoOnce() = runTest {
        val rhinoCalls = AtomicInteger(0)

        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            throw IllegalStateException("QuickJS initialization failed")
        }
        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    rhinoCalls.incrementAndGet()
                    return Result.success(888)
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("rhino")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("rhino")
            }
        }

        CipherBridge.selectEngine(CipherEngineType.QUICKJS)
        val result = CipherBridge.signatureTimestamp("test")

        assertTrue(result.isSuccess)
        assertEquals(888, result.getOrNull())
        assertEquals(1, rhinoCalls.get())
        assertEquals(CipherEngineType.QUICKJS, CipherBridge.getSelectedEngineType())
    }

    @Test
    fun failedRhinoDirectlyNeverTouchesQuickJs() = runTest {
        val quickFactoryCalls = AtomicInteger(0)

        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    return Result.failure(IllegalStateException("Rhino failed"))
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("rhino")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("rhino")
            }
        }
        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            quickFactoryCalls.incrementAndGet()
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> = Result.success(1)
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("quick")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("quick")
            }
        }

        val result = CipherBridge.signatureTimestamp("test")

        assertTrue(result.isFailure)
        assertEquals("Rhino failed", result.exceptionOrNull()?.message)
        assertEquals(0, quickFactoryCalls.get())
    }

    @Test
    fun bothEnginesFailReturnsFailurePreservingSuppressedPrimaryCause() = runTest {
        val primaryError = IllegalStateException("Primary QuickJS crash")
        val fallbackError = RuntimeException("Fallback Rhino crash")

        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    throw primaryError
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("quick")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("quick")
            }
        }
        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    throw fallbackError
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("rhino")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("rhino")
            }
        }

        CipherBridge.selectEngine(CipherEngineType.QUICKJS)
        val result = CipherBridge.signatureTimestamp("test")

        assertTrue(result.isFailure)
        val failure = result.exceptionOrNull()
        assertEquals(fallbackError, failure)
        assertTrue(failure?.suppressed?.contains(primaryError) == true)
    }

    @Test(expected = CancellationException::class)
    fun cancellationInPrimaryPropagatesImmediatelyWithoutFallback() = runTest {
        val rhinoCalled = AtomicBoolean(false)

        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    currentCoroutineContext().cancel()
                    throw CancellationException("Primary cancelled")
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("")
            }
        }
        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    rhinoCalled.set(true)
                    return Result.success(1)
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("")
            }
        }

        CipherBridge.selectEngine(CipherEngineType.QUICKJS)
        try {
            CipherBridge.signatureTimestamp("test")
        } finally {
            assertFalse(rhinoCalled.get())
        }
    }

    @Test
    fun selectDuringSuspendedPrimaryPreservesCapturedBackendAndFallbackPolicy() = runTest {
        val suspendedStarted = CompletableDeferred<Unit>()
        val allowComplete = CompletableDeferred<Unit>()

        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> = Result.success(1)
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> {
                    suspendedStarted.complete(Unit)
                    allowComplete.await()
                    return Result.failure(IllegalStateException("QuickJS failed after resume"))
                }
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("quick")
            }
        }
        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> = Result.success(2)
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("rhino_fallback_resolved")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("rhino")
            }
        }

        CipherBridge.selectEngine(CipherEngineType.QUICKJS)

        val op = async {
            CipherBridge.resolveStreamUrl("video", "cipher")
        }

        suspendedStarted.await()

        // Switch engine to RHINO mid-operation
        CipherBridge.selectEngine(CipherEngineType.RHINO)
        assertEquals(CipherEngineType.RHINO, CipherBridge.getSelectedEngineType())

        allowComplete.complete(Unit)
        val result = op.await()

        // The operation captured QUICKJS as primary, which failed and cleanly invoked RHINO fallback!
        assertTrue(result.isSuccess)
        assertEquals("rhino_fallback_resolved", result.getOrNull())
    }

    @Test
    fun traceLogsEngineResultAndFallbackOrderOnTransition() = runTest {
        val traceEvents = mutableListOf<String>()
        CipherTrace.sink = { traceEvents.add(it) }

        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    return Result.failure(IllegalStateException("QJS failed"))
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("qjs")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("qjs")
            }
        }
        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> = Result.success(42)
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("rhino")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("rhino")
            }
        }

        CipherBridge.selectEngine(CipherEngineType.QUICKJS)
        val result = CipherBridge.signatureTimestamp("vid")

        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())

        assertEquals(3, traceEvents.size)
        assertTrue(traceEvents[0].startsWith("CipherTrace | ENGINE_RESULT operation=ST engine=QUICKJS success=false"))
        assertTrue(traceEvents[0].contains("failure=IllegalStateException"))
        assertFalse(traceEvents[0].contains("QJS failed"))
        assertEquals("CipherTrace | FALLBACK operation=ST from=QUICKJS to=RHINO", traceEvents[1])
        assertTrue(traceEvents[2].startsWith("CipherTrace | ENGINE_RESULT operation=ST engine=RHINO success=true"))
    }

    @Test
    fun traceDoesNotLogFallbackWhenPrimarySucceeds() = runTest {
        val traceEvents = mutableListOf<String>()
        CipherTrace.sink = { traceEvents.add(it) }

        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> = Result.success(10)
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("qjs")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("qjs")
            }
        }

        CipherBridge.selectEngine(CipherEngineType.QUICKJS)
        val result = CipherBridge.signatureTimestamp("vid")

        assertTrue(result.isSuccess)
        assertEquals(1, traceEvents.size)
        assertTrue(traceEvents[0].startsWith("CipherTrace | ENGINE_RESULT operation=ST engine=QUICKJS success=true"))
        assertTrue(traceEvents.none { it.contains("FALLBACK") })
    }

    @Test(expected = CancellationException::class)
    fun returnedCancellationExceptionDoesNotTriggerFallbackAndLogsCancelled() = runTest {
        val traceEvents = mutableListOf<String>()
        CipherTrace.sink = { traceEvents.add(it) }
        val rhinoCalled = AtomicBoolean(false)

        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    return Result.failure(CancellationException("Returned cancel"))
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("")
            }
        }
        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    rhinoCalled.set(true)
                    return Result.success(1)
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("")
            }
        }

        CipherBridge.selectEngine(CipherEngineType.QUICKJS)
        try {
            CipherBridge.signatureTimestamp("vid")
        } finally {
            assertFalse(rhinoCalled.get())
            assertTrue(traceEvents.any { it == "CipherTrace | CANCELLED operation=ST engine=QUICKJS" })
            assertTrue(traceEvents.none { it.contains("FALLBACK") })
        }
    }

    @Test(expected = CancellationException::class)
    fun cancelledContextReturningNormalFailureDoesNotTriggerFallbackAndLogsCancelled() = runTest {
        val traceEvents = mutableListOf<String>()
        CipherTrace.sink = { traceEvents.add(it) }
        val rhinoCalled = AtomicBoolean(false)

        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    currentCoroutineContext().cancel()
                    return Result.failure(IllegalStateException("Normal failure during cancellation"))
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("")
            }
        }
        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            object : CipherEngine {
                override suspend fun signatureTimestamp(videoId: String): Result<Int?> {
                    rhinoCalled.set(true)
                    return Result.success(1)
                }
                override suspend fun resolveStreamUrl(videoId: String, signatureCipher: String): Result<String> = Result.success("")
                override suspend fun transformN(videoId: String, url: String): Result<String> = Result.success("")
            }
        }

        CipherBridge.selectEngine(CipherEngineType.QUICKJS)
        try {
            CipherBridge.signatureTimestamp("vid")
        } finally {
            assertFalse(rhinoCalled.get())
            assertTrue(traceEvents.any { it == "CipherTrace | CANCELLED operation=ST engine=QUICKJS" })
            assertTrue(traceEvents.none { it.contains("FALLBACK") })
        }
    }
}
