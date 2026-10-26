/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.obfuscator.quickjs

import android.content.Context
import android.util.Base64
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.QuickJsException
import com.dokar.quickjs.QuickJsInterruptedException
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import com.dokar.quickjs.evaluate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import moe.rukamori.archivetune.cipher.CipherTrace
import moe.rukamori.archivetune.obfuscator.quickjs.youtubei.YoutubeiDiskCache
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

internal class QuickJsMissingSourceException(message: String) : IllegalStateException(message)

internal class QuickJsCipherWorker(
    private val context: Context?,
    private val diskCache: YoutubeiDiskCache?,
    private val dispatcher: CoroutineDispatcher,
    private val onRuntimeDiscarded: () -> Unit = {},
) {
    private val secureRandom = SecureRandom()
    private var quickJs: QuickJs? = null
    private var initialized = false

    suspend fun decipherUrl(requestJson: String): String {
        val runtime = ensureInitialized()
        val response =
            try {
                val code = "await globalThis.ArchiveTuneYoutubei.decipherUrl(" +
                    JSONObject.quote(requestJson) +
                    ");"
                CipherTrace.log("JS_EXECUTE", "engine=QUICKJS operation=DECIPHER")
                runtime.evaluate<String>(code, "archivetune-decipher.js")
            } catch (cancellation: CancellationException) {
                discardRuntime(runtime, cancellation)
                throw cancellation
            } catch (interrupted: QuickJsInterruptedException) {
                discardRuntime(runtime, interrupted)
                currentCoroutineContext().ensureActive()
                throw interrupted
            } catch (throwable: Throwable) {
                if (throwable.isQuickJsOutOfMemory()) {
                    discardRuntime(runtime, throwable)
                }
                throw throwable
            }

        currentCoroutineContext().ensureActive()
        val json = JSONObject(response)
        if (!json.optBoolean("ok")) {
            val error = json.optJSONObject("error")
            val message = error?.optString("message") ?: "youtubei decipher failed"
            val kind = error?.optString("kind") ?: "INTERNAL"
            CipherTrace.log("JS_ERROR", "kind=$kind msg=$message raw=$response")
            if (kind == "DECIPHER" && message.contains("Player script source missing")) {
                throw QuickJsMissingSourceException(message)
            }
            throw IllegalStateException("[$kind] $message")
        }
        return json.getString("value")
    }

    fun close() {
        val runtime = quickJs
        quickJs = null
        initialized = false
        onRuntimeDiscarded()
        runtime?.close()
    }

    private suspend fun ensureInitialized(): QuickJs {
        quickJs?.takeIf { initialized }?.let {
            CipherTrace.log("RUNTIME_REUSED", "engine=QUICKJS")
            return it
        }
        quickJs?.close()

        val appContext = context?.applicationContext
            ?: throw IllegalStateException("Android Context is required to initialize QuickJS assets")

        CipherTrace.log("RUNTIME_INIT_START", "engine=QUICKJS")
        val initStartNanos = System.nanoTime()
        val runtime = QuickJs.create(dispatcher)
        try {
            runtime.memoryLimit = JAVASCRIPT_MEMORY_LIMIT_BYTES
            runtime.maxStackSize = JAVASCRIPT_STACK_LIMIT_BYTES

            runtime.asyncFunction<String, String?>("__archiveTuneCacheRead") { key ->
                diskCache?.read(key)
            }
            runtime.asyncFunction<String, Unit>("__archiveTuneCacheWrite") { request ->
                diskCache?.write(request)
            }
            runtime.asyncFunction<String, Unit>("__archiveTuneCacheRemove") { key ->
                diskCache?.remove(key)
            }
            runtime.asyncFunction<String, String>("__archiveTuneSha1") { value ->
                MessageDigest
                    .getInstance("SHA-1")
                    .digest(value.toByteArray(Charsets.UTF_8))
                    .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
            }
            runtime.function("__archiveTuneUuid") { UUID.randomUUID().toString() }
            runtime.function("__archiveTuneRandom") { arguments ->
                val size = (arguments.firstOrNull() as? Number)?.toInt()?.coerceIn(0, MAX_RANDOM_BYTES) ?: 0
                ByteArray(size)
                    .also(secureRandom::nextBytes)
                    .let { Base64.encodeToString(it, Base64.NO_WRAP) }
            }

            evaluateAsset(runtime, appContext, RUNTIME_POLYFILLS_ASSET)
            evaluateAsset(runtime, appContext, BUNDLE_ASSET)

            val version = runtime.evaluate<String>("globalThis.ArchiveTuneYoutubei.version;")
            check(version == YOUTUBEI_VERSION) { "Unexpected runtime version: $version" }

            quickJs = runtime
            initialized = true
            val durationMs = (System.nanoTime() - initStartNanos) / 1_000_000L
            CipherTrace.log("RUNTIME_INIT_SUCCESS", "engine=QUICKJS durationMs=$durationMs")
            return runtime
        } catch (throwable: Throwable) {
            val errorClass = throwable.javaClass.simpleName
            CipherTrace.log("RUNTIME_INIT_FAILED", "engine=QUICKJS error=$errorClass")
            discardRuntime(runtime, throwable)
            throw throwable
        }
    }

    private suspend fun evaluateAsset(
        runtime: QuickJs,
        appContext: Context,
        assetName: String,
    ) {
        val source =
            appContext.assets
                .open(assetName)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        runtime.evaluate<Unit>(source, assetName)
    }

    private fun discardRuntime(
        runtime: QuickJs,
        failure: Throwable,
    ) {
        if (quickJs === runtime) {
            quickJs = null
            initialized = false
            onRuntimeDiscarded()
        }
        try {
            runtime.close()
        } catch (closeFailure: Throwable) {
            failure.addSuppressed(closeFailure)
        }
    }

    private fun Throwable.isQuickJsOutOfMemory(): Boolean =
        this is QuickJsException && message.orEmpty().contains("out of memory", ignoreCase = true)

    companion object {
        const val YOUTUBEI_VERSION = "18.0.0"
        const val BUNDLE_ASSET = "youtubei-quickjs/youtubei.bundle.js"
        const val RUNTIME_POLYFILLS_ASSET = "youtubei-quickjs/runtime-polyfills.js"
        const val JAVASCRIPT_MEMORY_LIMIT_BYTES = 256L * 1024L * 1024L
        const val JAVASCRIPT_STACK_LIMIT_BYTES = 2L * 1024L * 1024L
        const val WORKER_STACK_SIZE_BYTES = 4L * JAVASCRIPT_STACK_LIMIT_BYTES
        const val MAX_RANDOM_BYTES = 4096
    }
}
