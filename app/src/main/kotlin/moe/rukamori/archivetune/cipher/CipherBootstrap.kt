/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.cipher

import android.content.Context
import android.util.Log
import moe.rukamori.archivetune.BuildConfig
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.obfuscator.MoriCipherConfig
import moe.rukamori.archivetune.obfuscator.MoriCipherRuntime
import moe.rukamori.archivetune.obfuscator.MoriPlayerScriptProvider
import moe.rukamori.archivetune.obfuscator.MoriRhinoCipherAdapter
import moe.rukamori.archivetune.obfuscator.quickjs.QuickJsCipher
import moe.rukamori.archivetune.obfuscator.quickjs.QuickJsCipherAdapter
import moe.rukamori.archivetune.obfuscator.quickjs.asQuickJsProvider
import java.io.File

object CipherBootstrap {
    private var initialized = false

    @Synchronized
    fun init(
        context: Context,
        defaultEngine: CipherEngineType = CipherEngineType.RHINO,
    ) {
        if (initialized) {
            CipherBridge.selectEngine(defaultEngine)
            return
        }

        CipherTrace.sink = if (BuildConfig.DEBUG) {
            { message -> Log.d("CipherTrace", message) }
        } else {
            null
        }

        val appContext = context.applicationContext
        val cacheDir = File(appContext.noBackupFilesDir, "mori_cipher")

        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            MoriCipherRuntime.initialize(
                MoriCipherConfig(
                    cacheDirectory = cacheDir,
                    proxyProvider = { YouTube.streamProxy },
                ),
            )
            MoriRhinoCipherAdapter()
        }

        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            val provider = MoriPlayerScriptProvider(
                cacheDirectory = cacheDir,
                proxyProvider = { YouTube.streamProxy },
            )
            val cipher = QuickJsCipher(
                context = appContext,
                playerScriptProvider = provider.asQuickJsProvider(),
            )
            QuickJsCipherAdapter(cipher)
        }

        CipherBridge.selectEngine(defaultEngine)
        initialized = true
    }
}
