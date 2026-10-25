/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.obfuscator.quickjs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class QuickJsCipherTest {

    @Test
    fun signatureTimestampExtractsValidTimestampWithoutRuntime() = runTest {
        val providerCalls = AtomicInteger(0)
        val provider = QuickJsPlayerScriptProvider {
            providerCalls.incrementAndGet()
            QuickJsPlayerScript(
                playerId = "testPlayer",
                source = "var a = { signatureTimestamp: 19842 };",
            )
        }
        val cipher = QuickJsCipher(
            context = null,
            playerScriptProvider = provider,
        )

        val result = cipher.signatureTimestamp("testVideo")

        assertTrue(result.isSuccess)
        assertEquals(19842, result.getOrNull())
        assertEquals(1, providerCalls.get())
        cipher.close()
    }

    @Test
    fun signatureTimestampFailsOnMalformedScript() = runTest {
        val provider = QuickJsPlayerScriptProvider {
            QuickJsPlayerScript(
                playerId = "testPlayer",
                source = "var a = {};",
            )
        }
        val cipher = QuickJsCipher(
            context = null,
            playerScriptProvider = provider,
        )

        val result = cipher.signatureTimestamp("testVideo")

        assertTrue(result.isFailure)
        cipher.close()
    }

    @Test
    fun transformNReturnsDirectUrlWithoutProviderCallsWhenNoNParam() = runTest {
        val providerCalls = AtomicInteger(0)
        val provider = QuickJsPlayerScriptProvider {
            providerCalls.incrementAndGet()
            QuickJsPlayerScript("test", "")
        }
        val cipher = QuickJsCipher(
            context = null,
            playerScriptProvider = provider,
        )

        val directUrl = "https://googlevideo.com/videoplayback?expire=12345&id=abc"
        val result = cipher.transformN("testVideo", directUrl)

        assertTrue(result.isSuccess)
        assertEquals(directUrl, result.getOrNull())
        assertEquals(0, providerCalls.get())
        cipher.close()
    }

    @Test
    fun resolveStreamUrlReturnsDirectUrlWithoutProviderCallsWhenNoCipherOrN() = runTest {
        val providerCalls = AtomicInteger(0)
        val provider = QuickJsPlayerScriptProvider {
            providerCalls.incrementAndGet()
            QuickJsPlayerScript("test", "")
        }
        val cipher = QuickJsCipher(
            context = null,
            playerScriptProvider = provider,
        )

        val input = "url=https%3A%2F%2Fgooglevideo.com%2Fvideoplayback%3Fexpire%3D123"
        val result = cipher.resolveStreamUrl("testVideo", input)

        assertTrue(result.isSuccess)
        assertEquals("https://googlevideo.com/videoplayback?expire=123", result.getOrNull())
        assertEquals(0, providerCalls.get())
        cipher.close()
    }

    @Test
    fun resolveStreamUrlFailsOnMalformedCipherMissingUrl() = runTest {
        val cipher = QuickJsCipher(
            context = null,
            playerScriptProvider = { QuickJsPlayerScript("test", "") },
        )

        val malformedCipher = "s=onlySignatureWithoutUrl&sp=signature"
        val result = cipher.resolveStreamUrl("testVideo", malformedCipher)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        cipher.close()
    }

    @Test
    fun resolveStreamUrlFailsOnBlankSignatureInCipher() = runTest {
        val cipher = QuickJsCipher(
            context = null,
            playerScriptProvider = { QuickJsPlayerScript("test", "") },
        )

        val blankSigCipher = "s=&sp=signature&url=https%3A%2F%2Fexample.com"
        val result = cipher.resolveStreamUrl("testVideo", blankSigCipher)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        cipher.close()
    }

    @Test
    fun resolveStreamUrlPreservesDirectUrlWithPlusAndPercentEncoding() = runTest {
        val cipher = QuickJsCipher(
            context = null,
            playerScriptProvider = { QuickJsPlayerScript("test", "") },
        )

        val directUrl = "https://googlevideo.com/videoplayback?expire=123&query=a%2Bb&nested=x%2520y"
        val result = cipher.resolveStreamUrl("testVideo", directUrl)

        assertTrue(result.isSuccess)
        assertEquals(directUrl, result.getOrNull())
        cipher.close()
    }

    @Test
    fun resolveStreamUrlFailsOnArbitraryTextNotUrl() = runTest {
        val cipher = QuickJsCipher(
            context = null,
            playerScriptProvider = { QuickJsPlayerScript("test", "") },
        )

        val notAUrl = "arbitraryNonUrlString"
        val result = cipher.resolveStreamUrl("testVideo", notAUrl)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        cipher.close()
    }

    @Test
    fun operationsFailExplicitlyAfterClose() = runTest {
        val cipher = QuickJsCipher(
            context = null,
            playerScriptProvider = {
                QuickJsPlayerScript(
                    playerId = "testPlayer",
                    source = "var a = { signatureTimestamp: 19842 };",
                )
            },
        )
        cipher.close()

        val stResult = cipher.signatureTimestamp("testVideo")
        assertTrue(stResult.isFailure)
        assertTrue(stResult.exceptionOrNull() is IllegalStateException)

        val nResult = cipher.transformN("testVideo", "https://googlevideo.com/videoplayback?expire=123")
        assertTrue(nResult.isFailure)
        assertTrue(nResult.exceptionOrNull() is IllegalStateException)

        val resolveResult = cipher.resolveStreamUrl("testVideo", "https://googlevideo.com/videoplayback?expire=123")
        assertTrue(resolveResult.isFailure)
        assertTrue(resolveResult.exceptionOrNull() is IllegalStateException)
    }

    @Test(expected = CancellationException::class)
    fun cancellationExceptionIsNotCaughtIntoResultFailure() = runTest {
        val provider = QuickJsPlayerScriptProvider {
            currentCoroutineContext().cancel()
            throw CancellationException("Cancelled")
        }
        val cipher = QuickJsCipher(
            context = null,
            playerScriptProvider = provider,
        )

        try {
            cipher.signatureTimestamp("testVideo")
        } finally {
            cipher.close()
        }
    }

    @Test
    fun queryParsingDecodesParametersAndExtractsCustomSp() {
        val query = "s=sig123&sp=custom_sig&url=https%3A%2F%2Fexample.com%3Fa%3D1%26b%3D2"
        val parsed = QuickJsCipher.parseFormEncoded(query)

        assertEquals("sig123", parsed["s"])
        assertEquals("custom_sig", parsed["sp"])
        assertEquals("https://example.com?a=1&b=2", parsed["url"])
    }
}
