/*
 * YumaPlayer (2026) | Original work by MuwMix
 * GPL-3.0 License
 */

package moe.rukamori.archivetune.cipher

import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.obfuscator.MoriCipherConfig
import moe.rukamori.archivetune.obfuscator.MoriCipherRuntime
import moe.rukamori.archivetune.obfuscator.MoriPlayerScriptProvider
import moe.rukamori.archivetune.obfuscator.MoriRhinoCipherAdapter
import moe.rukamori.archivetune.obfuscator.quickjs.QuickJsCipher
import moe.rukamori.archivetune.obfuscator.quickjs.QuickJsCipherAdapter
import moe.rukamori.archivetune.obfuscator.quickjs.asQuickJsProvider
import java.io.File
import javax.naming.Context

object CipherBootstrap {
    fun init(context: Context, defaultEngine: CipherEngineType = CipherEngineType.QUICKJS) {
        val cacheDir = File(context.noBackupFilesDir, "mori_cipher")

        // Регистрация Rhino
        CipherBridge.registerFactory(CipherEngineType.RHINO) {
            MoriCipherRuntime.initialize(
                MoriCipherConfig(
                    cacheDirectory = cacheDir,
                    proxyProvider = { YouTube.streamProxy },
                )
            )
            MoriRhinoCipherAdapter()
        }

        // Регистрация QuickJS
        CipherBridge.registerFactory(CipherEngineType.QUICKJS) {
            val provider = MoriPlayerScriptProvider(
                cacheDirectory = cacheDir,
                proxyProvider = { YouTube.streamProxy },
            )
            val cipher = QuickJsCipher(
                context = context,
                playerScriptProvider = provider.asQuickJsProvider(),
            )
            QuickJsCipherAdapter(cipher)
        }

        CipherBridge.selectEngine(defaultEngine)
    }
}