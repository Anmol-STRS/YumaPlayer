/*
 * YumaPlayer (2026) | Original work by MuwMix
 * GPL-3.0 License | Contributors: see git history
 */
package moe.rukamori.archivetune.cipher

import android.content.Context
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.File
import java.util.Collections
import java.util.IdentityHashMap

internal enum class FailureCategory {
    UNKNOWN,
    QUICKJS_JS_FAILURE,
    EXTRACTION_UNAVAILABLE,
    SIGNATURE_OUTPUT_MISSING,
    N_OUTPUT_FAILURE,
    SIGNATURE_CAPABILITY_UNAVAILABLE,
    N_CAPABILITY_UNAVAILABLE,
}

internal data class DiskArtifactMetadata(
    val playerId: String,
    val sha256: String,
    val ageMs: String,
)

internal object CipherDiagnosticHelper {

    private val ALLOWED_FILES = setOf(
        "CipherBridge.kt",
        "QuickJsCipher.kt",
        "QuickJsCipherWorker.kt",
        "QuickJsCipherAdapter.kt",
        "MoriRhinoCipherAdapter.kt",
        "MoriCipherRuntime.kt",
        "MoriPlayerScriptProvider.kt",
        "CipherBootstrap.kt",
        "CipherManualInstrumentedTest.kt",
    )

    private val SAFE_METHOD_REGEX = Regex("^[a-zA-Z0-9_$]{1,64}$")
    private val HEX_ID_REGEX = Regex("^[a-fA-F0-9_-]{4,64}$")
    private val SHA256_HEX_REGEX = Regex("^[a-fA-F0-9]{64}$")

    fun findCancellation(throwable: Throwable?): CancellationException? {
        if (throwable == null) return null
        val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        fun search(t: Throwable?): CancellationException? {
            if (t == null || !visited.add(t)) return null
            if (t is CancellationException) return t
            search(t.cause)?.let { return it }
            for (s in t.suppressed) {
                search(s)?.let { return it }
            }
            return null
        }
        return search(throwable)
    }

    fun classifyFailure(throwable: Throwable, isSignatureOp: Boolean): FailureCategory {
        val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        var current: Throwable? = throwable
        while (current != null && visited.add(current)) {
            if (current.javaClass.simpleName == "QuickJsMissingSourceException") {
                return FailureCategory.EXTRACTION_UNAVAILABLE
            }
            val msg = current.message.orEmpty()
            when {
                msg.startsWith("[EXTRACTION]") || msg.startsWith("Could not extract player signature timestamp") ->
                    return FailureCategory.EXTRACTION_UNAVAILABLE
                msg.startsWith("[DECIPHER]") || msg.startsWith("[INTERNAL]") || current.javaClass.simpleName == "QuickJsException" ->
                    return if (isSignatureOp) FailureCategory.QUICKJS_JS_FAILURE else FailureCategory.N_OUTPUT_FAILURE
                msg.startsWith("signatureCipher contains blank 's'") || msg.startsWith("signatureCipher is missing 'url'") ->
                    return FailureCategory.SIGNATURE_OUTPUT_MISSING
                msg.startsWith("[N_TRANSFORM]") ->
                    return FailureCategory.N_OUTPUT_FAILURE
                msg.startsWith("Input is neither a valid HTTP(S) URL nor a valid signatureCipher") ->
                    return FailureCategory.SIGNATURE_CAPABILITY_UNAVAILABLE
            }
            for (suppressed in current.suppressed) {
                val suppressedCat = classifyFailure(suppressed, isSignatureOp)
                if (suppressedCat != FailureCategory.UNKNOWN) {
                    return suppressedCat
                }
            }
            current = current.cause
        }
        return FailureCategory.UNKNOWN
    }

    fun extractSanitizedDiagnostics(throwable: Throwable): List<String> {
        val result = mutableListOf<String>()
        val visited = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        fun traverse(t: Throwable?, relation: String) {
            if (t == null || !visited.add(t) || visited.size > 10) return
            val className = t.javaClass.name.substringAfterLast('.')
            result.add("$relation:class=$className")
            var frameCount = 0
            for (frame in t.stackTrace) {
                val fileName = frame.fileName
                val methodName = frame.methodName
                if (fileName != null && ALLOWED_FILES.contains(fileName) && SAFE_METHOD_REGEX.matches(methodName)) {
                    result.add("  frame=${fileName}:${methodName}:${frame.lineNumber}")
                    frameCount++
                    if (frameCount >= 3) break
                }
            }
            traverse(t.cause, "CAUSE")
            for (s in t.suppressed) {
                traverse(s, "SUPPRESSED")
            }
        }
        traverse(throwable, "PRIMARY")
        return result
    }

    fun readDiskArtifact(context: Context): DiskArtifactMetadata {
        val cacheDir = File(context.noBackupFilesDir, "mori_cipher")
        val artifactFile = File(cacheDir, "active_player.json")
        if (!artifactFile.isFile || artifactFile.length() > 4 * 1024 * 1024) {
            return DiskArtifactMetadata("UNKNOWN", "UNKNOWN", "UNKNOWN")
        }
        return try {
            val text = artifactFile.readText(Charsets.UTF_8)
            val json = JSONObject(text)
            val plan = json.optJSONObject("plan")
            val rawPlayerId = plan?.optString("playerId").orEmpty()
            val rawSha256 = plan?.optString("sourceSha256").orEmpty()
            val createdAtMillis = plan?.optLong("createdAtMillis", -1L) ?: -1L
            val playerId = if (HEX_ID_REGEX.matches(rawPlayerId)) rawPlayerId else "UNKNOWN"
            val sha256 = if (SHA256_HEX_REGEX.matches(rawSha256)) rawSha256 else "UNKNOWN"
            val ageMs = if (createdAtMillis >= 0L) (System.currentTimeMillis() - createdAtMillis).coerceAtLeast(0L).toString() else "UNKNOWN"
            DiskArtifactMetadata(playerId, sha256, ageMs)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            DiskArtifactMetadata("UNKNOWN", "UNKNOWN", "UNKNOWN")
        }
    }
}
