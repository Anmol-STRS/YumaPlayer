/*
 * YumaPlayer (2026) | Original work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 */
package moe.rukamori.archivetune.cipher

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import moe.rukamori.archivetune.BuildConfig
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.YouTubeClient
import moe.rukamori.archivetune.innertube.models.response.PlayerResponse
import moe.rukamori.archivetune.obfuscator.quickjs.QuickJsCipher
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CipherManualInstrumentedTest {

    private data class AcquiredInputs(
        val signatureCipher: String,
        val nUrl: String,
        val isNUrlEmbeddedInCipher: Boolean,
        val clientName: String,
        val signatureItag: Int?,
        val nItag: Int?,
    )

    @Test
    fun testQuickJsCipherWithRealYouTubeData() {
        assertTrue("App build must be DEBUG", BuildConfig.DEBUG)
        assertTrue(
            "Expected active cipher engine to be QUICKJS",
            CipherBridge.getSelectedEngineType() == CipherEngineType.QUICKJS,
        )

        val args = InstrumentationRegistry.getArguments()
        val videoId = args.getString("cipher_video_id")?.trim()
        val manualCipher = args.getString("cipher_signature_cipher")?.trim()
        val manualNUrl = args.getString("cipher_n_url")?.trim()
        val checkAvailability = args.getString("cipher_check_availability")?.toBoolean() ?: true

        if (videoId.isNullOrEmpty()) {
            fail("Missing required instrumentation argument 'cipher_video_id'")
            return
        }
        val safeVideoId: String = videoId

        val hasManualCipher = !manualCipher.isNullOrEmpty()
        val hasManualNUrl = !manualNUrl.isNullOrEmpty()
        if (hasManualCipher xor hasManualNUrl) {
            fail("Partial manual inputs supplied; provide both cipher_signature_cipher and cipher_n_url or neither for auto acquisition")
            return
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val originalSink = CipherTrace.sink
        val capturedEvents = CopyOnWriteArrayList<String>()
        CipherTrace.sink = { message ->
            capturedEvents.add(message)
            originalSink?.invoke(message)
        }

        val testHttpClient = OkHttpClient.Builder()
            .apply {
                val proxy = YouTube.streamProxy
                if (proxy != null) {
                    proxy(proxy)
                }
            }
            .callTimeout(8, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        var provenanceClient = "MANUAL"
        var signatureItag: Int? = null
        var nItag: Int? = null

        var sigPassed = false
        var sigCategory = FailureCategory.UNKNOWN
        val sigDiagFrames = mutableListOf<String>()
        var sigAvailStatus = "SKIPPED"

        var nPassed = false
        var nCategory = FailureCategory.UNKNOWN
        val nDiagFrames = mutableListOf<String>()
        var nAvailStatus = "SKIPPED"

        var stConsistencyPassed = false

        try {
            val (safeSignatureCipher, safeNUrl, isNUrlEmbedded) = if (hasManualCipher && hasManualNUrl) {
                Triple(manualCipher, manualNUrl, false)
            } else {
                val acquired = runBlocking {
                    val result = withTimeoutOrNull(25_000L) {
                        capturedEvents.clear()
                        val firstStResult = CipherBridge.signatureTimestamp(safeVideoId)
                        currentCoroutineContext().ensureActive()
                        firstStResult.exceptionOrNull()?.let {
                            CipherDiagnosticHelper.findCancellation(it)?.let { cancel -> throw cancel }
                        }
                        val firstSt = firstStResult.getOrNull()
                        if (firstSt == null || firstSt <= 0) {
                            fail("INPUT_UNAVAILABLE reason=PLAYER_SCRIPT_UNVERIFIED")
                            return@withTimeoutOrNull null
                        }
                        val firstSnapshot = capturedEvents.toList()
                        assertTrue(
                            "ST failed QuickJS execution",
                            firstSnapshot.any { it.contains("ENGINE_RESULT") && it.contains("operation=ST") && it.contains("engine=QUICKJS") && it.contains("success=true") } &&
                                firstSnapshot.none { it.contains("FALLBACK") || it.contains("NEWPIPE_FALLBACK") },
                        )

                        val inputs = acquireRealInputs(safeVideoId, firstSt)

                        capturedEvents.clear()
                        val secondStResult = CipherBridge.signatureTimestamp(safeVideoId)
                        currentCoroutineContext().ensureActive()
                        secondStResult.exceptionOrNull()?.let {
                            CipherDiagnosticHelper.findCancellation(it)?.let { cancel -> throw cancel }
                        }
                        val secondSt = secondStResult.getOrNull()
                        if (secondSt == null || secondSt <= 0 || secondSt != firstSt) {
                            fail("INPUT_UNAVAILABLE reason=PLAYER_SCRIPT_UNVERIFIED")
                            return@withTimeoutOrNull null
                        }
                        val secondSnapshot = capturedEvents.toList()
                        assertTrue(
                            "Second ST failed QuickJS execution",
                            secondSnapshot.any { it.contains("ENGINE_RESULT") && it.contains("operation=ST") && it.contains("engine=QUICKJS") && it.contains("success=true") } &&
                                secondSnapshot.none { it.contains("FALLBACK") || it.contains("NEWPIPE_FALLBACK") },
                        )

                        stConsistencyPassed = true
                        inputs
                    }
                    currentCoroutineContext().ensureActive()
                    result
                }
                if (acquired == null) {
                    fail("INPUT_UNAVAILABLE reason=PLAYER_RESPONSE_TIMEOUT")
                    return
                }
                provenanceClient = acquired.clientName
                signatureItag = acquired.signatureItag
                nItag = acquired.nItag
                Triple(acquired.signatureCipher, acquired.nUrl, acquired.isNUrlEmbeddedInCipher)
            }

            val parsedCipher = QuickJsCipher.parseFormEncoded(safeSignatureCipher)
            val signatureParam = parsedCipher["s"]?.trim()
            if (signatureParam.isNullOrEmpty()) {
                fail("Supplied signatureCipher missing s parameter")
                return
            }

            val sourceHttpUrl = parsedCipher["url"]?.toHttpUrlOrNull()
            if (sourceHttpUrl == null) {
                fail("Supplied signatureCipher missing valid url parameter")
                return
            }

            val sp = parsedCipher["sp"]?.takeIf { it.isNotBlank() } ?: "signature"
            assertTrue(
                "Source URL already contains signature parameter",
                sourceHttpUrl.queryParameter(sp).isNullOrBlank(),
            )

            val parsedNUrl = safeNUrl.toHttpUrlOrNull()
            if (parsedNUrl == null) {
                fail("Supplied nUrl is not a valid HTTP URL")
                return
            }
            val initialN = parsedNUrl.queryParameter("n")?.trim()
            if (initialN.isNullOrEmpty()) {
                fail("Supplied nUrl missing n parameter")
                return
            }

            var resolvedSignatureValue: String? = null

            try {
                capturedEvents.clear()
                val sigResult = runBlocking {
                    CipherBridge.resolveStreamUrl(safeVideoId, safeSignatureCipher)
                }
                sigResult.exceptionOrNull()?.let {
                    CipherDiagnosticHelper.findCancellation(it)?.let { cancel -> throw cancel }
                }
                if (sigResult.isFailure) {
                    val ex = sigResult.exceptionOrNull()!!
                    sigCategory = CipherDiagnosticHelper.classifyFailure(ex, true)
                    sigDiagFrames.addAll(CipherDiagnosticHelper.extractSanitizedDiagnostics(ex))
                } else {
                    val resolvedUrl = sigResult.getOrNull()
                    assertTrue("Resolved URL must not be null", resolvedUrl != null)
                    val resolvedHttpUrl = resolvedUrl?.toHttpUrlOrNull()
                    assertTrue("Resolved URL is not a valid HTTP URL", resolvedHttpUrl != null)

                    assertTrue("Resolved URL did not change from direct URL", resolvedUrl != parsedCipher["url"])
                    assertTrue(
                        "Resolved URL missing signature parameter",
                        !resolvedHttpUrl?.queryParameter(sp).isNullOrBlank(),
                    )
                    assertTrue("Scheme changed", resolvedHttpUrl?.scheme == sourceHttpUrl.scheme)
                    assertTrue("Host changed", resolvedHttpUrl?.host == sourceHttpUrl.host)
                    assertTrue("Path changed", resolvedHttpUrl?.encodedPath == sourceHttpUrl.encodedPath)

                    val originalParams = sourceHttpUrl.queryParameterNames.filter { it != sp && it != "n" }
                    assertTrue(
                        "Original query parameters not preserved",
                        originalParams.all { sourceHttpUrl.queryParameterValues(it) == resolvedHttpUrl?.queryParameterValues(it) },
                    )

                    val sigSnapshot = capturedEvents.toList()
                    assertTrue(
                        "Missing ENGINE_RESULT event for SIGNATURE",
                        sigSnapshot.any { it.contains("ENGINE_RESULT") && it.contains("operation=SIGNATURE") && it.contains("engine=QUICKJS") && it.contains("success=true") },
                    )
                    assertTrue(
                        "Missing JS_EXECUTE event for SIGNATURE",
                        sigSnapshot.any { it.contains("JS_EXECUTE") && it.contains("operation=DECIPHER") && it.contains("engine=QUICKJS") },
                    )
                    assertTrue(
                        "Trace contains unexpected fallback",
                        sigSnapshot.none { it.contains("FALLBACK") || it.contains("NEWPIPE_FALLBACK") },
                    )

                    resolvedSignatureValue = resolvedHttpUrl?.queryParameter(sp)
                    sigPassed = true

                    if (checkAvailability) {
                        try {
                            verifyHttpAvailability(testHttpClient, resolvedUrl!!)
                            sigAvailStatus = "PASS"
                        } catch (e: Exception) {
                            CipherDiagnosticHelper.findCancellation(e)?.let { throw it }
                            sigAvailStatus = "FAIL"
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (assertion: AssertionError) {
                sigPassed = false
                sigCategory = FailureCategory.SIGNATURE_OUTPUT_MISSING
                sigDiagFrames.addAll(CipherDiagnosticHelper.extractSanitizedDiagnostics(assertion))
            } catch (e: Exception) {
                CipherDiagnosticHelper.findCancellation(e)?.let { throw it }
                sigPassed = false
                sigCategory = CipherDiagnosticHelper.classifyFailure(e, true)
                sigDiagFrames.addAll(CipherDiagnosticHelper.extractSanitizedDiagnostics(e))
            }

            try {
                capturedEvents.clear()
                val nResult = runBlocking {
                    CipherBridge.transformN(safeVideoId, safeNUrl)
                }
                nResult.exceptionOrNull()?.let {
                    CipherDiagnosticHelper.findCancellation(it)?.let { cancel -> throw cancel }
                }
                if (nResult.isFailure) {
                    val ex = nResult.exceptionOrNull()!!
                    nCategory = CipherDiagnosticHelper.classifyFailure(ex, false)
                    nDiagFrames.addAll(CipherDiagnosticHelper.extractSanitizedDiagnostics(ex))
                } else {
                    val transformedUrl = nResult.getOrNull()
                    assertTrue("Transformed URL must not be null", transformedUrl != null)
                    val transformedHttpUrl = transformedUrl?.toHttpUrlOrNull()
                    assertTrue("Transformed URL is not a valid HTTP URL", transformedHttpUrl != null)

                    assertTrue("Transformed URL did not change from input URL", transformedUrl != safeNUrl)
                    val transformedN = transformedHttpUrl?.queryParameter("n")
                    assertTrue("Transformed URL missing n parameter", !transformedN.isNullOrBlank())
                    assertTrue("Transformed n parameter unchanged", transformedN != initialN)
                    assertTrue("Scheme changed", transformedHttpUrl?.scheme == parsedNUrl.scheme)
                    assertTrue("Host changed", transformedHttpUrl?.host == parsedNUrl.host)
                    assertTrue("Path changed", transformedHttpUrl?.encodedPath == parsedNUrl.encodedPath)

                    val originalNParams = parsedNUrl.queryParameterNames.filter { it != "n" }
                    assertTrue(
                        "Original query parameters not preserved",
                        originalNParams.all { parsedNUrl.queryParameterValues(it) == transformedHttpUrl?.queryParameterValues(it) },
                    )

                    val nSnapshot = capturedEvents.toList()
                    assertTrue(
                        "Missing ENGINE_RESULT event for N_TRANSFORM",
                        nSnapshot.any { it.contains("ENGINE_RESULT") && it.contains("operation=N_TRANSFORM") && it.contains("engine=QUICKJS") && it.contains("success=true") },
                    )
                    assertTrue(
                        "Missing JS_EXECUTE event for N_TRANSFORM",
                        nSnapshot.any { it.contains("JS_EXECUTE") && it.contains("operation=DECIPHER") && it.contains("engine=QUICKJS") },
                    )
                    assertTrue(
                        "Trace contains unexpected fallback",
                        nSnapshot.none { it.contains("FALLBACK") || it.contains("NEWPIPE_FALLBACK") },
                    )

                    nPassed = true

                    if (checkAvailability) {
                        if (isNUrlEmbedded) {
                            if (resolvedSignatureValue.isNullOrBlank()) {
                                nAvailStatus = "UNAVAILABLE"
                            } else {
                                val nAvailabilityUrl = transformedHttpUrl!!.newBuilder()
                                    .removeAllQueryParameters(sp)
                                    .addQueryParameter(sp, resolvedSignatureValue)
                                    .build()
                                    .toString()
                                try {
                                    verifyHttpAvailability(testHttpClient, nAvailabilityUrl)
                                    nAvailStatus = "PASS"
                                } catch (e: Exception) {
                                    CipherDiagnosticHelper.findCancellation(e)?.let { throw it }
                                    nAvailStatus = "FAIL"
                                }
                            }
                        } else {
                            try {
                                verifyHttpAvailability(testHttpClient, transformedUrl!!)
                                nAvailStatus = "PASS"
                            } catch (e: Exception) {
                                CipherDiagnosticHelper.findCancellation(e)?.let { throw it }
                                nAvailStatus = "FAIL"
                            }
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (assertion: AssertionError) {
                nPassed = false
                nCategory = FailureCategory.N_OUTPUT_FAILURE
                nDiagFrames.addAll(CipherDiagnosticHelper.extractSanitizedDiagnostics(assertion))
            } catch (e: Exception) {
                CipherDiagnosticHelper.findCancellation(e)?.let { throw it }
                nPassed = false
                nCategory = CipherDiagnosticHelper.classifyFailure(e, false)
                nDiagFrames.addAll(CipherDiagnosticHelper.extractSanitizedDiagnostics(e))
            }

            val diskArtifact = CipherDiagnosticHelper.readDiskArtifact(context)

            val summaryBuilder = StringBuilder().apply {
                appendLine("=== CipherTest Diagnostic Summary ===")
                appendLine("INPUT_PROVENANCE_CLIENT: $provenanceClient")
                appendLine("SIGNATURE_ITAG: ${signatureItag ?: "UNKNOWN"}")
                appendLine("N_ITAG: ${nItag ?: "UNKNOWN"}")
                appendLine("DISK_ARTIFACT_PLAYER_ID: ${diskArtifact.playerId}")
                appendLine("DISK_ARTIFACT_SHA256: ${diskArtifact.sha256}")
                appendLine("DISK_ARTIFACT_AGE_MS: ${diskArtifact.ageMs}")
                appendLine("RUNTIME_PLAYER_ID: UNKNOWN")
                appendLine("RUNTIME_SHA256: UNKNOWN")
                appendLine("ST_CONSISTENCY: ${if (stConsistencyPassed) "PASS" else "SKIPPED_MANUAL"}")
                appendLine("OPERATION_SIGNATURE: ${if (sigPassed) "PASS" else "FAIL"}")
                appendLine("OPERATION_SIGNATURE_CATEGORY: $sigCategory")
                appendLine("OPERATION_SIGNATURE_AVAILABILITY: $sigAvailStatus")
                appendLine("OPERATION_N_TRANSFORM: ${if (nPassed) "PASS" else "FAIL"}")
                appendLine("OPERATION_N_TRANSFORM_CATEGORY: $nCategory")
                appendLine("OPERATION_N_TRANSFORM_AVAILABILITY: $nAvailStatus")
                if (sigDiagFrames.isNotEmpty()) {
                    appendLine("SIGNATURE_FRAMES:")
                    sigDiagFrames.forEach { appendLine("  $it") }
                }
                if (nDiagFrames.isNotEmpty()) {
                    appendLine("N_FRAMES:")
                    nDiagFrames.forEach { appendLine("  $it") }
                }
                appendLine("=====================================")
            }

            Log.i("CipherTest", summaryBuilder.toString())

            val overallSuccess = sigPassed && nPassed &&
                (sigAvailStatus == "PASS" || sigAvailStatus == "SKIPPED") &&
                (nAvailStatus == "PASS" || nAvailStatus == "SKIPPED" || nAvailStatus == "UNAVAILABLE")

            assertTrue("Aggregated cipher test failed (see sanitized diagnostic log)", overallSuccess)
        } finally {
            CipherTrace.sink = originalSink
            testHttpClient.dispatcher.executorService.shutdown()
            testHttpClient.connectionPool.evictAll()
        }
    }

    private fun acquireRealInputs(videoId: String, sts: Int): AcquiredInputs {
        val candidateClients = listOf(YouTubeClient.WEB_REMIX, YouTubeClient.WEB)
        var lastMissingType = "PLAYER_RESPONSE"

        for (client in candidateClients) {
            val playerResult = runBlocking {
                val res = withTimeoutOrNull(15_000L) {
                    val r = YouTube.player(
                        videoId = videoId,
                        client = client,
                        signatureTimestamp = sts,
                    )
                    currentCoroutineContext().ensureActive()
                    val error = r.exceptionOrNull()
                    if (error is CancellationException) {
                        throw error
                    }
                    r
                }
                currentCoroutineContext().ensureActive()
                res
            }

            if (playerResult == null) {
                lastMissingType = "PLAYER_RESPONSE_TIMEOUT"
                continue
            }

            val response = playerResult.getOrNull()
            if (response == null) {
                val errorClass = playerResult.exceptionOrNull()?.javaClass?.simpleName ?: "Unknown"
                lastMissingType = "PLAYER_RESPONSE_$errorClass"
                continue
            }

            if (response.playabilityStatus.status != "OK") {
                lastMissingType = "PLAYER_RESPONSE"
                continue
            }

            if (response.videoDetails != null && response.videoDetails?.videoId != videoId) {
                lastMissingType = "PLAYER_RESPONSE"
                continue
            }

            val extracted = extractInputsFromResponse(response, client.clientName)
            if (extracted != null) {
                return extracted
            }

            val streamingData = response.streamingData
            if (streamingData == null) {
                lastMissingType = "PLAYER_RESPONSE"
            } else {
                val formats = streamingData.formats.orEmpty() + streamingData.adaptiveFormats
                val hasValidCipher = formats.any { format ->
                    val c = format.signatureCipher ?: format.cipher
                    if (c.isNullOrBlank()) return@any false
                    val parsed = QuickJsCipher.parseFormEncoded(c)
                    !parsed["s"].isNullOrBlank() && !parsed["url"].isNullOrBlank() && parsed["url"]?.toHttpUrlOrNull() != null
                }
                lastMissingType = if (!hasValidCipher) "SIGNATURE_CIPHER" else "N_URL"
            }
        }

        fail("INPUT_UNAVAILABLE reason=$lastMissingType")
        error("Unreachable")
    }

    private fun extractInputsFromResponse(response: PlayerResponse, clientName: String): AcquiredInputs? {
        val streamingData = response.streamingData ?: return null
        val formats = streamingData.formats.orEmpty() + streamingData.adaptiveFormats

        var chosenCipher: String? = null
        var chosenCipherItag: Int? = null
        var embeddedUrlWithN: String? = null

        for (format in formats) {
            val rawCipher = format.signatureCipher ?: format.cipher
            if (!rawCipher.isNullOrBlank()) {
                val parsed = QuickJsCipher.parseFormEncoded(rawCipher)
                val s = parsed["s"]
                val directUrl = parsed["url"]
                if (!s.isNullOrBlank() && !directUrl.isNullOrBlank() && directUrl.toHttpUrlOrNull() != null) {
                    chosenCipher = rawCipher
                    chosenCipherItag = format.itag
                    val directHttpUrl = directUrl.toHttpUrlOrNull()
                    if (!directHttpUrl?.queryParameter("n").isNullOrBlank()) {
                        embeddedUrlWithN = directUrl
                    }
                    break
                }
            }
        }

        if (chosenCipher == null) return null

        for (format in formats) {
            val u = format.url
            if (!u.isNullOrBlank()) {
                val httpUrl = u.toHttpUrlOrNull()
                if (!httpUrl?.queryParameter("n").isNullOrBlank()) {
                    return AcquiredInputs(
                        signatureCipher = chosenCipher,
                        nUrl = u,
                        isNUrlEmbeddedInCipher = false,
                        clientName = clientName,
                        signatureItag = chosenCipherItag,
                        nItag = format.itag,
                    )
                }
            }
        }

        if (embeddedUrlWithN != null) {
            return AcquiredInputs(
                signatureCipher = chosenCipher,
                nUrl = embeddedUrlWithN,
                isNUrlEmbeddedInCipher = true,
                clientName = clientName,
                signatureItag = chosenCipherItag,
                nItag = chosenCipherItag,
            )
        }

        return null
    }

    private fun verifyHttpAvailability(client: OkHttpClient, url: String) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-1")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                assertTrue(
                    "HTTP availability check returned non-success code",
                    response.code in 200..299,
                )
            }
        } catch (e: Exception) {
            fail("HTTP availability check failed")
        }
    }
}
