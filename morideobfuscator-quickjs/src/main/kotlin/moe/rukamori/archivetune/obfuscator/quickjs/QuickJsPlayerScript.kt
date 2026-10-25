/*
 * YumaPlayer (2026) | Modified work by MuwMix
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.obfuscator.quickjs

import moe.rukamori.archivetune.cipher.PlayerScriptProvider

data class QuickJsPlayerScript(
    val playerId: String,
    val source: String,
    val url: String? = null,
)

fun interface QuickJsPlayerScriptProvider {
    suspend fun getPlayerScript(videoId: String): QuickJsPlayerScript
}

fun PlayerScriptProvider.asQuickJsProvider(): QuickJsPlayerScriptProvider =
    QuickJsPlayerScriptProvider { videoId ->
        val raw = getPlayerScript(videoId)
        QuickJsPlayerScript(raw.playerId, raw.source, raw.url)
    }
